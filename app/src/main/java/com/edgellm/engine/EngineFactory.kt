package com.edgellm.engine

import android.content.ContentResolver

object EngineFactory {
    fun create(uri: String, context: android.content.Context): InferenceEngine {
        return when {
            uri.contains(".gguf", ignoreCase = true) -> GgufEngine(context.contentResolver)
            uri.contains(".litertlm", ignoreCase = true) -> LiteRtEngine(context)
            else -> throw IllegalArgumentException("Unknown model format")
        }
    }
}
