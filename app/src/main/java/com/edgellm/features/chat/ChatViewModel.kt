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
    val error: String? = null
)

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {
    
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    private val _isGenerating = MutableStateFlow(false)
    
    val state: StateFlow<ChatState> = combine(
        _currentSessionId,
        _isGenerating,
        _currentSessionId.flatMapLatest { id ->
            if (id != null) repository.getMessages(id) else flowOf(emptyList())
        }
    ) { sessionId, isGen, messages ->
        ChatState(
            messages = messages,
            isGenerating = isGen,
            currentSessionId = sessionId
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
            if (_currentSessionId.value == id) {
                _currentSessionId.value = null
            }
        }
    }

    fun sendMessage(text: String) {
        val engine = engineRef ?: return
        
        viewModelScope.launch {
            // Auto-create session if none exists
            val sessionId = _currentSessionId.value ?: repository.createNewSession("New Chat").also { 
                _currentSessionId.value = it 
            }
            
            repository.saveMessage(sessionId, ChatMessage("user", text))
            repository.saveMessage(sessionId, ChatMessage("assistant", ""))
            
            _isGenerating.value = true
            
            val systemPrompt = if (agentSkillsEnabled) {
                skillManager?.buildSkillSystemPrompt(skillManager!!.skills.value) ?: ""
            } else ""

            val history = state.value.messages
            val prompt = buildPrompt(systemPrompt, history.dropLast(1))

            var fullText = ""
            try {
                engine.generateStream(prompt).collect { token ->
                    fullText += token
                    val result = processThinkingTags(fullText)
                    repository.saveMessage(sessionId, 
                        ChatMessage("assistant", result.second, result.first.ifEmpty { null })
                    )
                }
            } catch (e: Exception) {
                repository.saveMessage(sessionId, ChatMessage("assistant", "Error: ${e.message}"))
            } finally {
                _isGenerating.value = false
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
        // Regex refined for 2026 standards (handles multi-line and greedy matching)
        val thinkRegex = Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL)
        val thinking = thinkRegex.findAll(text).joinToString("\n") { it.groupValues[1] }
        val display = thinkRegex.replace(text, "").trim()
        return Pair(thinking, display)
    }
}
