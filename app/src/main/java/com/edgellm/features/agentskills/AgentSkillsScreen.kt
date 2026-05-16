package com.edgellm.features.agentskills

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgellm.features.chat.MessageBubble
import com.edgellm.skills.Skill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentSkillsScreen(
    vm: AgentSkillsViewModel = viewModel(),
    skills: List<Skill>
) {
    val state by vm.state.collectAsState()
    var activeSkills by remember { mutableStateOf(setOf<String>()) }
    var input by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Skills") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, "Add skill")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp) {
                Column {
                    // Skill Selector above input
                    Text(
                        "Active Agents:",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(skills) { skill ->
                            FilterChip(
                                selected = skill.name in activeSkills,
                                onClick = {
                                    activeSkills = if (skill.name in activeSkills)
                                        activeSkills - skill.name else activeSkills + skill.name
                                },
                                label = { Text(skill.name) },
                                leadingIcon = if (skill.name in activeSkills) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

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
                            placeholder = { Text("Ask using active skills...") },
                            enabled = !state.isGenerating,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (input.isNotBlank()) {
                                    vm.sendWithSkills(input, skills.filter { it.name in activeSkills })
                                    input = ""
                                }
                            },
                            enabled = input.isNotBlank() && !state.isGenerating,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, null)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Select a skill above and start chatting with the agent.", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(state.messages) { msg ->
                    MessageBubble(msg)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Import Agent Skill") },
            text = {
                Column {
                    Text("Enter the URL to a SKILL.md file (Markdown format with frontmatter).", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("https://raw.githubusercontent.com/...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addSkillFromUrl(urlInput)
                    showAddDialog = false
                    urlInput = ""
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
