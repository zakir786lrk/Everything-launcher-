package rikazzz.everythinglauncher.ai

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class AiWidget(
    val id: String,
    val title: String,
    val content: String,
    val type: String = "card", // card, note, quote, actions, status
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("content", content)
        put("type", type)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject): AiWidget = AiWidget(
            id = json.getString("id"),
            title = json.optString("title", "AI Widget"),
            content = json.optString("content", ""),
            type = json.optString("type", "card"),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }
}

data class AiProgram(
    val id: String,
    val name: String,
    val description: String,
    val commands: List<String>,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("description", description)
        val arr = JSONArray()
        commands.forEach { arr.put(it) }
        put("commands", arr)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(json: JSONObject): AiProgram {
            val cmdArray = json.optJSONArray("commands") ?: JSONArray()
            val list = mutableListOf<String>()
            for (i in 0 until cmdArray.length()) {
                list.add(cmdArray.getString(i))
            }
            return AiProgram(
                id = json.getString("id"),
                name = json.optString("name", "Automation Task"),
                description = json.optString("description", ""),
                commands = list,
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}

class AiPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("everything_ai_prefs", Context.MODE_PRIVATE)

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "").orEmpty()
        set(value) = prefs.edit().putString("gemini_api_key", value).apply()

    var geminiModel: String
        get() = prefs.getString("gemini_model", "gemini-1.5-flash").orEmpty()
        set(value) = prefs.edit().putString("gemini_model", value).apply()

    var customSystemPrompt: String
        get() = prefs.getString("custom_system_prompt", "").orEmpty()
        set(value) = prefs.edit().putString("custom_system_prompt", value).apply()

    var rootEnabled: Boolean
        get() = prefs.getBoolean("root_enabled", true)
        set(value) = prefs.edit().putBoolean("root_enabled", value).apply()

    var showDock: Boolean
        get() = prefs.getBoolean("show_dock", true)
        set(value) = prefs.edit().putBoolean("show_dock", value).apply()

    var dockLabelsEnabled: Boolean
        get() = prefs.getBoolean("dock_labels_enabled", true)
        set(value) = prefs.edit().putBoolean("dock_labels_enabled", value).apply()

    var dockBackgroundStyle: String
        get() = prefs.getString("dock_bg_style", "glass").orEmpty()
        set(value) = prefs.edit().putString("dock_bg_style", value).apply()

    var dockCustomAppKeys: Set<String>
        get() = prefs.getStringSet("dock_custom_app_keys", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("dock_custom_app_keys", value).apply()

    var stackLabelsEnabled: Boolean
        get() = prefs.getBoolean("stack_labels_enabled", true)
        set(value) = prefs.edit().putBoolean("stack_labels_enabled", value).apply()

    var forceRootEnabled: Boolean
        get() = prefs.getBoolean("force_root_enabled", false)
        set(value) = prefs.edit().putBoolean("force_root_enabled", value).apply()

    var aiProvider: String
        get() = prefs.getString("ai_provider", "gemini").orEmpty()
        set(value) = prefs.edit().putString("ai_provider", value).apply()

    var customApiEndpoint: String
        get() = prefs.getString("custom_api_endpoint", "https://openrouter.ai/api/v1").orEmpty()
        set(value) = prefs.edit().putString("custom_api_endpoint", value).apply()

    var customApiKey: String
        get() = prefs.getString("custom_api_key", "").orEmpty()
        set(value) = prefs.edit().putString("custom_api_key", value).apply()

    var customModel: String
        get() = prefs.getString("custom_model", "meta-llama/llama-3.2-3b-instruct:free").orEmpty()
        set(value) = prefs.edit().putString("custom_model", value).apply()

    fun getSavedPrograms(): List<AiProgram> {
        val raw = prefs.getString("saved_ai_programs", null) ?: return defaultPrograms()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<AiProgram>()
            for (i in 0 until arr.length()) {
                list.add(AiProgram.fromJson(arr.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            defaultPrograms()
        }
    }

    fun saveProgram(program: AiProgram) {
        val current = getSavedPrograms().filterNot { it.id == program.id } + program
        val arr = JSONArray()
        current.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("saved_ai_programs", arr.toString()).apply()
    }

    fun deleteProgram(programId: String) {
        val current = getSavedPrograms().filterNot { it.id == programId }
        val arr = JSONArray()
        current.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("saved_ai_programs", arr.toString()).apply()
    }

    fun getWidgets(): List<AiWidget> {
        val raw = prefs.getString("ai_custom_widgets", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<AiWidget>()
            for (i in 0 until arr.length()) {
                list.add(AiWidget.fromJson(arr.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveWidget(widget: AiWidget) {
        val current = getWidgets().filterNot { it.id == widget.id } + widget
        val arr = JSONArray()
        current.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("ai_custom_widgets", arr.toString()).apply()
    }

    fun deleteWidget(widgetId: String) {
        val current = getWidgets().filterNot { it.id == widgetId }
        val arr = JSONArray()
        current.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("ai_custom_widgets", arr.toString()).apply()
    }

    private fun defaultPrograms(): List<AiProgram> = listOf(
        AiProgram(
            id = "preset_sleep",
            name = "Night Mode",
            description = "Turns off Wi-Fi, Bluetooth and silences volume",
            commands = listOf("svc wifi disable", "svc bluetooth disable", "media volume --stream 3 --set 0")
        ),
        AiProgram(
            id = "preset_work",
            name = "Work Focus",
            description = "Turns on Wi-Fi and sets media volume to 20%",
            commands = listOf("svc wifi enable", "media volume --stream 3 --set 5")
        )
    )
}
