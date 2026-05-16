package com.edgellm.features.agentskills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgellm.engine.InferenceEngine
import com.edgellm.features.chat.ChatMessage
import com.edgellm.skills.Skill
import com.edgellm.skills.SkillManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AgentSkillsViewModel : ViewModel() {
    private val _state = MutableStateFlow(AgentSkillsState())
    val state: StateFlow<AgentSkillsState> = _state

    var engineRef: InferenceEngine? = null
    var skillManager: SkillManager? = null

    fun sendWithSkills(input: String, activeSkills: List<Skill>) {
        val engine = engineRef ?: return
        val manager = skillManager ?: return
        
        val history = _state.value.messages + ChatMessage("user", input)
        _state.value = _state.value.copy(
            messages = history + ChatMessage("assistant", ""),
            isGenerating = true
        )

        viewModelScope.launch {
            val systemPrompt = manager.buildSkillSystemPrompt(activeSkills)
            val prompt = "System: $systemPrompt\nUser: $input\nAssistant:"

            var fullText = ""
            try {
                engine.generateStream(prompt).collect { token ->
                    fullText += token
                    val updated = _state.value.messages.dropLast(1) + ChatMessage("assistant", fullText)
                    _state.value = _state.value.copy(messages = updated)
                }
            } catch (e: Exception) {
                val updated = _state.value.messages.dropLast(1) + ChatMessage("assistant", "Error: ${e.message}")
                _state.value = _state.value.copy(messages = updated)
            } finally {
                _state.value = _state.value.copy(isGenerating = true)
            }
            _state.value = _state.value.copy(isGenerating = false)
        }
    }

    fun addSkillFromUrl(url: String) {
        viewModelScope.launch {
            try {
                skillManager?.downloadFromUrl(url)
            } catch (e: Exception) {
                // In a real app, show a snackbar or error toast
            }
        }
    }
}

data class AgentSkillsState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false
)
