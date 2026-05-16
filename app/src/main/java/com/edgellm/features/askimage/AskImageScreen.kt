package com.edgellm.features.askimage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskImageScreen(vm: AskImageViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.setImage(context, it) } }

    // Camera capture
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) { /* already handled in capture logic */ } }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Vision Lab") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState) // CRITICAL: Fixes hidden buttons
        ) {
            // Image preview
            state.imageUri?.let { uri ->
                Card(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Pick image buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Library")
                }
                Button(
                    onClick = {
                        val uri = vm.createCaptureUri(context)
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Camera")
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Ask the AI about this image:", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Describe this image in detail") },
                minLines = 3
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { vm.analyze(prompt) },
                enabled = state.imageBytes != null && prompt.isNotBlank() && !state.isAnalyzing,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (state.isAnalyzing) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Analyzing...")
                } else {
                    Text("Run Vision AI")
                }
            }

            state.result?.let { result ->
                Spacer(Modifier.height(24.dp))
                Text("Result", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(result, Modifier.padding(16.dp))
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}
