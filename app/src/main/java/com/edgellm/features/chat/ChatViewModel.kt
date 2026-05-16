package com.edgellm.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgellm.data.ChatRepository
import com.edgellm.skills.SkillManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val currentSessionId: Long? = null,
    val streamingText: String = "",
    val streamingThinking: String? = null
)

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {
    
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    private val _isGenerating = MutableStateFlow(false)
    private val _streamingText = MutableStateFlow("")
    private val _streamingThinking = MutableStateFlow<String?>(null)
    
    val state: StateFlow<ChatState> = combine(
        _currentSessionId,
        _isGenerating,
        _streamingText,
        _streamingThinking,
        _currentSessionId.flatMapLatest { id ->
            if (id != null) repository.getMessages(id) else flowOf(emptyList())
        }
    ) { sessionId, isGen, streaming, streamingThink, dbMessages ->
        // If we are generating, the LAST message in the list should be our streaming one
        val displayMessages = if (isGen && (streaming.isNotEmpty() || streamingThink != null)) {
            dbMessages + ChatMessage("assistant", streaming, streamingThink)
        } else {
            dbMessages
        }
        
        ChatState(
            messages = displayMessages,
            isGenerating = isGen,
            currentSessionId = sessionId,
            streamingText = streaming,
            streamingThinking = streamingThink
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatState())

    val sessions = repository.sessions
    var engineRef: com.edgellm.engine.InferenceEngine? = null
    var skillManager: SkillManager? = null
    var agentSkillsEnabled: Boolean = true

    fun createNewChat() {
        viewModelScope.launch {
            val id = repository.createNewSession("Chat ${System.currentTimeMillis()}")
            _currentSessionId.value = id
        }
    }

    fun selectSession(id: Long) {
        _currentSessionId.value = id
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            repository.deleteSession(id)
            if (_currentSessionId.value == id) _currentSessionId.value = null
        }
    }

    fun sendMessage(text: String) {
        val engine = engineRef ?: return
        
        viewModelScope.launch {
            val sessionId = _currentSessionId.value ?: repository.createNewSession("New Chat").also { 
                _currentSessionId.value = it 
            }
            
            // 1. Save user message to DB
            repository.saveMessage(sessionId, ChatMessage("user", text))
            
            // 2. Start streaming state
            _isGenerating.value = true
            _streamingText.value = ""
            _streamingThinking.value = null
            
            val systemPrompt = if (agentSkillsEnabled) {
                skillManager?.buildSkillSystemPrompt(skillManager!!.skills.value) ?: ""
            } else ""

            val history = state.value.messages
            val prompt = buildPrompt(systemPrompt, history)

            var fullResponse = ""
            try {
                engine.generateStream(prompt).collect { token ->
                    fullResponse += token
                    val result = processThinkingTags(fullResponse)
                    
                    // Update ONLY the UI flows (In-memory) for performance
                    _streamingText.value = result.second
                    _streamingThinking.value = result.first.ifEmpty { null }
                }
                
                // 3. ONCE FINISHED: Save the final complete message to DB
                val finalResult = processThinkingTags(fullResponse)
                repository.saveMessage(sessionId, 
                    ChatMessage("assistant", finalResult.second, finalResult.first.ifEmpty { null })
                )
            } catch (e: Exception) {
                repository.saveMessage(sessionId, ChatMessage("assistant", "Error: ${e.message}"))
            } finally {
                _isGenerating.value = false
                _streamingText.value = ""
                _streamingThinking.value = null
            }
        }
    }

    private fun buildPrompt(system: String, history: List<ChatMessage>): String {
        val sb = StringBuilder()
        if (system.isNotEmpty()) sb.append("System: $system\n")
        history.forEach { 
            if (it.content.isNotEmpty()) {
                sb.append("${it.role.uppercase()}: ${it.content}\n")
            }
        }
        sb.append("ASSISTANT:")
        return sb.toString()
    }

    private fun processThinkingTags(text: String): Pair<String, String> {
        val thinkRegex = Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL)
        val thinking = thinkRegex.findAll(text).joinToString("\n") { it.groupValues[1] }
        val display = thinkRegex.replace(text, "").trim()
        return Pair(thinking, display)
    }
}
