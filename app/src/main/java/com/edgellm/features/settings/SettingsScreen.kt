package com.edgellm.features.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.edgellm.engine.ModelManager
import com.edgellm.service.EdgeLLMService
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(service: EdgeLLMService?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val modelManager = remember { ModelManager(context) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    
    var useGpu by remember { mutableStateOf(true) }

    val modelPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isLoading = true
            statusMessage = "Loading model..."
            scope.launch {
                val config = com.edgellm.engine.EngineConfig(useGpu = useGpu)
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

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Model Configuration", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    
                    Button(
                        onClick = { modelPicker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pick Local Model")
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = useGpu, onCheckedChange = { useGpu = it })
                        Text("Use GPU acceleration", style = MaterialTheme.typography.bodySmall)
                    }

                    if (statusMessage.isNotEmpty()) {
                        Text(statusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            Text("Model Library (Native Downloads)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }

        items(modelManager.availableModels) { model ->
            OutlinedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(model.name, style = MaterialTheme.typography.bodyLarge)
                        Text(model.description, style = MaterialTheme.typography.bodySmall)
                        Text("${model.size} • ${model.type}", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = {
                        if (modelManager.isDownloaded(model)) {
                            isLoading = true
                            scope.launch {
                                service?.loadModel(modelManager.getLocalFile(model).absolutePath)
                                isLoading = false
                            }
                        } else {
                            modelManager.downloadModel(model)
                        }
                    }) {
                        Icon(
                            imageVector = if (modelManager.isDownloaded(model)) 
                                           androidx.compose.material.icons.Icons.Default.Download 
                                           else androidx.compose.material.icons.Icons.Default.Download,
                            contentDescription = "Download",
                            tint = if (modelManager.isDownloaded(model)) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("API Server", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    var port by remember { mutableStateOf("8080") }
                    OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") })
                    Row(Modifier.padding(top = 8.dp)) {
                        Button(onClick = { service?.startServer(port.toIntOrNull() ?: 8080) }) { Text("Start") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { service?.stopServer() }) { Text("Stop") }
                    }
                }
            }
        }
    }
}
