package com.edgellm.features.audioscribe

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScribeScreen(vm: AudioScribeViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var mode by remember { mutableStateOf("transcribe") } // or "translate"
    var targetLang by remember { mutableStateOf("English") }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Audio Scribe", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        // Mode selector
        Row {
            FilterChip(
                selected = mode == "transcribe",
                onClick = { mode = "transcribe" },
                label = { Text("Transcribe") }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = mode == "translate",
                onClick = { mode = "translate" },
                label = { Text("Translate") }
            )
        }

        if (mode == "translate") {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = targetLang,
                onValueChange = { targetLang = it },
                label = { Text("Target language") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(32.dp))

        // Record button
        val context = androidx.compose.ui.platform.LocalContext.current
        FloatingActionButton(
            onClick = {
                if (state.isRecording) vm.stopRecording()
                else vm.startRecording(context)
            },
            containerColor = if (state.isRecording) MaterialTheme.colorScheme.error
                             else MaterialTheme.colorScheme.primary
        ) {
            Icon(
                if (state.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (state.isRecording) "Stop" else "Record"
            )
        }

        Text(
            text = if (state.isRecording) "Recording… tap to stop" else "Tap to record",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(16.dp))

        if (state.isProcessing) {
            CircularProgressIndicator()
            Text("Processing audio…", style = MaterialTheme.typography.bodySmall)
        }

        state.transcript?.let { text ->
            Spacer(Modifier.height(16.dp))
            Text(
                "Result:",
                style = MaterialTheme.typography.labelMedium
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text, Modifier.padding(12.dp))
            }
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
