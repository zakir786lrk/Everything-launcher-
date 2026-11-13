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
    var modelDropdownExpanded by remember { mutableStateOf(false) }

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

    LaunchedEffect(apiKey) {
        if (apiKey.isNotBlank() && detectedModels.isEmpty()) {
            isDetectingModels = true
            val res = agent.fetchAvailableModels(apiKey)
            isDetectingModels = false
            if (res.isSuccess) {
                val models = res.getOrDefault(emptyList())
                if (models.isNotEmpty()) {
                    detectedModels = models
                }
            }
        }
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

                // Gemini Model Dropdown
                item {
                    Text("Gemini Model", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))

                    val fallbackModels = listOf("gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro-latest", "gemini-1.5-flash-latest")
                    val availableModels = if (detectedModels.isNotEmpty()) detectedModels else fallbackModels

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = {
                                selectedModel = it
                                prefs.geminiModel = it
                            },
                            label = { Text(if (detectedModels.isNotEmpty()) "Select Gemini Model (${detectedModels.size} available)" else "Select Gemini Model") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isDetectingModels) {
                                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    IconButton(onClick = {
                                        modelDropdownExpanded = !modelDropdownExpanded
                                        if (modelDropdownExpanded && detectedModels.isEmpty() && apiKey.isNotBlank() && !isDetectingModels) {
                                            scope.launch {
                                                isDetectingModels = true
                                                val res = agent.fetchAvailableModels(apiKey)
                                                isDetectingModels = false
                                                if (res.isSuccess) {
                                                    val list = res.getOrDefault(emptyList())
                                                    if (list.isNotEmpty()) detectedModels = list
                                                }
                                            }
                                        }
                                    }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ms_expand_more),
                                            contentDescription = "Show model dropdown"
                                        )
                                    }
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = modelDropdownExpanded,
                            onDismissRequest = { modelDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 320.dp)
                        ) {
                            if (isDetectingModels) {
                                DropdownMenuItem(
                                    text = { Text("Fetching models from Gemini API...") },
                                    onClick = {},
                                    leadingIcon = { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) }
                                )
                            } else {
                                availableModels.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = model,
                                                fontWeight = if (selectedModel == model) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedModel == model) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            selectedModel = model
                                            prefs.geminiModel = model
                                            modelDropdownExpanded = false
                                        },
                                        leadingIcon = if (selectedModel == model) {
                                            { Icon(painterResource(R.drawable.ms_check), contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (detectedModels.isNotEmpty()) "Loaded ${detectedModels.size} models from Gemini" else "Tap Fetch to query Gemini API",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isDetectingModels = true
                                    val res = agent.fetchAvailableModels(apiKey)
                                    isDetectingModels = false
                                    if (res.isSuccess) {
                                        val models = res.getOrDefault(emptyList())
                                        if (models.isNotEmpty()) {
                                            detectedModels = models
                                            modelDropdownExpanded = true
                                            Toast.makeText(context, "Loaded ${models.size} models from Gemini", Toast.LENGTH_SHORT).show()
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
                            Icon(painterResource(R.drawable.ms_update), contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Fetch from Gemini")
                        }
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
