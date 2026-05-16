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

    // Set externally from service binding (see service section)
    var engineRef: com.edgellm.engine.InferenceEngine? = null
    var skillManager: SkillManager? = null
    var agentSkillsEnabled: Boolean = false

    fun sendMessage(text: String) {
        val engine = engineRef ?: return
        val history = _state.value.messages + ChatMessage("user", text)
        _state.value = _state.value.copy(
            messages = history + ChatMessage("assistant", ""),
            isGenerating = true
        )
        viewModelScope.launch {
            // Build system prompt (inject active skills if enabled)
            val systemPrompt = if (agentSkillsEnabled) {
                skillManager?.buildSkillSystemPrompt(skillManager!!.skills.value) ?: ""
            } else ""

            val promptParts = buildList {
                if (systemPrompt.isNotEmpty()) add("System: $systemPrompt")
                history.forEach { add("${it.role.replaceFirstChar { c -> c.uppercase() }}: ${it.content}") }
                add("Assistant:")
            }
            val prompt = promptParts.joinToString("\n")

            var fullText = ""
            var thinkBuffer = ""
            var inThink = false

            engine.generateStream(prompt).collect { token ->
                fullText += token

                // Parse <think>...</think> tags for Thinking Mode
                val displayText = processThinkingTags(fullText).second
                val thinking = processThinkingTags(fullText).first

                val updated = _state.value.messages.dropLast(1) +
                    ChatMessage("assistant", displayText, thinking.ifEmpty { null })
                _state.value = _state.value.copy(messages = updated)
            }
            _state.value = _state.value.copy(isGenerating = false)
        }
    }

    // Extracts <think>content</think> from response
    // Returns Pair(thinkingText, displayText)
    private fun processThinkingTags(text: String): Pair<String, String> {
        val thinkRegex = Regex("<think>(.*?)</think>", RegexOption.DOT_MATCHES_ALL)
        val thinking = thinkRegex.findAll(text).joinToString("\n") { it.groupValues[1] }
        val display = thinkRegex.replace(text, "").trim()
        return Pair(thinking, display)
    }
}
