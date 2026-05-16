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
import androidx.compose.ui.Alignment
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

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.setImage(context, it) } }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) { /* already handled in captureUri */ } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Vision Analysis") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState) // CRITICAL: Enables scrolling for small screens
                .padding(16.dp)
        ) {
            // Image Preview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                if (state.imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(state.imageUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Image Selected", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Selection Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Library")
                }
                Button(onClick = {
                    val uri = vm.createCaptureUri(context)
                    cameraLauncher.launch(uri)
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Camera")
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Conversation Prompt", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What's in this image?") },
                minLines = 2
            )

            Spacer(Modifier.height(16.dp))

            // Action Button - Always visible at bottom of content
            Button(
                onClick = { vm.analyze(prompt) },
                enabled = state.imageBytes != null && prompt.isNotBlank() && !state.isAnalyzing,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (state.isAnalyzing) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Run Multi-Modal Analysis")
                }
            }

            state.result?.let { result ->
                Spacer(Modifier.height(24.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(result, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            // Padding for the bottom
            Spacer(Modifier.height(48.dp))
        }
    }
}
