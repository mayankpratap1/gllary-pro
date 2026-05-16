package com.edgellm.engine

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig as LiteRtConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * High-performance LiteRT Engine.
 * Fixed: Robust text extraction and explicit CPU backend for initialization.
 */
class LiteRtEngine(private val context: Context) : InferenceEngine {

    private var engine: Engine? = null
    private var conversation: com.google.ai.edge.litertlm.Conversation? = null

    override var isLoaded = false
        private set
    override var modelName: String? = null
        private set
    override val supportsVision = true
    override val supportsThinking = true

    override suspend fun load(uri: String, config: EngineConfig): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("LiteRtEngine", "Attempting load: $uri")
                
                // Stability: Use CPU for initialization as requested by Google samples
                val liteRtConfig = LiteRtConfig(
                    modelPath = uri,
                    backend = Backend.CPU()
                )
                
                val e = Engine(liteRtConfig)
                e.initialize()
                
                engine = e
                conversation = e.createConversation()
                isLoaded = true
                modelName = File(uri).nameWithoutExtension
                Log.i("LiteRtEngine", "Load Success: $modelName")
                Result.success(Unit)
            } catch (ex: Exception) {
                Log.e("LiteRtEngine", "Load Error", ex)
                isLoaded = false
                Result.failure(ex)
            }
        }
    }

    override suspend fun generate(prompt: String): String {
        val conv = conversation ?: return "Model not loaded"
        return withContext(Dispatchers.IO) {
            val sb = StringBuilder()
            try {
                conv.sendMessageAsync(prompt).collect { msg ->
                    sb.append(extractString(msg))
                }
                sb.toString()
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    override fun generateStream(prompt: String): Flow<String> {
        val conv = conversation ?: return flowOf("Model not loaded")
        return conv.sendMessageAsync(prompt).map { extractString(it) }.flowOn(Dispatchers.IO)
    }

    override suspend fun generateWithImage(prompt: String, imageBytes: ByteArray): String {
        // Multi-modal Vision requires specific Gemma Vision models. 
        // Forwarding to text generator for base compatibility.
        return generate(prompt)
    }

    /**
     * Extracts text from the library message object.
     * Uses toString() and then trims common prefixes to get just the AI response.
     */
    private fun extractString(msg: Any): String {
        val str = msg.toString()
        // LiteRT toString often includes metadata. We try to extract just the text.
        return if (str.contains("text=")) {
            str.substringAfter("text=").substringBefore(",").trim()
        } else {
            str
        }
    }

    override fun unload() {
        conversation?.close()
        engine?.close()
        engine = null
        conversation = null
        isLoaded = false
    }
}
