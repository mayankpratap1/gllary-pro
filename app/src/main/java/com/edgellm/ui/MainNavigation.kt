package com.edgellm.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.edgellm.features.agentskills.AgentSkillsScreen
import com.edgellm.features.askimage.AskImageScreen
import com.edgellm.features.audioscribe.AudioScribeScreen
import com.edgellm.features.benchmark.BenchmarkScreen
import com.edgellm.features.chat.ChatScreen
import com.edgellm.features.promptlab.PromptLabScreen
import com.edgellm.features.settings.SettingsScreen

data class NavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun MainNavigation() {
    val nav = rememberNavController()
    val service = com.edgellm.LocalEdgeLLMService.current
    val items = listOf(
        NavItem("chat", "Chat", Icons.Default.Chat),
        NavItem("askimage", "Vision", Icons.Default.Image),
        NavItem("audioscribe", "Audio", Icons.Default.Mic),
        NavItem("promptlab", "Lab", Icons.Default.Science),
        NavItem("skills", "Skills", Icons.Default.Extension),
        NavItem("benchmark", "Bench", Icons.Default.Speed),
        NavItem("settings", "Settings", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val current = nav.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected = current == item.route,
                        onClick = { nav.navigate(item.route) { launchSingleTop = true } },
                        icon = { Icon(item.icon, null) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "chat", modifier = androidx.compose.ui.Modifier.padding(padding)) {
            composable("chat") { 
                val vm: com.edgellm.features.chat.ChatViewModel = viewModel()
                LaunchedEffect(service?.currentEngine) {
                    vm.engineRef = service?.currentEngine
                    vm.skillManager = service?.skillManager
                    vm.agentSkillsEnabled = true // Enable by default or via settings
                }
                ChatScreen(vm) 
            }
            composable("askimage") { 
                val vm: com.edgellm.features.askimage.AskImageViewModel = viewModel()
                LaunchedEffect(service?.currentEngine) {
                    vm.engineRef = service?.currentEngine
                }
                AskImageScreen(vm) 
            }
            composable("audioscribe") { 
                val vm: com.edgellm.features.audioscribe.AudioScribeViewModel = viewModel()
                LaunchedEffect(service?.currentEngine) {
                    vm.engineRef = service?.currentEngine
                }
                AudioScribeScreen(vm) 
            }
            composable("promptlab") { 
                val vm: com.edgellm.features.promptlab.PromptLabViewModel = viewModel()
                LaunchedEffect(service?.currentEngine) {
                    vm.engineRef = service?.currentEngine
                }
                PromptLabScreen(vm) 
            }
            composable("skills") { 
                val vm: com.edgellm.features.agentskills.AgentSkillsViewModel = viewModel()
                val skills by (service?.skillManager?.skills?.collectAsState() ?: mutableStateOf(emptyList()))
                AgentSkillsScreen(vm, skills = skills) 
            }
            composable("benchmark") { 
                val vm: com.edgellm.features.benchmark.BenchmarkViewModel = viewModel()
                LaunchedEffect(service?.currentEngine) {
                    vm.engineRef = service?.currentEngine
                }
                BenchmarkScreen(vm) 
            }
            composable("settings") { 
                com.edgellm.features.settings.SettingsScreen(service) 
            }
        }
    }
}
