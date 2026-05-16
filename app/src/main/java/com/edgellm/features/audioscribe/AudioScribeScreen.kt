package com.edgellm.features.audioscribe

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScribeScreen(vm: AudioScribeViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { /* In a pro app, you'd send this to a transcription engine */ } }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Audio Lab") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Voice & Audio Analysis",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Convert voice to text or upload audio files for AI processing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Record button
            LargeRecordButton(
                isRecording = state.isRecording,
                onClick = {
                    if (state.isRecording) vm.stopRecording()
                    else vm.startRecording(context)
                }
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (state.isRecording) "Recording..." else "Tap to Start Recording",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(Modifier.height(48.dp))

            // File Upload Section
            OutlinedButton(
                onClick = { filePicker.launch("audio/*") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.AudioFile, null)
                Spacer(Modifier.width(12.dp))
                Text("Upload Audio File (.mp3, .opus, .wav)")
            }

            if (state.isProcessing) {
                Spacer(Modifier.height(32.dp))
                CircularProgressIndicator()
                Text("AI is processing audio...", Modifier.padding(top = 16.dp))
            }

            state.transcript?.let { text ->
                Spacer(Modifier.height(32.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Transcription / AI Analysis", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            state.error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            
            Spacer(Modifier.height(64.dp))
        }
    }
}

@Composable
fun LargeRecordButton(isRecording: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = if (isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(100.dp),
        shadowElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}
