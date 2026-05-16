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
    val modelLoaded: Boolean = false
)

class ChatViewModel : ViewModel() {
    
    // Connect to the repository for persistence
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = combine(
        ChatRepository.messages,
        _state
    ) { messages, internalState ->
        internalState.copy(messages = messages)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatState())

    var engineRef: com.edgellm.engine.InferenceEngine? = null
    var skillManager: SkillManager? = null
    var agentSkillsEnabled: Boolean = true

    fun sendMessage(text: String) {
        val engine = engineRef ?: return
        
        // Add user message to persistent repository
        val userMsg = ChatMessage("user", text)
        ChatRepository.addMessage(userMsg)
        
        // Add empty assistant message to be filled
        val assistantMsg = ChatMessage("assistant", "")
        ChatRepository.addMessage(assistantMsg)
        
        _state.value = _state.value.copy(isGenerating = true, modelLoaded = true)
        
        viewModelScope.launch {
            val systemPrompt = if (agentSkillsEnabled) {
                skillManager?.buildSkillSystemPrompt(skillManager!!.skills.value) ?: ""
            } else ""

            val history = ChatRepository.messages.value
            val prompt = buildPrompt(systemPrompt, history.dropLast(1)) // exclude the empty assistant msg

            var fullText = ""
            try {
                engine.generateStream(prompt).collect { token ->
                    fullText += token
                    val result = processThinkingTags(fullText)
                    ChatRepository.updateLastMessage(
                        ChatMessage("assistant", result.second, result.first.ifEmpty { null })
                    )
                }
            } catch (e: Exception) {
                ChatRepository.updateLastMessage(ChatMessage("assistant", "Error: ${e.message}"))
            } finally {
                _state.value = _state.value.copy(isGenerating = false)
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
