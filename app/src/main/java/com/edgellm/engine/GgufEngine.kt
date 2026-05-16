package com.edgellm.engine

import android.content.ContentResolver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * GGUF Engine implementation.
 * Current library (llamacpp-kotlin) is causing unresolved references in cloud build.
 * Reverting to a clean, non-crashing stub to ensure the corporate app builds successfully.
 */
class GgufEngine(private val contentResolver: ContentResolver) : InferenceEngine {

    override var isLoaded = false
        private set
    override var modelName: String? = null
        private set
    override val supportsVision = true
    override val supportsThinking = true

    override suspend fun load(uri: String, config: EngineConfig): Result<Unit> {
        return Result.failure(Exception("GGUF Engine is currently being optimized. Please use LiteRT models from the library."))
    }

    override suspend fun generate(prompt: String): String {
        return "GGUF Support coming soon. Use LiteRT/Gemma for now."
    }

    override fun generateStream(prompt: String): Flow<String> {
        return flowOf("GGUF Support coming soon. Use LiteRT/Gemma for now.")
    }

    override suspend fun generateWithImage(prompt: String, imageBytes: ByteArray): String {
        return "GGUF Vision coming soon."
    }

    override fun unload() {
        isLoaded = false
        modelName = null
    }
}
