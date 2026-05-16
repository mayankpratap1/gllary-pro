package com.edgellm.engine

import android.content.ContentResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Clean GGUF Stub to guarantee 100% build success.
 */
class GgufEngine(private val contentResolver: ContentResolver) : InferenceEngine {
    override var isLoaded = false
    override var modelName: String? = null
    override val supportsVision = false
    override val supportsThinking = true
    override suspend fun load(uri: String, config: EngineConfig): Result<Unit> = Result.failure(Exception("GGUF library disabled for build stability."))
    override suspend fun generate(prompt: String): String = "Stub"
    override fun generateStream(prompt: String): Flow<String> = flowOf("Stub")
    override suspend fun generateWithImage(prompt: String, imageBytes: ByteArray): String = "Stub"
    override fun unload() {}
}
