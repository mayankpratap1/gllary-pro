package com.edgellm.features.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var showThinking by remember { mutableStateOf(false) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty())
            listState.animateScrollToItem(state.messages.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        // Toolbar
        @OptIn(ExperimentalMaterial3Api::class)
        TopAppBar(
            title = { Text("AI Chat") },
            actions = {
                // Thinking Mode toggle (like Edge Gallery)
                IconToggleButton(
                    checked = showThinking,
                    onCheckedChange = { showThinking = it }
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = "Thinking Mode",
                        tint = if (showThinking) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.messages) { msg ->
                MessageBubble(msg, showThinking)
            }
            if (state.isGenerating) {
                item { GeneratingIndicator() }
            }
        }

        // Input row
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message…") },
                enabled = state.modelLoaded && !state.isGenerating
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { vm.sendMessage(input); input = "" },
                enabled = input.isNotBlank() && state.modelLoaded && !state.isGenerating
            ) { Text("Send") }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage, showThinking: Boolean) {
    val isUser = msg.role == "user"
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Thinking section (collapsible, like Edge Gallery)
        if (!isUser && msg.thinkingContent != null && showThinking) {
            AnimatedVisibility(visible = true) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(
                            "💭 Thinking",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            msg.thinkingContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = msg.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun GeneratingIndicator() {
    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text("Generating…", style = MaterialTheme.typography.bodySmall)
    }
}

// Data classes for chat
data class ChatMessage(
    val role: String,
    val content: String,
    val thinkingContent: String? = null  // Extracted from <think>...</think> tags
)
