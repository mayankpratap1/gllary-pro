package com.edgellm.features.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.edgellm.service.EdgeLLMService
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(service: EdgeLLMService?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    
    // Engine Config State
    var useGpu by remember { mutableStateOf(true) }
    var useNpu by remember { mutableStateOf(false) }

    val modelPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isLoading = true
            statusMessage = "Loading model..."
            scope.launch {
                val config = com.edgellm.engine.EngineConfig(useGpu = useGpu, useNpu = useNpu)
                val result = service?.loadModel(it.toString(), config)
                isLoading = false
                statusMessage = if (result?.isSuccess == true) {
                    "Model loaded: ${service.currentEngine?.modelName}"
                } else {
                    "Failed to load: ${result?.exceptionOrNull()?.message}"
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Model Configuration", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                
                Button(
                    onClick = { modelPicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load Model (.gguf or .litertlm)")
                }
                
                if (statusMessage.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(statusMessage, style = MaterialTheme.typography.bodySmall)
                }
                
                if (isLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = useGpu, onCheckedChange = { useGpu = it })
                    Text("Use GPU (LiteRT)", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = useNpu, onCheckedChange = { useNpu = it })
                    Text("Use NPU (LiteRT)", style = MaterialTheme.typography.bodySmall)
                }
                
                service?.currentEngine?.let { engine ->
                    Spacer(Modifier.height(16.dp))
                    Text("Current Engine: ${engine.javaClass.simpleName}")
                    Text("Model Name: ${engine.modelName}")
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("API Server", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                
                var port by remember { mutableStateOf("8080") }
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Server Port") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(8.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { service?.startServer(port.toIntOrNull() ?: 8080) },
                        modifier = Modifier.weight(1f),
                        enabled = service?.currentEngine?.isLoaded == true
                    ) {
                        Text("Start Server")
                    }
                    OutlinedButton(
                        onClick = { service?.stopServer() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stop Server")
                    }
                }
            }
        }
    }
}
