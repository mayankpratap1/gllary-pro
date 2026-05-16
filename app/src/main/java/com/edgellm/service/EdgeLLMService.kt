package com.edgellm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.edgellm.R
import com.edgellm.engine.EngineFactory
import com.edgellm.engine.InferenceEngine
import com.edgellm.server.ApiServer
import com.edgellm.skills.SkillManager
import kotlinx.coroutines.*

class EdgeLLMService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): EdgeLLMService = this@EdgeLLMService
    }

    private val binder = LocalBinder()
    
    var currentEngine: InferenceEngine? = null
        private set
        
    private var apiServer: ApiServer? = null
    lateinit var skillManager: SkillManager
        private set

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        skillManager = SkillManager(this)
        scope.launch { skillManager.loadAll() }
        createNotificationChannel()
        startForeground(1, buildNotification("EdgeLLM Service Started"))
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        apiServer?.stop()
        currentEngine?.unload()
        scope.cancel()
        super.onDestroy()
    }

    suspend fun loadModel(uriString: String, config: com.edgellm.engine.EngineConfig = com.edgellm.engine.EngineConfig()): Result<Unit> {
        currentEngine?.unload()
        
        val finalUri = if (uriString.startsWith("content://") && uriString.contains(".litertlm")) {
            copyToInternalStorage(uriString) ?: uriString
        } else {
            uriString
        }
        
        val newEngine = EngineFactory.create(finalUri, contentResolver)
        val result = newEngine.load(finalUri, config)
        if (result.isSuccess) {
            currentEngine = newEngine
        }
        return result
    }

    private fun copyToInternalStorage(uriString: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            val fileName = "loaded_model.litertlm"
            val destFile = java.io.File(filesDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun startServer(port: Int = 8080) {
        val engine = currentEngine ?: return
        if (apiServer == null) {
            apiServer = ApiServer(engine)
        }
        apiServer?.start(port)
        updateNotification("API Server running on port $port")
    }

    fun stopServer() {
        apiServer?.stop()
        apiServer = null
        updateNotification("API Server stopped")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "edgellm_channel",
                "EdgeLLM Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String) = NotificationCompat.Builder(this, "edgellm_channel")
        .setContentTitle("EdgeLLM Pro")
        .setContentText(text)
        .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback icon since we don't have R.mipmap.ic_launcher yet
        .setOngoing(true)
        .build()

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(1, buildNotification(text))
    }
}
