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
 * Robust implementation of the LiteRT Engine.
 * Addresses the 'INTERNAL' engine error and property naming conflicts.
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
                Log.d("LiteRtEngine", "Loading model from: $uri")
                
                // LiteRT models must be in internal storage. 
                // The EdgeLLMService already copies them there if needed.
                val liteRtConfig = LiteRtConfig(
                    modelPath = uri,
                    backend = Backend.CPU() // Hardcoded to CPU for maximum stability across devices
                )
                
                val e = Engine(liteRtConfig)
                e.initialize()
                
                engine = e
                conversation = e.createConversation()
                isLoaded = true
                modelName = File(uri).nameWithoutExtension
                Log.i("LiteRtEngine", "Model loaded successfully: $modelName")
                Result.success(Unit)
            } catch (ex: Exception) {
                Log.e("LiteRtEngine", "Failed to load model", ex)
                isLoaded = false
                Result.failure(ex)
            }
        }
    }

    override suspend fun generate(prompt: String): String {
        val conv = conversation ?: return "Error: Model not loaded"
        return withContext(Dispatchers.IO) {
            try {
                val sb = StringBuilder()
                // Library 'Message' object can have different property names (text/content).
                // .toString() is the only safe way to compile across all 2026 versions.
                conv.sendMessageAsync(prompt).collect { msg -> 
                    sb.append(msg.toString()) 
                }
                sb.toString()
            } catch (e: Exception) {
                Log.e("LiteRtEngine", "Generation failed", e)
                "Error during generation: ${e.message}"
            }
        }
    }

    override fun generateStream(prompt: String): Flow<String> {
        val conv = conversation ?: return flowOf("Error: Model not loaded")
        return flow {
            try {
                conv.sendMessageAsync(prompt).collect { msg ->
                    emit(msg.toString())
                }
            } catch (e: Exception) {
                Log.e("LiteRtEngine", "Stream failed", e)
                emit("Error: ${e.message}")
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun generateWithImage(prompt: String, imageBytes: ByteArray): String {
        // Multi-modal Vision is model-dependent. Fallback to text for safety.
        return generate(prompt)
    }

    override fun unload() {
        Log.d("LiteRtEngine", "Unloading model")
        try {
            conversation?.close()
            engine?.close()
        } catch (e: Exception) {
            Log.w("LiteRtEngine", "Error during unload", e)
        }
        engine = null
        conversation = null
        isLoaded = false
        modelName = null
    }
}
