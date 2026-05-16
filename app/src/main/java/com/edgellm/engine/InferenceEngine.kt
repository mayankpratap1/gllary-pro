package com.edgellm.engine

import kotlinx.coroutines.flow.Flow

/**
 * Common interface implemented by both GgufEngine and LiteRtEngine.
 * All features (Chat, AskImage, AudioScribe, PromptLab) talk to this
 * interface — they never call engine-specific code directly.
 */
interface InferenceEngine {

    val isLoaded: Boolean
    val modelName: String?
    val supportsVision: Boolean   // true for LLaVA-GGUF and LiteRT multimodal
    val supportsThinking: Boolean // true for Gemma 4 / reasoning models

    /** Load model. Uri = content:// for GGUF, file path for LiteRT. */
    suspend fun load(uri: String, config: EngineConfig = EngineConfig()): Result<Unit>

    /** Blocking generate — returns full text. */
    suspend fun generate(prompt: String): String

    /** Streaming generate — emits tokens as Flow. */
    fun generateStream(prompt: String): Flow<String>

    /** Vision generate — for Ask Image feature. imageBytes = JPEG bytes. */
    suspend fun generateWithImage(prompt: String, imageBytes: ByteArray): String

    /** Unload and free memory. */
    fun unload()
}

data class EngineConfig(
    val contextLength: Int = 2048,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val maxTokens: Int = 512,
    val useGpu: Boolean = true,    // LiteRT only
    val useNpu: Boolean = false    // LiteRT only, Qualcomm/MediaTek
)
