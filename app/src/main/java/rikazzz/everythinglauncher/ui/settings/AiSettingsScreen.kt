package rikazzz.everythinglauncher.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import rikazzz.everythinglauncher.R
import rikazzz.everythinglauncher.ai.AiPreferences
import rikazzz.everythinglauncher.ai.AiProgram
import rikazzz.everythinglauncher.ai.RootExecutor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AiPreferences(context) }

    var apiKey by remember { mutableStateOf(prefs.geminiApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(prefs.geminiModel) }
    var systemPrompt by remember { mutableStateOf(prefs.customSystemPrompt) }
    var rootStatus by remember { mutableStateOf<String?>("Checking...") }
    var savedPrograms by remember { mutableStateOf(prefs.getSavedPrograms()) }
    var aiWidgets by remember { mutableStateOf(prefs.getWidgets()) }

    LaunchedEffect(Unit) {
        val hasRoot = RootExecutor.isRootAvailable()
        rootStatus = if (hasRoot) "Root access granted (su available)" else "Root not detected"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Agent & Automations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ms_arrow_back), contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Gemini AI Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // API Key
            item {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        prefs.geminiApiKey = it
                    },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                painter = painterResource(if (showApiKey) R.drawable.ms_visibility else R.drawable.ms_visibility_off),
                                contentDescription = "Toggle key visibility"
                            )
                        }
                    },
                    supportingText = {
                        Text("Get a free API key at aistudio.google.com")
                    }
                )
            }

            // Model Selection
            item {
                Text("Gemini Model", style = MaterialTheme.typography.labelLarge)
                val models = listOf("gemini-1.5-flash", "gemini-2.0-flash-exp", "gemini-1.5-pro")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    models.forEach { model ->
                        FilterChip(
                            selected = selectedModel == model,
                            onClick = {
                                selectedModel = model
                                prefs.geminiModel = model
                            },
                            label = { Text(model) }
                        )
                    }
                }
            }

            // Root Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Root Access Status", fontWeight = FontWeight.Bold)
                            Text(rootStatus ?: "Unknown", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = {
                            scope.launch {
                                rootStatus = "Checking..."
                                val hasRoot = RootExecutor.isRootAvailable()
                                rootStatus = if (hasRoot) "Root access granted (su available)" else "Root not detected"
                            }
                        }) {
                            Text("Recheck")
                        }
                    }
                }
            }

            // Custom System Prompt
            item {
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = {
                        systemPrompt = it
                        prefs.customSystemPrompt = it
                    },
                    label = { Text("Custom AI Instructions (Optional)") },
                    placeholder = { Text("e.g. Always respond in concise bullet points...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

            // Saved Automations
            item {
                Text(
                    text = "Saved Automations (Tasker-like)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (savedPrograms.isEmpty()) {
                item {
                    Text("No saved automations. Ask the AI to save one (e.g. 'Save a movie mode automation').", style = MaterialTheme.typography.bodySmall)
                }
            }

            items(savedPrograms) { prog ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(prog.name, fontWeight = FontWeight.Bold)
                            Row {
                                IconButton(onClick = {
                                    scope.launch {
                                        Toast.makeText(context, "Running ${prog.name}...", Toast.LENGTH_SHORT).show()
                                        prog.commands.forEach { RootExecutor.execute(it) }
                                        Toast.makeText(context, "${prog.name} completed", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(painterResource(R.drawable.ms_play_arrow), contentDescription = "Run")
                                }
                                IconButton(onClick = {
                                    prefs.deleteProgram(prog.id)
                                    savedPrograms = prefs.getSavedPrograms()
                                }) {
                                    Icon(painterResource(R.drawable.ms_delete), contentDescription = "Delete")
                                }
                            }
                        }
                        if (prog.description.isNotBlank()) {
                            Text(prog.description, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = "Commands: " + prog.commands.joinToString(" ; "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // AI Generated Widgets
            item {
                Text(
                    text = "Custom AI Widgets (${aiWidgets.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (aiWidgets.isEmpty()) {
                item {
                    Text("No custom AI widgets created yet. Ask AI to generate one!", style = MaterialTheme.typography.bodySmall)
                }
            }

            items(aiWidgets) { widget ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(widget.title, fontWeight = FontWeight.Bold)
                            Text(widget.content, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                        IconButton(onClick = {
                            prefs.deleteWidget(widget.id)
                            aiWidgets = prefs.getWidgets()
                        }) {
                            Icon(painterResource(R.drawable.ms_delete), contentDescription = "Delete")
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
