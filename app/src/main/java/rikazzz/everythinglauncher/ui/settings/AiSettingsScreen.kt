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
import rikazzz.everythinglauncher.ai.GeminiAgent
import rikazzz.everythinglauncher.ai.RootExecutor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AiPreferences(context) }
    val agent = remember { GeminiAgent(context) }

    var provider by remember { mutableStateOf(prefs.aiProvider) }
    var apiKey by remember { mutableStateOf(prefs.geminiApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(prefs.geminiModel) }
    var detectedModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var isDetectingModels by remember { mutableStateOf(false) }

    // Custom OpenAI / OpenRouter / Local LLM settings
    var customEndpoint by remember { mutableStateOf(prefs.customApiEndpoint) }
    var customKey by remember { mutableStateOf(prefs.customApiKey) }
    var customModelName by remember { mutableStateOf(prefs.customModel) }

    var systemPrompt by remember { mutableStateOf(prefs.customSystemPrompt) }
    var rootStatus by remember { mutableStateOf("Checking...") }
    var forceRoot by remember { mutableStateOf(prefs.forceRootEnabled) }
    var savedPrograms by remember { mutableStateOf(prefs.getSavedPrograms()) }
    var aiWidgets by remember { mutableStateOf(prefs.getWidgets()) }

    fun checkRootNow() {
        scope.launch {
            rootStatus = "Testing root..."
            val hasRoot = RootExecutor.isRootAvailable(forceRecheck = true)
            rootStatus = if (hasRoot) "✅ Root access granted (su active)" else "⚠️ " + RootExecutor.lastCheckMessage
        }
    }

    LaunchedEffect(Unit) {
        checkRootNow()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Agent & Tasker") },
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
            // Provider Selection
            item {
                Text(
                    text = "AI Engine Provider",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = provider == "gemini",
                        onClick = {
                            provider = "gemini"
                            prefs.aiProvider = "gemini"
                        },
                        label = { Text("Google Gemini") }
                    )
                    FilterChip(
                        selected = provider == "custom_openai",
                        onClick = {
                            provider = "custom_openai"
                            prefs.aiProvider = "custom_openai"
                        },
                        label = { Text("OpenRouter / DeepSeek / Local") }
                    )
                }
            }

            if (provider == "gemini") {
                // Gemini API Key
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
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        },
                        supportingText = {
                            Text("Free key from aistudio.google.com")
                        }
                    )
                }

                // Model Selection
                item {
                    Text("Gemini Model", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    val defaultModels = listOf("gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro-latest", "gemini-1.5-flash-latest")
                    val allModels = (defaultModels + detectedModels).distinct()

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            allModels.take(2).forEach { model ->
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            allModels.drop(2).take(3).forEach { model ->
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

                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isDetectingModels = true
                                val res = agent.fetchAvailableModels(apiKey)
                                isDetectingModels = false
                                if (res.isSuccess) {
                                    val models = res.getOrDefault(emptyList())
                                    if (models.isNotEmpty()) {
                                        detectedModels = models
                                        Toast.makeText(context, "Found ${models.size} models from Google", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No generateContent models returned", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = apiKey.isNotBlank() && !isDetectingModels
                    ) {
                        if (isDetectingModels) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Detect Available Models from Google API")
                    }
                }
            } else {
                // Custom Provider (OpenRouter, DeepSeek, Local Ollama)
                item {
                    Text(
                        text = "OpenAI-Compatible / Lightweight LLM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customEndpoint,
                        onValueChange = {
                            customEndpoint = it
                            prefs.customApiEndpoint = it
                        },
                        label = { Text("Base URL Endpoint") },
                        placeholder = { Text("https://openrouter.ai/api/v1") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("e.g. https://api.deepseek.com/v1 or http://localhost:11434/v1 for local Ollama") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customKey,
                        onValueChange = {
                            customKey = it
                            prefs.customApiKey = it
                        },
                        label = { Text("API Key (Optional for local)") },
                        placeholder = { Text("sk-or-...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customModelName,
                        onValueChange = {
                            customModelName = it
                            prefs.customModel = it
                        },
                        label = { Text("Model Name") },
                        placeholder = { Text("meta-llama/llama-3.2-3b-instruct:free or deepseek-chat") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Root Status & Management
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Root Status & Tasker", fontWeight = FontWeight.Bold)
                                Text(rootStatus, style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { checkRootNow() }) {
                                Text("Recheck")
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 12.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Force Root Execution", fontWeight = FontWeight.Medium)
                                Text(
                                    "Bypasses probe check and dispatches all system commands directly via `su`",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = forceRoot,
                                onCheckedChange = {
                                    forceRoot = it
                                    prefs.forceRootEnabled = it
                                }
                            )
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
                    label = { Text("Custom AI System Instructions (Optional)") },
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
                    Text("No saved automations. Ask the AI in search or chat to save one.", style = MaterialTheme.typography.bodySmall)
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
