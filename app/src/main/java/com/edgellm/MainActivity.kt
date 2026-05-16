package com.edgellm

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.edgellm.service.EdgeLLMService
import com.edgellm.ui.MainNavigation
import com.edgellm.ui.theme.EdgeLLMProTheme

// CompositionLocal to provide the service to all screens
val LocalEdgeLLMService = staticCompositionLocalOf<EdgeLLMService?> { null }

class MainActivity : ComponentActivity() {
    private var edgeService by mutableStateOf<EdgeLLMService?>(null)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as EdgeLLMService.LocalBinder
            edgeService = binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            edgeService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start and bind the service
        val intent = Intent(this, EdgeLLMService::class.java)
        startForegroundService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        setContent {
            // Request permissions at startup
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Handle results if needed */ }

            LaunchedEffect(Unit) {
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.READ_MEDIA_IMAGES
                ))
            }

            EdgeLLMProTheme {
                CompositionLocalProvider(LocalEdgeLLMService provides edgeService) {
                    MainNavigation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}
