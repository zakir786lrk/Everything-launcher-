package rikazzz.everythinglauncher.quicksearch

import android.content.Context
import android.content.Intent
import android.provider.Settings
import rikazzz.everythinglauncher.R

data class DeviceSettingItem(
    val id: String,
    val title: String,
    val category: String,
    val keywords: List<String>,
    val intentAction: String,
    val iconResId: Int = R.drawable.ms_settings
) {
    fun toIntent(): Intent = Intent(intentAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

class DeviceSettingsRepository(private val context: Context) {
    private val packageManager = context.packageManager

    private val allSettings = listOf(
        DeviceSettingItem(
            id = "wifi",
            title = "Wi-Fi & Internet",
            category = "Connectivity",
            keywords = listOf("wifi", "wi-fi", "internet", "wlan", "network"),
            intentAction = Settings.ACTION_WIFI_SETTINGS,
            iconResId = R.drawable.ms_air
        ),
        DeviceSettingItem(
            id = "bluetooth",
            title = "Bluetooth",
            category = "Connectivity",
            keywords = listOf("bluetooth", "bt", "wireless", "pair", "headphones"),
            intentAction = Settings.ACTION_BLUETOOTH_SETTINGS,
            iconResId = R.drawable.ms_settings
        ),
        DeviceSettingItem(
            id = "display",
            title = "Display & Brightness",
            category = "Device",
            keywords = listOf("display", "brightness", "screen", "dark mode", "font"),
            intentAction = Settings.ACTION_DISPLAY_SETTINGS,
            iconResId = R.drawable.ms_palette
        ),
        DeviceSettingItem(
            id = "sound",
            title = "Sound & Vibration",
            category = "Device",
            keywords = listOf("sound", "volume", "vibration", "ringer", "audio"),
            intentAction = Settings.ACTION_SOUND_SETTINGS,
            iconResId = R.drawable.ms_vibration
        ),
        DeviceSettingItem(
            id = "battery",
            title = "Battery",
            category = "Device",
            keywords = listOf("battery", "power", "charging", "battery saver"),
            intentAction = Settings.ACTION_BATTERY_SAVER_SETTINGS,
            iconResId = R.drawable.ms_battery_full
        ),
        DeviceSettingItem(
            id = "storage",
            title = "Storage",
            category = "System",
            keywords = listOf("storage", "memory", "space", "disk", "cleanup"),
            intentAction = Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            iconResId = R.drawable.ms_settings
        ),
        DeviceSettingItem(
            id = "apps",
            title = "Apps & Permissions",
            category = "Apps",
            keywords = listOf("apps", "applications", "installed apps", "permissions"),
            intentAction = Settings.ACTION_APPLICATION_SETTINGS,
            iconResId = R.drawable.ms_apps
        ),
        DeviceSettingItem(
            id = "developer_options",
            title = "Developer Options",
            category = "System",
            keywords = listOf("developer", "developer options", "usb debugging", "dev"),
            intentAction = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            iconResId = R.drawable.ms_code
        ),
        DeviceSettingItem(
            id = "date_time",
            title = "Date & Time",
            category = "System",
            keywords = listOf("date", "time", "clock", "timezone"),
            intentAction = Settings.ACTION_DATE_SETTINGS,
            iconResId = R.drawable.ms_schedule
        ),
        DeviceSettingItem(
            id = "security",
            title = "Security & Screen Lock",
            category = "Personal",
            keywords = listOf("security", "lock screen", "fingerprint", "face unlock", "password", "pin"),
            intentAction = Settings.ACTION_SECURITY_SETTINGS,
            iconResId = R.drawable.ms_lock
        ),
        DeviceSettingItem(
            id = "location",
            title = "Location",
            category = "Privacy",
            keywords = listOf("location", "gps", "maps", "position"),
            intentAction = Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            iconResId = R.drawable.ms_location_on
        ),
        DeviceSettingItem(
            id = "notifications",
            title = "Notifications & Do Not Disturb",
            category = "System",
            keywords = listOf("notifications", "alerts", "dnd", "do not disturb", "status bar"),
            intentAction = Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS,
            iconResId = R.drawable.ms_notifications
        ),
        DeviceSettingItem(
            id = "accessibility",
            title = "Accessibility",
            category = "System",
            keywords = listOf("accessibility", "talkback", "contrast", "screen reader"),
            intentAction = Settings.ACTION_ACCESSIBILITY_SETTINGS,
            iconResId = R.drawable.ms_touch_app
        ),
        DeviceSettingItem(
            id = "hotspot",
            title = "Hotspot & Tethering",
            category = "Connectivity",
            keywords = listOf("hotspot", "tethering", "portable hotspot", "usb tethering"),
            intentAction = Settings.ACTION_WIRELESS_SETTINGS,
            iconResId = R.drawable.ms_air
        )
    )

    private val availableSettings: List<DeviceSettingItem> by lazy {
        allSettings.filter { canResolve(it) }
    }

    fun searchSettings(query: String): List<DeviceSettingItem> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()

        return availableSettings.filter { setting ->
            val matchesTitle = setting.title.lowercase().contains(q)
            val matchesCategory = setting.category.lowercase().contains(q)
            val matchesKeyword = setting.keywords.any { it.contains(q) || q.contains(it) }

            matchesTitle || matchesCategory || matchesKeyword
        }
    }

    private fun canResolve(setting: DeviceSettingItem): Boolean {
        return try {
            setting.toIntent().resolveActivity(packageManager) != null
        } catch (e: Exception) {
            false
        }
    }
}
