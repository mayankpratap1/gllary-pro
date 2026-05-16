package com.edgellm.engine

import android.content.ContentResolver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * GGUF Engine Stub.
 * Resolves unresolved reference 'github' to allow APK build.
 * Replace with actual implementation once llamacpp-kotlin is verified.
 */
class GgufEngine(private val contentResolver: ContentResolver) : InferenceEngine {

    override var isLoaded = false
        private set
    override var modelName: String? = null
        private set
    override val supportsVision = true
    override val supportsThinking = true

    override suspend fun load(uri: String, config: EngineConfig): Result<Unit> {
        return Result.failure(Exception("GGUF Engine not yet supported in this build environment."))
    }

    override suspend fun generate(prompt: String): String {
        return "GGUF Engine stub: Please use LiteRT models."
    }

    override fun generateStream(prompt: String): Flow<String> {
        return flowOf("GGUF Engine stub: Please use LiteRT models.")
    }

    override suspend fun generateWithImage(prompt: String, imageBytes: ByteArray): String {
        return "GGUF Engine stub: Vision not available."
    }

    override fun unload() {
        isLoaded = false
        modelName = null
    }
}
