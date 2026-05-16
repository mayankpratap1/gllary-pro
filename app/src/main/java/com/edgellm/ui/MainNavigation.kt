package com.edgellm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.edgellm.MyApplication
import com.edgellm.data.ChatRepository
import com.edgellm.features.agentskills.AgentSkillsScreen
import com.edgellm.features.agentskills.AgentSkillsViewModel
import com.edgellm.features.askimage.AskImageScreen
import com.edgellm.features.askimage.AskImageViewModel
import com.edgellm.features.audioscribe.AudioScribeScreen
import com.edgellm.features.audioscribe.AudioScribeViewModel
import com.edgellm.features.benchmark.BenchmarkScreen
import com.edgellm.features.benchmark.BenchmarkViewModel
import com.edgellm.features.chat.ChatScreen
import com.edgellm.features.chat.ChatViewModel
import com.edgellm.features.promptlab.PromptLabScreen
import com.edgellm.features.promptlab.PromptLabViewModel
import com.edgellm.features.settings.SettingsScreen
import kotlinx.coroutines.launch

data class NavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun MainNavigation() {
    val nav = rememberNavController()
    val service = com.edgellm.LocalEdgeLLMService.current
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val repository = app.repository
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val chatVm: ChatViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(repository) as T
        }
    })

    val items = listOf(
        NavItem("chat", "Chat", Icons.Default.Chat),
        NavItem("askimage", "Vision", Icons.Default.Image),
        NavItem("audioscribe", "Audio", Icons.Default.Mic),
        NavItem("promptlab", "Lab", Icons.Default.Science),
        NavItem("skills", "Skills", Icons.Default.Extension),
        NavItem("benchmark", "Bench", Icons.Default.Speed),
        NavItem("settings", "Settings", Icons.Default.Settings)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ChatHistoryDrawerContent(
                    chatVm = chatVm,
                    onSessionSelected = { 
                        chatVm.selectSession(it)
                        scope.launch { drawerState.close() }
                        nav.navigate("chat") { launchSingleTop = true }
                    },
                    onNewChat = {
                        chatVm.createNewChat()
                        scope.launch { drawerState.close() }
                        nav.navigate("chat") { launchSingleTop = true }
                    }
                )
            }
        }
    ) {
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
            NavHost(nav, startDestination = "chat", modifier = Modifier.padding(padding)) {
                composable("chat") {
                    LaunchedEffect(service?.currentEngine) {
                        chatVm.engineRef = service?.currentEngine
                        chatVm.skillManager = service?.skillManager
                    }
                    ChatScreen(chatVm, onOpenDrawer = { scope.launch { drawerState.open() } })
                }
                composable("askimage") {
                    val vm: AskImageViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return AskImageViewModel(repository) as T
                        }
                    })
                    LaunchedEffect(service?.currentEngine) { vm.engineRef = service?.currentEngine }
                    AskImageScreen(vm)
                }
                composable("audioscribe") {
                    val vm: AudioScribeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return AudioScribeViewModel(repository) as T
                        }
                    })
                    LaunchedEffect(service?.currentEngine) { vm.engineRef = service?.currentEngine }
                    AudioScribeScreen(vm)
                }
                composable("promptlab") {
                    val vm: PromptLabViewModel = viewModel()
                    LaunchedEffect(service?.currentEngine) { vm.engineRef = service?.currentEngine }
                    PromptLabScreen(vm)
                }
                composable("skills") {
                    val vm: AgentSkillsViewModel = viewModel()
                    val skills by (service?.skillManager?.skills?.collectAsState() ?: mutableStateOf(emptyList()))
                    AgentSkillsScreen(vm, skills = skills)
                }
                composable("benchmark") {
                    val vm: BenchmarkViewModel = viewModel()
                    LaunchedEffect(service?.currentEngine) { vm.engineRef = service?.currentEngine }
                    BenchmarkScreen(vm)
                }
                composable("settings") {
                    SettingsScreen(service)
                }
            }
        }
    }
}

@Composable
fun ChatHistoryDrawerContent(
    chatVm: ChatViewModel,
    onSessionSelected: (Long) -> Unit,
    onNewChat: () -> Unit
) {
    val sessions by chatVm.sessions.collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Conversations", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = onNewChat,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("New Chat")
        }
        
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sessions) { session ->
                NavigationDrawerItem(
                    label = { Text(session.title, maxLines = 1) },
                    selected = false,
                    onClick = { onSessionSelected(session.id) },
                    icon = { Icon(Icons.Default.ChatBubbleOutline, null) },
                    badge = {
                        IconButton(onClick = { chatVm.deleteSession(session.id) }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                )
            }
        }
    }
}
