package com.edgellm.engine

import android.content.ContentResolver

object EngineFactory {
    /**
     * Returns the correct engine based on file extension.
     * .gguf → GgufEngine
     * .litertlm → LiteRtEngine
     */
    fun create(uri: String, contentResolver: ContentResolver): InferenceEngine {
        return when {
            uri.endsWith(".gguf", ignoreCase = true) ||
            uri.contains(".gguf", ignoreCase = true) -> GgufEngine(contentResolver)
            uri.endsWith(".litertlm", ignoreCase = true) ||
            uri.contains(".litertlm", ignoreCase = true) -> LiteRtEngine()
            else -> throw IllegalArgumentException(
                "Unknown model format. Use .gguf or .litertlm"
            )
        }
    }
}
