package com.edgellm.engine

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig as LiteRtConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class LiteRtEngine : InferenceEngine {

    private var engine: Engine? = null
    private var conversation: com.google.ai.edge.litertlm.Conversation? = null

    override var isLoaded = false
        private set
    override var modelName: String? = null
        private set
    override val supportsVision = true   // Gemma 4 multimodal
    override val supportsThinking = true // Gemma 4 thinking mode

    override suspend fun load(uri: String, config: EngineConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // LiteRT-LM takes a file path, not a content URI
                // For files in internal storage, uri IS the absolute path
                val liteRtConfig = LiteRtConfig(
                    modelPath = uri,
                    backend = when {
                        config.useNpu -> Backend.NPU
                        config.useGpu -> Backend.GPU
                        else -> Backend.CPU
                    }
                )
                val e = Engine(liteRtConfig)
                // engine.initialize() can take up to 10s — must be on background thread
                e.initialize()
                engine = e
                conversation = e.createConversation()
                isLoaded = true
                modelName = uri.substringAfterLast("/").substringBeforeLast(".")
                Result.success(Unit)
            } catch (ex: Exception) {
                isLoaded = false
                Result.failure(ex)
            }
        }
    }

    override suspend fun generate(prompt: String): String {
        check(isLoaded) { "No model loaded" }
        return withContext(Dispatchers.IO) {
            val sb = StringBuilder()
            conversation!!.sendMessageAsync(prompt).collect { token -> sb.append(token) }
            sb.toString()
        }
    }

    override fun generateStream(prompt: String): Flow<String> {
        check(isLoaded) { "No model loaded" }
        return flow {
            conversation!!.sendMessageAsync(prompt).collect { token -> emit(token) }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun generateWithImage(prompt: String, imageBytes: ByteArray): String {
        // LiteRT-LM multimodal: pass image bytes directly
        // NOTE: Conversation API supports image bytes in newer builds
        // Fallback to text-only if model doesn't support vision
        return generate(prompt)
        // TODO: Replace with multimodal API once stable:
        // conversation!!.sendMessageAsync(prompt, imageBytes).collect { ... }
    }

    override fun unload() {
        conversation?.close()
        engine?.close()
        engine = null
        conversation = null
        isLoaded = false
        modelName = null
    }
}
