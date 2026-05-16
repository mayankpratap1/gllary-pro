package com.edgellm.features.benchmark

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgellm.engine.InferenceEngine
import com.edgellm.engine.LiteRtEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BenchmarkViewModel : ViewModel() {
    private val _state = MutableStateFlow(BenchmarkState())
    val state: StateFlow<BenchmarkState> = _state

    var engineRef: InferenceEngine? = null

    fun runBenchmark(context: Context) {
        val engine = engineRef ?: return
        if (!engine.isLoaded) return

        _state.value = _state.value.copy(isRunning = true, progress = 0f, status = "Starting benchmark...")

        viewModelScope.launch {
            try {
                // 1. Measure Prefill (Prompt Processing)
                _state.value = _state.value.copy(status = "Measuring Prefill speed...", progress = 0.3f)
                val prefillPrompt = "What is the capital of France? ".repeat(10) // ~50 tokens
                val prefillStart = System.currentTimeMillis()
                engine.generate(prefillPrompt)
                val prefillEnd = System.currentTimeMillis()
                val prefillMs = prefillEnd - prefillStart
                val prefillToksPerSec = if (prefillMs > 0) (50 * 1000 / prefillMs).toInt() else 0

                // 2. Measure Decode (Token Generation)
                _state.value = _state.value.copy(status = "Measuring Decode speed...", progress = 0.6f)
                val decodePrompt = "Tell me a long story about a robot."
                val decodeStart = System.currentTimeMillis()
                var ttft: Long = 0
                var tokenCount = 0
                
                engine.generateStream(decodePrompt).collect {
                    if (tokenCount == 0) {
                        ttft = System.currentTimeMillis() - decodeStart
                    }
                    tokenCount++
                    if (tokenCount >= 50) return@collect // Stop after 50 tokens for bench
                }
                val decodeEnd = System.currentTimeMillis()
                val decodeMs = decodeEnd - decodeStart
                val decodeToksPerSec = if (decodeMs > 0) (tokenCount * 1000 / decodeMs).toInt() else 0

                // 3. Memory Stats
                _state.value = _state.value.copy(status = "Gathering stats...", progress = 0.9f)
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                val peakMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024)

                val results = BenchmarkResults(
                    loadTimeMs = 0, // Should be tracked during actual load
                    prefillToksPerSec = prefillToksPerSec,
                    decodeToksPerSec = decodeToksPerSec,
                    ttftMs = ttft,
                    peakMemMb = peakMem.toInt(),
                    engineType = if (engine is LiteRtEngine) "LiteRT" else "GGUF",
                    backend = "CPU/GPU" // Simplified
                )

                _state.value = _state.value.copy(isRunning = false, progress = 1.0f, status = "Done", results = results)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isRunning = false, status = "Error: ${e.message}")
            }
        }
    }
}

data class BenchmarkState(
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val status: String = "",
    val results: BenchmarkResults? = null
)

data class BenchmarkResults(
    val loadTimeMs: Long,
    val prefillToksPerSec: Int,
    val decodeToksPerSec: Int,
    val ttftMs: Long,
    val peakMemMb: Int,
    val engineType: String,
    val backend: String
)
