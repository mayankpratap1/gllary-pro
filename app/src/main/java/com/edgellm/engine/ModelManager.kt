package com.edgellm.engine

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class DownloadableModel(
    val name: String,
    val description: String,
    val url: String,
    val fileName: String,
    val size: String,
    val type: String // "LiteRT" or "GGUF"
)

class ModelManager(private val context: Context) {

    private val _downloadProgress = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<Long, Float>> = _downloadProgress

    val availableModels = listOf(
        DownloadableModel(
            "Gemma 2B (LiteRT)",
            "Google's lightweight 2B model, perfect for most phones.",
            "https://storage.googleapis.com/aju-static/litert/gemma-2b-it-gpu-int4.bin", // Example URL
            "gemma-2b.litertlm",
            "1.4 GB",
            "LiteRT"
        ),
        DownloadableModel(
            "Llama 3.2 1B (GGUF)",
            "Extremely fast 1B model by Meta.",
            "https://huggingface.co/lmstudio-community/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            "llama-3.2-1b.gguf",
            "0.8 GB",
            "GGUF"
        )
    )

    fun downloadModel(model: DownloadableModel): Long {
        val request = DownloadManager.Request(Uri.parse(model.url))
            .setTitle("Downloading ${model.name}")
            .setDescription("EdgeLLM Model Download")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, model.fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }
    
    fun getLocalFile(model: DownloadableModel): File {
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), model.fileName)
    }

    fun isDownloaded(model: DownloadableModel): Boolean {
        return getLocalFile(model).exists()
    }
}
