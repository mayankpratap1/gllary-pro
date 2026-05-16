package com.edgellm.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgellm.skills.SkillManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val modelLoaded: Boolean = false,
    val error: String? = null
)

class ChatViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state

    var engineRef: com.edgellm.engine.InferenceEngine? = null
    var skillManager: SkillManager? = null
    var agentSkillsEnabled: Boolean = false

    fun sendMessage(text: String) {
        val engine = engineRef ?: return
        val userMsg = ChatMessage("user", text)
        val history = _state.value.messages + userMsg
        
        _state.value = _state.value.copy(
            messages = history + ChatMessage("assistant", ""),
            isGenerating = true
        )
        
        viewModelScope.launch {
            val systemPrompt = if (agentSkillsEnabled) {
                skillManager?.buildSkillSystemPrompt(skillManager!!.skills.value) ?: ""
            } else ""

            val prompt = buildPrompt(systemPrompt, history)

            var fullText = ""
            try {
                engine.generateStream(prompt).collect { token ->
                    fullText += token
                    val result = processThinkingTags(fullText)
                    val updated = _state.value.messages.dropLast(1) + 
                        ChatMessage("assistant", result.second, result.first.ifEmpty { null })
                    _state.value = _state.value.copy(messages = updated)
                }
            } catch (e: Exception) {
                val updated = _state.value.messages.dropLast(1) + ChatMessage("assistant", "Error: ${e.message}")
                _state.value = _state.value.copy(messages = updated)
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
