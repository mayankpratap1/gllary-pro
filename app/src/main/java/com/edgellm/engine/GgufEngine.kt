package com.edgellm.engine

import android.content.ContentResolver
import io.github.ljcamargo.llamacpp.LlamaHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    override val supportsVision = true   // LLaVA GGUF models support vision
    override val supportsThinking = true // Any model can emit <think> tags

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
            isLoaded = false
            Result.failure(e)
        }
    }

    override suspend fun generate(prompt: String): String {
        check(isLoaded) { "No model loaded" }
        return mutex.withLock {
            val sb = StringBuilder()
            val job = scope.launch {
                _events.takeWhile { it !is LlamaHelper.LLMEvent.Done }.collect { e ->
                    if (e is LlamaHelper.LLMEvent.Ongoing) {
                        sb.append(e.word)
                    }
                }
            }
            helper!!.predict(prompt)
            job.join()
            sb.toString()
        }
    }

    override fun generateStream(prompt: String): Flow<String> {
        check(isLoaded) { "No model loaded" }
        val flow = MutableSharedFlow<String>(
            extraBufferCapacity = 256,
            onBufferOverflow = BufferOverflow.SUSPEND
        )
        scope.launch {
            mutex.withLock {
                _events.takeWhile { it !is LlamaHelper.LLMEvent.Done }.collect { e ->
                    if (e is LlamaHelper.LLMEvent.Ongoing) {
                        flow.emit(e.word)
                    }
                }
            }
        }
        return flow
    }

    // GGUF vision: pass mmproj file alongside model
    // For basic implementation, encode image as base64 in prompt
    override suspend fun generateWithImage(prompt: String, imageBytes: ByteArray): String {
        val base64 = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
        return generate("[IMG:$base64]\n$prompt")
    }

    override fun unload() {
        helper = null
        isLoaded = false
        modelName = null
    }
}
