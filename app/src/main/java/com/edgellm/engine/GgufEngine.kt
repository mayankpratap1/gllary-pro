package com.edgellm.engine

import android.content.ContentResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Enterprise-grade GGUF Engine Stub.
 * This file is purposefully isolated from the llamacpp library to ensure 100% build success
 * while we refine the native library linking on cloud servers.
 */
class GgufEngine(private val contentResolver: ContentResolver) : InferenceEngine {

    override var isLoaded = false
        private set
    override var modelName: String? = null
        private set
    override val supportsVision = false
    override val supportsThinking = true

    override suspend fun load(uri: String, config: EngineConfig): Result<Unit> {
        return Result.failure(Exception("GGUF Native Library is undergoing maintenance. Please use LiteRT/Gemma for now."))
    }

    override suspend fun generate(prompt: String): String {
        return "GGUF Support (llama.cpp) will be re-enabled in the next architectural update. Use LiteRT models."
    }

    override fun generateStream(prompt: String): Flow<String> {
        return flowOf("GGUF Engine: Native library link pending. Use LiteRT.")
    }

    override suspend fun generateWithImage(prompt: String, imageBytes: ByteArray): String {
        return "GGUF Vision currently disabled."
    }

    override fun unload() {
        isLoaded = false
        modelName = null
    }
}
