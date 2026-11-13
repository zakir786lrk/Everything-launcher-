package rikazzz.everythinglauncher.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

sealed class AiAgentResult {
    data class Success(val message: String, val actionsExecuted: List<String> = emptyList()) : AiAgentResult()
    data class Error(val error: String) : AiAgentResult()
    data class MissingApiKey(val message: String = "Please set your Gemini API Key in Settings > AI Agent.") : AiAgentResult()
}

class GeminiAgent(private val context: Context) {
    private val prefs = AiPreferences(context)

    suspend fun processQuery(userPrompt: String): AiAgentResult = withContext(Dispatchers.IO) {
        val apiKey = prefs.geminiApiKey.trim()
        if (apiKey.isEmpty()) {
            return@withContext AiAgentResult.MissingApiKey()
        }

        val model = prefs.geminiModel.ifBlank { "gemini-1.5-flash" }
        val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val systemInstruction = """
                You are Everything Launcher's AI Agent & Tasker Assistant.
                You can execute Android system actions, run shell commands as root, generate custom widgets for the launcher, and save automation scripts.
                Always be helpful, precise and concise.
                
                When the user asks to control the system (like turning wifi/bluetooth on/off, changing volume, running commands, creating widgets, or saving programs), invoke the corresponding tool.
                ${prefs.customSystemPrompt}
            """.trimIndent()

            val requestJson = buildRequestJson(userPrompt, systemInstruction)

            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 25000

            OutputStreamWriter(conn.outputStream).use { it.write(requestJson.toString()) }

            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val responseText = BufferedReader(InputStreamReader(stream)).use { it.readText() }

            if (responseCode !in 200..299) {
                return@withContext AiAgentResult.Error("Gemini API Error ($responseCode): $responseText")
            }

            val responseJson = JSONObject(responseText)
            handleGeminiResponse(responseJson)
        } catch (e: Exception) {
            AiAgentResult.Error("Error: ${e.localizedMessage ?: e.message}")
        }
    }

    private suspend fun handleGeminiResponse(responseJson: JSONObject): AiAgentResult {
        val candidates = responseJson.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            return AiAgentResult.Error("No response from AI model.")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts") ?: JSONArray()

        val executedActions = mutableListOf<String>()
        val textResponses = mutableListOf<String>()

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("text")) {
                textResponses.add(part.getString("text"))
            }
            if (part.has("functionCall")) {
                val call = part.getJSONObject("functionCall")
                val name = call.getString("name")
                val args = call.optJSONObject("args") ?: JSONObject()
                val result = executeTool(name, args)
                executedActions.add(result)
            }
        }

        val message = buildString {
            if (executedActions.isNotEmpty()) {
                append("⚡ Actions Executed:\n")
                executedActions.forEach { append("• $it\n") }
                if (textResponses.isNotEmpty()) append("\n")
            }
            if (textResponses.isNotEmpty()) {
                append(textResponses.joinToString("\n"))
            } else if (executedActions.isEmpty()) {
                append("Done.")
            }
        }.trim()

        return AiAgentResult.Success(message, executedActions)
    }

    private suspend fun executeTool(name: String, args: JSONObject): String {
        return when (name) {
            "set_wifi" -> {
                val enable = args.optBoolean("enabled", true)
                val res = RootExecutor.setWifi(enable)
                if (res.isSuccess) "Wi-Fi ${if (enable) "enabled" else "disabled"}" else "Failed to set Wi-Fi: ${res.exceptionOrNull()?.message}"
            }
            "set_bluetooth" -> {
                val enable = args.optBoolean("enabled", true)
                val res = RootExecutor.setBluetooth(enable)
                if (res.isSuccess) "Bluetooth ${if (enable) "enabled" else "disabled"}" else "Failed to set Bluetooth: ${res.exceptionOrNull()?.message}"
            }
            "set_mobile_data" -> {
                val enable = args.optBoolean("enabled", true)
                val res = RootExecutor.setMobileData(enable)
                if (res.isSuccess) "Mobile Data ${if (enable) "enabled" else "disabled"}" else "Failed to set Data: ${res.exceptionOrNull()?.message}"
            }
            "set_volume" -> {
                val level = args.optInt("level", 50)
                val res = RootExecutor.setVolume(3, level)
                if (res.isSuccess) "Media volume set to $level%" else "Failed to set volume"
            }
            "run_shell" -> {
                val command = args.optString("command", "")
                if (command.isBlank()) "Empty shell command"
                else {
                    val res = RootExecutor.execute(command)
                    if (res.isSuccess) "Ran: `$command` -> ${res.getOrNull()}" else "Failed: `${res.exceptionOrNull()?.message}`"
                }
            }
            "create_widget" -> {
                val title = args.optString("title", "AI Widget")
                val content = args.optString("content", "")
                val type = args.optString("type", "card")
                val widget = AiWidget(id = UUID.randomUUID().toString(), title = title, content = content, type = type)
                prefs.saveWidget(widget)
                "Created widget: '$title'"
            }
            "save_automation" -> {
                val progName = args.optString("name", "Task")
                val description = args.optString("description", "")
                val commandsArr = args.optJSONArray("commands") ?: JSONArray()
                val commandsList = mutableListOf<String>()
                for (i in 0 until commandsArr.length()) {
                    commandsList.add(commandsArr.getString(i))
                }
                val prog = AiProgram(id = UUID.randomUUID().toString(), name = progName, description = description, commands = commandsList)
                prefs.saveProgram(prog)
                "Saved automation task: '$progName'"
            }
            else -> "Unknown action: $name"
        }
    }

    private fun buildRequestJson(userPrompt: String, systemInstruction: String): JSONObject {
        val root = JSONObject()

        // System Instruction
        val sysContent = JSONObject()
        val sysParts = JSONArray().apply {
            put(JSONObject().apply { put("text", systemInstruction) })
        }
        sysContent.put("parts", sysParts)
        root.put("system_instruction", sysContent)

        // Contents
        val contents = JSONArray()
        val contentObj = JSONObject()
        val parts = JSONArray().apply {
            put(JSONObject().apply { put("text", userPrompt) })
        }
        contentObj.put("parts", parts)
        contents.put(contentObj)
        root.put("contents", contents)

        // Tools
        val tools = JSONArray()
        val toolObj = JSONObject()
        val funcDecls = JSONArray()

        funcDecls.put(createToolDeclaration("set_wifi", "Enable or disable Wi-Fi on device", listOf("enabled" to "boolean")))
        funcDecls.put(createToolDeclaration("set_bluetooth", "Enable or disable Bluetooth", listOf("enabled" to "boolean")))
        funcDecls.put(createToolDeclaration("set_mobile_data", "Enable or disable Mobile Data", listOf("enabled" to "boolean")))
        funcDecls.put(createToolDeclaration("set_volume", "Set device media volume percentage (0-100)", listOf("level" to "integer")))
        funcDecls.put(createToolDeclaration("run_shell", "Execute a root shell command on the Android device", listOf("command" to "string")))
        funcDecls.put(createToolDeclaration("create_widget", "Generate a new custom widget on the home screen", listOf("title" to "string", "content" to "string", "type" to "string")))
        funcDecls.put(createToolDeclaration("save_automation", "Save a sequence of shell/system commands as an automation program (Tasker-like)", listOf("name" to "string", "description" to "string")))

        toolObj.put("function_declarations", funcDecls)
        tools.put(toolObj)
        root.put("tools", tools)

        return root
    }

    private fun createToolDeclaration(name: String, description: String, properties: List<Pair<String, String>>): JSONObject {
        val fn = JSONObject()
        fn.put("name", name)
        fn.put("description", description)
        val params = JSONObject()
        params.put("type", "OBJECT")
        val props = JSONObject()
        val required = JSONArray()
        properties.forEach { (propName, propType) ->
            val p = JSONObject()
            p.put("type", propType.uppercase())
            props.put(propName, p)
            required.put(propName)
        }
        params.put("properties", props)
        params.put("required", required)
        fn.put("parameters", params)
        return fn
    }
}
