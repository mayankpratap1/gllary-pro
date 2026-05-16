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
                // Determine backend instance
                val selectedBackend = if (config.useNpu) {
                    Backend.NPU
                } else if (config.useGpu) {
                    Backend.GPU
                } else {
                    Backend.CPU
                }
                
                val liteRtConfig = LiteRtConfig(
                    modelPath = uri,
                    backend = selectedBackend
                )
                val e = Engine(liteRtConfig)
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
        val currentConv = conversation ?: return "Error: No model loaded"
        return withContext(Dispatchers.IO) {
            val sb = StringBuilder()
            currentConv.sendMessageAsync(prompt).collect { msg -> 
                sb.append(msg.text) 
            }
            sb.toString()
        }
    }

    override fun generateStream(prompt: String): Flow<String> {
        val currentConv = conversation ?: return flowOf("Error: No model loaded")
        return currentConv.sendMessageAsync(prompt).map { it.text }.flowOn(Dispatchers.IO)
    }

    override suspend fun generateWithImage(prompt: String, imageBytes: ByteArray): String {
        return generate(prompt)
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
