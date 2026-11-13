package rikazzz.everythinglauncher.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class CustomActivityShortcut(
    val id: String,
    val name: String,
    val packageName: String,
    val activityName: String,
    val showInSearch: Boolean = true,
    val showInDrawer: Boolean = true,
    val assignToQuickTile: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("packageName", packageName)
        put("activityName", activityName)
        put("showInSearch", showInSearch)
        put("showInDrawer", showInDrawer)
        put("assignToQuickTile", assignToQuickTile)
    }

    companion object {
        fun fromJson(json: JSONObject): CustomActivityShortcut = CustomActivityShortcut(
            id = json.optString("id", java.util.UUID.randomUUID().toString()),
            name = json.optString("name", "Custom Activity"),
            packageName = json.optString("packageName", ""),
            activityName = json.optString("activityName", ""),
            showInSearch = json.optBoolean("showInSearch", true),
            showInDrawer = json.optBoolean("showInDrawer", true),
            assignToQuickTile = json.optBoolean("assignToQuickTile", false)
        )
    }
}

class LauncherCustomPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("launcher_custom_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var cachedShortcuts: List<CustomActivityShortcut>? = null
    }

    // Dock & App Stacks
    var fixedDockStackId: String?
        get() = prefs.getString("fixed_dock_stack_id", null)
        set(value) = prefs.edit().putString("fixed_dock_stack_id", value).apply()

    var stackLabelsEnabled: Boolean
        get() = prefs.getBoolean("stack_labels_enabled", true)
        set(value) = prefs.edit().putBoolean("stack_labels_enabled", value).apply()

    // Quick Search features
    var searchCalculatorEnabled: Boolean
        get() = prefs.getBoolean("search_calculator_enabled", true)
        set(value) = prefs.edit().putBoolean("search_calculator_enabled", value).apply()

    var searchDeviceSettingsEnabled: Boolean
        get() = prefs.getBoolean("search_device_settings_enabled", true)
        set(value) = prefs.edit().putBoolean("search_device_settings_enabled", value).apply()

    var searchWebSuggestionsEnabled: Boolean
        get() = prefs.getBoolean("search_web_suggestions_enabled", true)
        set(value) = prefs.edit().putBoolean("search_web_suggestions_enabled", value).apply()

    var searchEngineChipsEnabled: Boolean
        get() = prefs.getBoolean("search_engine_chips_enabled", true)
        set(value) = prefs.edit().putBoolean("search_engine_chips_enabled", value).apply()

    var hiddenEngineChips: Set<String>
        get() = prefs.getStringSet("hidden_engine_chips", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("hidden_engine_chips", value).apply()

    var searchCustomShortcutsEnabled: Boolean
        get() = prefs.getBoolean("search_custom_shortcuts_enabled", true)
        set(value) = prefs.edit().putBoolean("search_custom_shortcuts_enabled", value).apply()

    // Search App Recents & Frequency toggles
    var searchShowRecentlyInstalled: Boolean
        get() = prefs.getBoolean("search_show_recently_installed", true)
        set(value) = prefs.edit().putBoolean("search_show_recently_installed", value).apply()

    var searchShowRecentlyOpened: Boolean
        get() = prefs.getBoolean("search_show_recently_opened", true)
        set(value) = prefs.edit().putBoolean("search_show_recently_opened", value).apply()

    var searchShowFrequentlyOpened: Boolean
        get() = prefs.getBoolean("search_show_frequently_opened", true)
        set(value) = prefs.edit().putBoolean("search_show_frequently_opened", value).apply()

    var defaultSearchEngine: String
        get() = prefs.getString("default_search_engine", "google").orEmpty()
        set(value) = prefs.edit().putString("default_search_engine", value).apply()

    // Custom Activity Shortcuts list (with in-memory cache)
    fun getCustomShortcuts(): List<CustomActivityShortcut> {
        cachedShortcuts?.let { return it }
        val raw = prefs.getString("custom_activity_shortcuts", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<CustomActivityShortcut>()
            for (i in 0 until array.length()) {
                list.add(CustomActivityShortcut.fromJson(array.getJSONObject(i)))
            }
            cachedShortcuts = list
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCustomShortcuts(shortcuts: List<CustomActivityShortcut>) {
        cachedShortcuts = shortcuts
        val array = JSONArray()
        shortcuts.forEach { array.put(it.toJson()) }
        prefs.edit().putString("custom_activity_shortcuts", array.toString()).apply()
    }

    fun addCustomShortcut(shortcut: CustomActivityShortcut) {
        val current = getCustomShortcuts().filterNot { it.id == shortcut.id }.toMutableList()
        current.add(shortcut)
        saveCustomShortcuts(current)
    }

    fun removeCustomShortcut(id: String) {
        val current = getCustomShortcuts().filterNot { it.id == id }
        saveCustomShortcuts(current)
    }

    fun updateCustomShortcut(shortcut: CustomActivityShortcut) {
        val current = getCustomShortcuts().map { if (it.id == shortcut.id) shortcut else it }
        saveCustomShortcuts(current)
    }
}
