package com.edgellm.features.promptlab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgellm.engine.EngineConfig
import com.edgellm.engine.InferenceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PromptLabViewModel : ViewModel() {
    private val _state = MutableStateFlow(PromptLabState())
    val state: StateFlow<PromptLabState> = _state

    var engineRef: InferenceEngine? = null

    fun run(prompt: String, systemPrompt: String, temperature: Float, topK: Int, maxTokens: Int) {
        val engine = engineRef ?: return
        
        _state.value = _state.value.copy(isRunning = true, result = "", stats = null)
        
        val fullPrompt = if (systemPrompt.isNotEmpty()) "System: $systemPrompt\nUser: $prompt\nAssistant:" else prompt
        val config = EngineConfig(
            temperature = temperature,
            topK = topK,
            maxTokens = maxTokens
        )

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            var firstTokenTime: Long? = null
            var tokenCount = 0
            var fullText = ""

            try {
                engine.generateStream(fullPrompt).collect { token ->
                    if (firstTokenTime == null) {
                        firstTokenTime = System.currentTimeMillis()
                    }
                    tokenCount++
                    fullText += token
                    _state.value = _state.value.copy(result = fullText)
                }

                val endTime = System.currentTimeMillis()
                val totalTimeSec = (endTime - startTime) / 1000.0
                val tokensPerSec = if (totalTimeSec > 0) (tokenCount / totalTimeSec).toInt() else 0
                val latencyMs = if (firstTokenTime != null) firstTokenTime!! - startTime else 0

                _state.value = _state.value.copy(
                    isRunning = false,
                    stats = RunStats(
                        tokensPerSec = tokensPerSec,
                        totalTokens = tokenCount,
                        latencyMs = latencyMs
                    )
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isRunning = false, result = "Error: ${e.message}")
            }
        }
    }
}

data class PromptLabState(
    val isRunning: Boolean = false,
    val result: String? = null,
    val stats: RunStats? = null
)

data class RunStats(val tokensPerSec: Int, val totalTokens: Int, val latencyMs: Long)
