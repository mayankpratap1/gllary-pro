package com.edgellm.engine

import android.content.ContentResolver
import android.util.Log
import io.github.ljcamargo.llamacpp.LlamaHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Enterprise-grade GGUF Engine.
 * Supports Llama and other GGUF models via llama.cpp.
 */
class GgufEngine(private val contentResolver: ContentResolver) : InferenceEngine {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()

    private val _events = MutableSharedFlow<LlamaHelper.LLMEvent>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var helper: LlamaHelper? = null

    override var isLoaded = false
        private set
    override var modelName: String? = null
        private set
    override val supportsVision = false
    override val supportsThinking = true

    override suspend fun load(uri: String, config: EngineConfig): Result<Unit> {
        return try {
            val h = LlamaHelper(contentResolver, scope, _events)
            h.load(path = uri, contextLength = config.contextLength) { _ ->
                helper = h
                isLoaded = true
                modelName = uri.substringAfterLast("/").substringBeforeLast(".")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GgufEngine", "Load Failure", e)
            isLoaded = false
            Result.failure(e)
        }
    }

    override suspend fun generate(prompt: String): String {
        val h = helper ?: return "Error: GGUF not loaded"
        return mutex.withLock {
            val sb = StringBuilder()
            val job = scope.launch {
                _events.takeWhile { it !is LlamaHelper.LLMEvent.Done }.collect { e ->
                    if (e is LlamaHelper.LLMEvent.Ongoing) {
                        sb.append(e.word)
                    }
                }
            }
            h.predict(prompt)
            job.join()
            sb.toString()
        }
    }

    override fun generateStream(prompt: String): Flow<String> {
        val h = helper ?: return flowOf("Error: GGUF not loaded")
        val outputFlow = MutableSharedFlow<String>(
            extraBufferCapacity = 256,
            onBufferOverflow = BufferOverflow.SUSPEND
        )
        scope.launch {
            mutex.withLock {
                val job = launch {
                    _events.takeWhile { it !is LlamaHelper.LLMEvent.Done }.collect { e ->
                        if (e is LlamaHelper.LLMEvent.Ongoing) {
                            outputFlow.emit(e.word)
                        }
                    }
                }
                h.predict(prompt)
                job.join()
            }
        }
        return outputFlow
    }

    override suspend fun generateWithImage(prompt: String, imageBytes: ByteArray): String {
        return generate(prompt)
    }

    override fun unload() {
        helper = null
        isLoaded = false
        modelName = null
    }
}
