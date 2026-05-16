package com.edgellm.features.agentskills

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Agent Skills", style = MaterialTheme.typography.headlineMedium)
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add skill")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Skill chips (multi-select)
        Text("Active Skills:", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(skills) { skill ->
                FilterChip(
                    selected = skill.name in activeSkills,
                    onClick = {
                        activeSkills = if (skill.name in activeSkills)
                            activeSkills - skill.name else activeSkills + skill.name
                    },
                    label = { Text(skill.name) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Chat history
        state.messages.forEach { msg ->
            Surface(
                color = if (msg.role == "user") MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(msg.content, Modifier.padding(12.dp))
            }
        }

        Spacer(Modifier.weight(1f))

        // Input
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask with skills…") }
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    vm.sendWithSkills(
                        input,
                        skills.filter { it.name in activeSkills }
                    )
                    input = ""
                },
                enabled = input.isNotBlank() && !state.isGenerating
            ) { Text("Send") }
        }
    }

    // Add skill dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Skill from URL") },
            text = {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("https://raw.githubusercontent.com/…/SKILL.md") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addSkillFromUrl(urlInput)
                    showAddDialog = false
                    urlInput = ""
                }) { Text("Download") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
