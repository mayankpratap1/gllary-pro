package com.edgellm.features.benchmark

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun BenchmarkScreen(vm: BenchmarkViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Benchmark", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { vm.runBenchmark(context) },
            enabled = !state.isRunning,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.isRunning) "Running…" else "Run Benchmark") }

        Spacer(Modifier.height(16.dp))

        state.results?.let { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Results", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BenchRow("Model load time", "${r.loadTimeMs} ms")
                    BenchRow("Prefill speed", "${r.prefillToksPerSec} tok/s")
                    BenchRow("Decode speed", "${r.decodeToksPerSec} tok/s")
                    BenchRow("Time to first token", "${r.ttftMs} ms")
                    BenchRow("Peak memory", "${r.peakMemMb} MB")
                    BenchRow("Engine", r.engineType)
                    BenchRow("Backend", r.backend)
                }
            }
        }

        if (state.isRunning) {
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(state.status, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun BenchRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary)
    }
}
