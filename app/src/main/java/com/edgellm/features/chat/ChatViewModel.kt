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
    val streamingText: String = "" // In-memory buffer for smooth UI updates
)

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {
    
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    private val _isGenerating = MutableStateFlow(false)
    private val _streamingText = MutableStateFlow("")
    
    val state: StateFlow<ChatState> = combine(
        _currentSessionId,
        _isGenerating,
        _streamingText,
        _currentSessionId.flatMapLatest { id ->
            if (id != null) repository.getMessages(id) else flowOf(emptyList())
        }
    ) { sessionId, isGen, streaming, dbMessages ->
        // If we are generating, append the streaming buffer to the UI list
        val displayMessages = if (isGen && streaming.isNotEmpty()) {
            dbMessages.dropLast(1) + ChatMessage("assistant", streaming)
        } else {
            dbMessages
        }
        
        ChatState(
            messages = displayMessages,
            isGenerating = isGen,
            currentSessionId = sessionId,
            streamingText = streaming
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
            
            // Save user message to DB immediately
            repository.saveMessage(sessionId, ChatMessage("user", text))
            
            // Start generation state
            _isGenerating.value = true
            _streamingText.value = ""
            
            val systemPrompt = if (agentSkillsEnabled) {
                skillManager?.buildSkillSystemPrompt(skillManager!!.skills.value) ?: ""
            } else ""

            val history = state.value.messages
            val prompt = buildPrompt(systemPrompt, history)

            var fullResponse = ""
            try {
                engine.generateStream(prompt).collect { token ->
                    fullResponse += token
                    // Update only UI buffer for speed
                    _streamingText.value = fullResponse
                }
                
                // Once done, save the complete message to the persistent database
                val result = processThinkingTags(fullResponse)
                repository.saveMessage(sessionId, 
                    ChatMessage("assistant", result.second, result.first.ifEmpty { null })
                )
            } catch (e: Exception) {
                repository.saveMessage(sessionId, ChatMessage("assistant", "Error: ${e.message}"))
            } finally {
                _isGenerating.value = false
                _streamingText.value = ""
            }
        }
    }

    private fun buildPrompt(system: String, history: List<ChatMessage>): String {
        val sb = StringBuilder()
        if (system.isNotEmpty()) sb.append("System: $system\n")
        history.forEach { sb.append("${it.role.uppercase()}: ${it.content}\n") }
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
