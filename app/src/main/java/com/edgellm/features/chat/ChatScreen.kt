package com.edgellm.features.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Chat", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .imePadding()
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        maxLines = 4,
                        enabled = !state.isGenerating,
                        shape = MaterialTheme.shapes.extraLarge
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            if (input.isNotBlank()) {
                                vm.sendMessage(input)
                                input = ""
                            }
                        },
                        enabled = input.isNotBlank() && !state.isGenerating,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Default.Send, null)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            items(state.messages) { msg ->
                MessageBubble(msg)
            }

            if (state.isGenerating) {
                item { GeneratingIndicator() }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    var isThinkingExpanded by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Collapsible Thinking Section
        if (!isUser && msg.thinkingContent != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 6.dp)
                    .clickable { isThinkingExpanded = !isThinkingExpanded },
                shape = MaterialTheme.shapes.small
            ) {
                Column(Modifier.padding(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Thinking Process", style = MaterialTheme.typography.labelSmall)
                        }
                        Icon(
                            if (isThinkingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (isThinkingExpanded) {
                        Divider(Modifier.padding(vertical = 4.dp), alpha = 0.2f)
                        Text(
                            text = msg.thinkingContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = if (isUser) 
                MaterialTheme.shapes.medium.copy(bottomEnd = CornerSize(0.dp)) 
                else MaterialTheme.shapes.medium.copy(bottomStart = CornerSize(0.dp)),
            modifier = Modifier.widthIn(max = 300.dp)
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
    Row(
        Modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text("AI is generating response...", style = MaterialTheme.typography.labelSmall)
    }
}

private fun CornerSize(size: androidx.compose.ui.unit.Dp) = androidx.compose.foundation.shape.CornerSize(size)
