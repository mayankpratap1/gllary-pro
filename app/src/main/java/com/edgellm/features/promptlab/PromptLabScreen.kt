package com.edgellm.features.promptlab

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptLabScreen(vm: PromptLabViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var prompt by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf(0.7f) }
    var topK by remember { mutableStateOf(40f) }
    var maxTokens by remember { mutableStateOf(512f) }
    var showConfig by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Prompt Lab", style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = { showConfig = !showConfig }) {
                Text(if (showConfig) "Hide Config" else "Config")
            }
        }

        // Config panel
        if (showConfig) {
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Temperature: ${"%.2f".format(temperature)}")
                    Slider(value = temperature, onValueChange = { temperature = it }, valueRange = 0f..2f)

                    Text("Top-K: ${topK.toInt()}")
                    Slider(value = topK, onValueChange = { topK = it }, valueRange = 1f..100f)

                    Text("Max Tokens: ${maxTokens.toInt()}")
                    Slider(value = maxTokens, onValueChange = { maxTokens = it }, valueRange = 64f..2048f)

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = { Text("System prompt (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }
        }

        // Preset templates
        Text("Templates:", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val templates = listOf("Summarize", "Rewrite", "Code", "Explain")
            for (tmpl in templates) {
                SuggestionChip(
                    onClick = { prompt = tmpl },
                    label = { Text(tmpl, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            placeholder = { Text("Enter your prompt…") }
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                vm.run(
                    prompt = prompt,
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    topK = topK.toInt(),
                    maxTokens = maxTokens.toInt()
                )
            },
            enabled = prompt.isNotBlank() && !state.isRunning,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.isRunning) "Running…" else "Run") }

        state.result?.let { result ->
            Spacer(Modifier.height(16.dp))
            // Stats bar like Edge Gallery
            state.stats?.let { stats ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text("${stats.tokensPerSec} tok/s", style = MaterialTheme.typography.labelSmall)
                    Text("${stats.totalTokens} tokens", style = MaterialTheme.typography.labelSmall)
                    Text("${stats.latencyMs}ms first token", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(8.dp))
            }
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
