package rikazzz.everythinglauncher.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class CustomActivityShortcut(
    val id: String,
    val name: String,
    val packageName: String = "",
    val activityName: String = "",
    val action: String? = null,
    val showInSearch: Boolean = true,
    val showInDrawer: Boolean = true,
    val assignToQuickTile: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("packageName", packageName)
        put("activityName", activityName)
        action?.let { put("action", it) }
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
            action = if (json.has("action") && !json.isNull("action")) json.optString("action").takeIf { it.isNotBlank() } else null,
            showInSearch = json.optBoolean("showInSearch", true),
            showInDrawer = json.optBoolean("showInDrawer", true),
            assignToQuickTile = json.optBoolean("assignToQuickTile", false)
        )

        val defaultPreloadedShortcuts: List<CustomActivityShortcut> = listOf(
            CustomActivityShortcut(
                id = "preloaded_wifi",
                name = "Wi-Fi Settings",
                action = android.provider.Settings.ACTION_WIFI_SETTINGS,
                showInSearch = true,
                showInDrawer = true
            ),
            CustomActivityShortcut(
                id = "preloaded_bluetooth",
                name = "Bluetooth Settings",
                action = android.provider.Settings.ACTION_BLUETOOTH_SETTINGS,
                showInSearch = true,
                showInDrawer = true
            ),
            CustomActivityShortcut(
                id = "preloaded_airplane",
                name = "Airplane Mode",
                action = android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS,
                showInSearch = true,
                showInDrawer = true
            ),
            CustomActivityShortcut(
                id = "preloaded_hotspot",
                name = "Hotspot & Tethering",
                action = android.provider.Settings.ACTION_WIRELESS_SETTINGS,
                showInSearch = true,
                showInDrawer = true
            ),
            CustomActivityShortcut(
                id = "preloaded_battery",
                name = "Battery & Power",
                action = android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS,
                showInSearch = true,
                showInDrawer = true
            ),
            CustomActivityShortcut(
                id = "preloaded_display",
                name = "Display & Brightness",
                action = android.provider.Settings.ACTION_DISPLAY_SETTINGS,
                showInSearch = true,
                showInDrawer = true
            ),
            CustomActivityShortcut(
                id = "preloaded_sound",
                name = "Sound & Vibration",
                action = android.provider.Settings.ACTION_SOUND_SETTINGS,
                showInSearch = true,
                showInDrawer = true
            ),
            CustomActivityShortcut(
                id = "preloaded_developer",
                name = "Developer Options",
                action = android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
                showInSearch = true,
                showInDrawer = true
            ),
            CustomActivityShortcut(
                id = "preloaded_manage_apps",
                name = "Manage All Apps",
                action = android.provider.Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS,
                showInSearch = true,
                showInDrawer = true
            ),
            CustomActivityShortcut(
                id = "preloaded_storage",
                name = "Storage Settings",
                action = android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
                showInSearch = true,
                showInDrawer = true
            ),
            CustomActivityShortcut(
                id = "preloaded_notifications",
                name = "App Notification Settings",
                action = "android.settings.ALL_APPS_NOTIFICATION_SETTINGS",
                showInSearch = true,
                showInDrawer = true
            ),
            CustomActivityShortcut(
                id = "preloaded_datetime",
                name = "Date & Time",
                action = android.provider.Settings.ACTION_DATE_SETTINGS,
                showInSearch = true,
                showInDrawer = true
            )
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

    var dockBackgroundEnabled: Boolean
        get() = prefs.getBoolean("dock_background_enabled", false)
        set(value) = prefs.edit().putBoolean("dock_background_enabled", value).apply()

    // Home Wallpaper Dimming
    var dimHomeScreenWallpaper: Boolean
        get() = prefs.getBoolean("dim_home_screen_wallpaper", true)
        set(value) = prefs.edit().putBoolean("dim_home_screen_wallpaper", value).apply()

    // Home Screen Favorites Overflow & Layout
    var favoritesOverflowMode: String
        get() = prefs.getString("favorites_overflow_mode", "scroll") ?: "scroll"
        set(value) = prefs.edit().putString("favorites_overflow_mode", value).apply()

    var favoritesMaxVisibleCount: Int
        get() = prefs.getInt("favorites_max_visible_count", 6).coerceIn(3, 15)
        set(value) = prefs.edit().putInt("favorites_max_visible_count", value.coerceIn(3, 15)).apply()

    var favoritesOverflowFolderName: String
        get() = prefs.getString("favorites_overflow_folder_name", "More") ?: "More"
        set(value) = prefs.edit().putString("favorites_overflow_folder_name", value).apply()

    // Keyboard Auto-Open settings
    var searchAutoOpenKeyboard: Boolean
        get() = prefs.getBoolean("search_auto_open_keyboard", true)
        set(value) = prefs.edit().putBoolean("search_auto_open_keyboard", value).apply()

    var drawerAutoOpenKeyboard: Boolean
        get() = prefs.getBoolean("drawer_auto_open_keyboard", false)
        set(value) = prefs.edit().putBoolean("drawer_auto_open_keyboard", value).apply()

    // Swipe app right to open popup (turn off to prevent conflict with swipe right for newsfeed)
    var swipeAppRightToOpenPopup: Boolean
        get() = prefs.getBoolean("swipe_app_right_to_open_popup", false)
        set(value) = prefs.edit().putBoolean("swipe_app_right_to_open_popup", value).apply()

    // First time setup
    var firstTimeSetupDone: Boolean
        get() = prefs.getBoolean("first_time_setup_done", false)
        set(value) = prefs.edit().putBoolean("first_time_setup_done", value).apply()

    // Feed Screen
    var feedScreenEnabled: Boolean
        get() = prefs.getBoolean("feed_screen_enabled", true)
        set(value) = prefs.edit().putBoolean("feed_screen_enabled", value).apply()

    var feedDefaultTopic: String
        get() = prefs.getString("feed_default_topic", "top") ?: "top"
        set(value) = prefs.edit().putString("feed_default_topic", value).apply()

    var feedRegion: String
        get() = prefs.getString("feed_region", "US:en") ?: "US:en"
        set(value) = prefs.edit().putString("feed_region", value).apply()

    var feedShowSearchBar: Boolean
        get() = prefs.getBoolean("feed_show_search_bar", true)
        set(value) = prefs.edit().putBoolean("feed_show_search_bar", value).apply()

    var feedShowWeather: Boolean
        get() = prefs.getBoolean("feed_show_weather", true)
        set(value) = prefs.edit().putBoolean("feed_show_weather", value).apply()

    // Icon Theming Fallback
    var useLetterFallbackForUnthemed: Boolean
        get() = prefs.getBoolean("use_letter_fallback_unthemed", false)
        set(value) = prefs.edit().putBoolean("use_letter_fallback_unthemed", value).apply()

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
        val raw = prefs.getString("custom_activity_shortcuts", null)
        if (raw == null) {
            cachedShortcuts = CustomActivityShortcut.defaultPreloadedShortcuts
            return CustomActivityShortcut.defaultPreloadedShortcuts
        }
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<CustomActivityShortcut>()
            for (i in 0 until array.length()) {
                list.add(CustomActivityShortcut.fromJson(array.getJSONObject(i)))
            }
            cachedShortcuts = list
            list
        } catch (e: Exception) {
            CustomActivityShortcut.defaultPreloadedShortcuts
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

    fun exportToJson(): JSONObject = JSONObject().apply {
        put("fixed_dock_stack_id", fixedDockStackId)
        put("stack_labels_enabled", stackLabelsEnabled)
        put("dock_background_enabled", dockBackgroundEnabled)
        put("dim_home_screen_wallpaper", dimHomeScreenWallpaper)
        put("favorites_overflow_mode", favoritesOverflowMode)
        put("favorites_max_visible_count", favoritesMaxVisibleCount)
        put("favorites_overflow_folder_name", favoritesOverflowFolderName)
        put("feed_screen_enabled", feedScreenEnabled)
        put("feed_default_topic", feedDefaultTopic)
        put("feed_region", feedRegion)
        put("feed_show_search_bar", feedShowSearchBar)
        put("feed_show_weather", feedShowWeather)
        put("search_auto_open_keyboard", searchAutoOpenKeyboard)
        put("drawer_auto_open_keyboard", drawerAutoOpenKeyboard)
        put("swipe_app_right_to_open_popup", swipeAppRightToOpenPopup)
        put("use_letter_fallback_unthemed", useLetterFallbackForUnthemed)
        put("search_calculator_enabled", searchCalculatorEnabled)
        put("search_device_settings_enabled", searchDeviceSettingsEnabled)
        put("search_web_suggestions_enabled", searchWebSuggestionsEnabled)
        put("search_engine_chips_enabled", searchEngineChipsEnabled)
        put("hidden_engine_chips", JSONArray(hiddenEngineChips.toList()))
        put("search_custom_shortcuts_enabled", searchCustomShortcutsEnabled)
        put("search_show_recently_installed", searchShowRecentlyInstalled)
        put("search_show_recently_opened", searchShowRecentlyOpened)
        put("search_show_frequently_opened", searchShowFrequentlyOpened)
        put("default_search_engine", defaultSearchEngine)
        val shortcutsArray = JSONArray()
        getCustomShortcuts().forEach { shortcutsArray.put(it.toJson()) }
        put("custom_activity_shortcuts", shortcutsArray)
    }

    fun importFromJson(json: JSONObject) {
        val editor = prefs.edit()
        if (json.has("fixed_dock_stack_id")) editor.putString("fixed_dock_stack_id", json.optString("fixed_dock_stack_id", null))
        if (json.has("stack_labels_enabled")) editor.putBoolean("stack_labels_enabled", json.optBoolean("stack_labels_enabled", true))
        if (json.has("dock_background_enabled")) editor.putBoolean("dock_background_enabled", json.optBoolean("dock_background_enabled", false))
        if (json.has("dim_home_screen_wallpaper")) editor.putBoolean("dim_home_screen_wallpaper", json.optBoolean("dim_home_screen_wallpaper", true))
        if (json.has("favorites_overflow_mode")) editor.putString("favorites_overflow_mode", json.optString("favorites_overflow_mode", "scroll"))
        if (json.has("favorites_max_visible_count")) editor.putInt("favorites_max_visible_count", json.optInt("favorites_max_visible_count", 6))
        if (json.has("favorites_overflow_folder_name")) editor.putString("favorites_overflow_folder_name", json.optString("favorites_overflow_folder_name", "More"))
        if (json.has("feed_screen_enabled")) editor.putBoolean("feed_screen_enabled", json.optBoolean("feed_screen_enabled", true))
        if (json.has("feed_default_topic")) editor.putString("feed_default_topic", json.optString("feed_default_topic", "top"))
        if (json.has("feed_region")) editor.putString("feed_region", json.optString("feed_region", "US:en"))
        if (json.has("feed_show_search_bar")) editor.putBoolean("feed_show_search_bar", json.optBoolean("feed_show_search_bar", true))
        if (json.has("feed_show_weather")) editor.putBoolean("feed_show_weather", json.optBoolean("feed_show_weather", true))
        if (json.has("search_auto_open_keyboard")) editor.putBoolean("search_auto_open_keyboard", json.optBoolean("search_auto_open_keyboard", true))
        if (json.has("drawer_auto_open_keyboard")) editor.putBoolean("drawer_auto_open_keyboard", json.optBoolean("drawer_auto_open_keyboard", false))
        if (json.has("swipe_app_right_to_open_popup")) editor.putBoolean("swipe_app_right_to_open_popup", json.optBoolean("swipe_app_right_to_open_popup", false))
        if (json.has("use_letter_fallback_unthemed")) editor.putBoolean("use_letter_fallback_unthemed", json.optBoolean("use_letter_fallback_unthemed", false))
        if (json.has("search_calculator_enabled")) editor.putBoolean("search_calculator_enabled", json.optBoolean("search_calculator_enabled", true))
        if (json.has("search_device_settings_enabled")) editor.putBoolean("search_device_settings_enabled", json.optBoolean("search_device_settings_enabled", true))
        if (json.has("search_web_suggestions_enabled")) editor.putBoolean("search_web_suggestions_enabled", json.optBoolean("search_web_suggestions_enabled", true))
        if (json.has("search_engine_chips_enabled")) editor.putBoolean("search_engine_chips_enabled", json.optBoolean("search_engine_chips_enabled", true))
        if (json.has("hidden_engine_chips")) {
            val chipsArray = json.optJSONArray("hidden_engine_chips")
            if (chipsArray != null) {
                val set = mutableSetOf<String>()
                for (i in 0 until chipsArray.length()) set.add(chipsArray.getString(i))
                editor.putStringSet("hidden_engine_chips", set)
            }
        }
        if (json.has("search_custom_shortcuts_enabled")) editor.putBoolean("search_custom_shortcuts_enabled", json.optBoolean("search_custom_shortcuts_enabled", true))
        if (json.has("search_show_recently_installed")) editor.putBoolean("search_show_recently_installed", json.optBoolean("search_show_recently_installed", true))
        if (json.has("search_show_recently_opened")) editor.putBoolean("search_show_recently_opened", json.optBoolean("search_show_recently_opened", true))
        if (json.has("search_show_frequently_opened")) editor.putBoolean("search_show_frequently_opened", json.optBoolean("search_show_frequently_opened", true))
        if (json.has("default_search_engine")) editor.putString("default_search_engine", json.optString("default_search_engine", "google"))
        if (json.has("custom_activity_shortcuts")) {
            val raw = json.optJSONArray("custom_activity_shortcuts")?.toString()
            if (raw != null) {
                editor.putString("custom_activity_shortcuts", raw)
                cachedShortcuts = null
            }
        }
        editor.apply()
    }
}
