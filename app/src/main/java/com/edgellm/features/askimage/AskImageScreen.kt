package com.edgellm.features.askimage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
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

    // Gallery picker
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { vm.setImage(context, it) } }

    // Camera capture
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) { /* image already set via vm.captureUri */ } }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ask Image", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Image preview
        state.imageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        // Pick image buttons
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoLibrary, null)
                Spacer(Modifier.width(4.dp))
                Text("Gallery")
            }
            OutlinedButton(
                onClick = {
                    val uri = vm.createCaptureUri(context)
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(4.dp))
                Text("Camera")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Prompt templates (like Edge Gallery)
        Text("Quick prompts:", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        listOf(
            "Describe this image in detail",
            "What objects are in this image?",
            "Solve this visual puzzle",
            "Extract all text from this image",
            "Write a design brief for this"
        ).forEach { template ->
            FilterChip(
                selected = prompt == template,
                onClick = { prompt = template },
                label = { Text(template, style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ask about this image…") }
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { vm.analyze(prompt) },
            enabled = state.imageBytes != null && prompt.isNotBlank() && !state.isAnalyzing,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.isAnalyzing) "Analyzing…" else "Analyze") }

        state.result?.let { result ->
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(result, Modifier.padding(12.dp))
            }
        }
    }
}
