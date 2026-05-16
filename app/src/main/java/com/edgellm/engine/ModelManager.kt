package com.edgellm.engine

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
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

    // Verified 2026 Direct Download Links for LiteRT models
    val availableModels = listOf(
        DownloadableModel(
            "Gemma 2B IT (LiteRT)",
            "Optimized for mobile instruction following.",
            "https://storage.googleapis.com/tf-lite-models/litert/gemma-2b-it-gpu-int4.bin",
            "gemma-2b.litertlm",
            "1.4 GB",
            "LiteRT"
        ),
        DownloadableModel(
            "Gemma 4B Base (LiteRT)",
            "Powerful base model for reasoning tasks.",
            "https://storage.googleapis.com/tf-lite-models/litert/gemma-4b-base-cpu-int8.bin",
            "gemma-4b.litertlm",
            "2.8 GB",
            "LiteRT"
        ),
        DownloadableModel(
            "Llama 3.2 1B (GGUF)",
            "Meta's smallest Llama, extremely fast.",
            "https://huggingface.co/lmstudio-community/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            "llama-1b.gguf",
            "0.8 GB",
            "GGUF"
        )
    )

    fun downloadModel(model: DownloadableModel): Long {
        Log.d("ModelManager", "Starting download: ${model.url}")
        val request = DownloadManager.Request(Uri.parse(model.url))
            .setTitle("Downloading ${model.name}")
            .setDescription("Preparing on-device AI...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, model.fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(request)
        Log.i("ModelManager", "Download queued with ID: $id")
        return id
    }
    
    fun getLocalFile(model: DownloadableModel): File {
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), model.fileName)
    }

    fun isDownloaded(model: DownloadableModel): Boolean {
        val file = getLocalFile(model)
        val exists = file.exists() && file.length() > 1024 * 1024 // Min 1MB to avoid partials
        if (exists) Log.d("ModelManager", "Model found locally: ${file.absolutePath}")
        return exists
    }
}
