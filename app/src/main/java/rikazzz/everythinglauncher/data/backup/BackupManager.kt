package rikazzz.everythinglauncher.data.backup

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import java.io.File
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import rikazzz.everythinglauncher.data.*
import rikazzz.everythinglauncher.data.weather.WeatherConnectionPreferences

/**
 * Legacy Everything Launcher v1.0 `.json` backup decoder and importer.
 * Called inside [LauncherBackup.restore] when the selected stream is a JSON backup,
 * sharing the same validation, pre-restore snapshot, rollback, and restart guarantees.
 */
object BackupManager {
    private val KNOWN_SECTIONS = listOf(
        "settings", "folders", "favorite_keys", "home_items", "appearance",
        "custom_preferences", "hidden_apps", "custom_icons", "custom_icon_files", "app_popups",
    )

    internal fun validateLegacyJson(bytes: ByteArray): JSONObject {
        val root = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (e: JSONException) {
            throw LauncherBackupException("The backup file is damaged.")
        }
        if (KNOWN_SECTIONS.none(root::has)) {
            throw LauncherBackupException("Not a launcher backup.")
        }
        if (root.has("custom_icon_files")) {
            val iconFilesObj = root.optJSONObject("custom_icon_files")
                ?: throw LauncherBackupException("The backup file is damaged.")
            for (fileName in iconFilesObj.keys()) {
                if (!isBackupFileEntry("files/item_icons/$fileName")) {
                    throw LauncherBackupException("Unexpected icon file name in backup.")
                }
            }
        }
        return root
    }

    internal suspend fun applyLegacyJson(
        context: Context,
        database: LauncherDatabase,
        filesRoot: File,
        customPreferencesName: String?,
        root: JSONObject,
        afterStep: (String) -> Unit = {},
    ) {
        val decodedIcons = mutableMapOf<String, ByteArray>()
        if (root.has("custom_icon_files")) {
            val iconFilesObj = root.getJSONObject("custom_icon_files")
            for (fileName in iconFilesObj.keys()) {
                val base64 = iconFilesObj.getString(fileName)
                decodedIcons[fileName] = Base64.decode(base64, Base64.NO_WRAP)
            }
        }

        database.withTransaction {
            val settingsDao = database.settingsDao()
            val itemsDao = database.itemsDao()

            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                val current = settingsDao.readSettings()?.toSettings() ?: LauncherSettings()
                val restored = current.copy(
                    clockEnabled = sObj.optBoolean("clockEnabled", current.clockEnabled),
                    calendarAboveClock = sObj.optBoolean("calendarAboveClock", current.calendarAboveClock),
                    mediaAlwaysVisible = sObj.optBoolean("mediaAlwaysVisible", current.mediaAlwaysVisible),
                    mediaKeepWhilePlaying = sObj.optBoolean("mediaKeepWhilePlaying", current.mediaKeepWhilePlaying),
                    dismissResistance = sObj.optInt("dismissResistance", current.dismissResistance),
                    dismissThresholdDp = sObj.optInt("dismissThresholdDp", current.dismissThresholdDp),
                    mediaAppKey = if (sObj.has("mediaAppKey")) sObj.optString("mediaAppKey").takeIf { it.isNotBlank() } else current.mediaAppKey,
                    calendarAgenda = sObj.optBoolean("calendarAgenda", current.calendarAgenda),
                    showBatteryPercentage = sObj.optBoolean("showBatteryPercentage", current.showBatteryPercentage),
                    allowHapticFeedback = sObj.optBoolean("allowHapticFeedback", current.allowHapticFeedback),
                    allowScreenRotation = sObj.optBoolean("allowScreenRotation", current.allowScreenRotation),
                    useDynamicColors = sObj.optBoolean("useDynamicColors", current.useDynamicColors),
                    themeColor = sObj.optInt("themeColor", current.themeColor),
                    paletteStyle = sObj.optString("paletteStyle").takeIf { it.isNotBlank() }?.let(::decodePaletteStyle) ?: current.paletteStyle,
                    darkMode = sObj.optString("darkMode").let { name -> ThemeMode.entries.firstOrNull { it.name == name } ?: current.darkMode },
                    amoledMode = sObj.optBoolean("amoledMode", current.amoledMode),
                    iconPackPackage = if (sObj.has("iconPackPackage")) sObj.optString("iconPackPackage").takeIf { it.isNotBlank() } else current.iconPackPackage,
                    iconPackPackages = if (sObj.has("iconPackPackagesJson")) {
                        val raw = sObj.optString("iconPackPackagesJson")
                        if (raw.isNotBlank()) {
                            val arr = JSONArray(raw)
                            (0 until arr.length()).map { arr.getString(it) }
                        } else current.iconPackPackages
                    } else current.iconPackPackages,
                    iconDesign = if (sObj.has("iconDesignJson")) sObj.optString("iconDesignJson").takeIf { it.isNotBlank() }?.let(ItemIcon::decode) else current.iconDesign,
                    mediaPlayer = sObj.optBoolean("mediaPlayer", current.mediaPlayer),
                    weatherEnabled = sObj.optBoolean("weatherEnabled", current.weatherEnabled),
                    weatherForecastDays = sObj.optInt("weatherForecastDays", current.weatherForecastDays),
                    weatherLocationId = if (sObj.has("weatherLocationId")) sObj.optString("weatherLocationId").takeIf { it.isNotBlank() } else current.weatherLocationId,
                    clockAppKey = if (sObj.has("clockAppKey")) sObj.optString("clockAppKey").takeIf { it.isNotBlank() } else current.clockAppKey,
                    hideStatusBar = sObj.optBoolean("hideStatusBar", current.hideStatusBar),
                    hideAlphabet = sObj.optBoolean("hideAlphabet", current.hideAlphabet),
                    hideFavoriteNames = sObj.optBoolean("hideFavoriteNames", current.hideFavoriteNames),
                    dimWallpaper = sObj.optBoolean("dimWallpaper", current.dimWallpaper),
                    wallpaperDimAmount = sObj.optInt("wallpaperDimAmount", current.wallpaperDimAmount),
                    blurWallpaper = sObj.optBoolean("blurWallpaper", current.blurWallpaper),
                    wallpaperBlurRadius = sObj.optInt("wallpaperBlurRadius", current.wallpaperBlurRadius),
                    appFontId = if (sObj.has("appFontId")) sObj.optString("appFontId").takeIf { it.isNotBlank() } else current.appFontId,
                    fontLibrary = if (sObj.has("fontLibraryJson")) FontLibrary.decode(sObj.getString("fontLibraryJson"), sObj.optString("appFontId")) else current.fontLibrary,
                    applyFontToSettings = sObj.optBoolean("applyFontToSettings", current.applyFontToSettings),
                    preferNonAccessibility = sObj.optBoolean("preferNonAccessibility", current.preferNonAccessibility),
                    mediaStyle = if (sObj.has("mediaStyleJson")) MediaPlayerStyle.decode(sObj.getString("mediaStyleJson")) else current.mediaStyle,
                    graceButton = if (sObj.has("graceButtonJson")) GraceButtonSettings.decode(sObj.getString("graceButtonJson")) else current.graceButton,
                    weatherConnection = if (sObj.has("weatherConnectionJson")) WeatherConnectionPreferences.decode(sObj.getString("weatherConnectionJson")) else current.weatherConnection,
                    clockStyle = if (sObj.has("clockStyleJson")) ClockStyle.decode(sObj.getString("clockStyleJson"), sObj.optBoolean("showBatteryPercentage")) else current.clockStyle,
                    agendaAppearance = if (sObj.has("agendaAppearanceJson")) AgendaAppearance.decode(sObj.getString("agendaAppearanceJson")) else current.agendaAppearance,
                    homeLayout = if (sObj.has("homeLayoutJson")) HomeLayout.decode(scrubBackupJson(sObj.getString("homeLayoutJson"))) else current.homeLayout,
                    fontColors = if (sObj.has("fontColorsJson")) FontColors.decode(sObj.getString("fontColorsJson")) else current.fontColors,
                    listAppearance = if (sObj.has("listAppearanceJson")) ListAppearance.decode(sObj.getString("listAppearanceJson"), sObj.optBoolean("hideFavoriteNames")) else current.listAppearance,
                    privateSpace = if (sObj.has("privateSpaceJson")) ProfileSettings.decode(sObj.getString("privateSpaceJson")) else current.privateSpace,
                    workProfile = if (sObj.has("workProfileJson")) ProfileSettings.decode(sObj.getString("workProfileJson"), ProfileSettings.workDefaults()) else current.workProfile,
                    homeGestures = if (sObj.has("homeGesturesJson")) GraceButtonSettings.decode(sObj.getString("homeGesturesJson"), GraceButtonSettings.homeDefaults()) else current.homeGestures,
                    search = if (sObj.has("searchJson")) SearchSettings.decode(sObj.getString("searchJson")) else current.search,
                    shortcutsFolders = if (sObj.has("shortcutsFoldersJson")) ShortcutsFoldersSettings.decode(sObj.getString("shortcutsFoldersJson")) else current.shortcutsFolders,
                    digitalWellbeing = if (sObj.has("digitalWellbeingJson")) DigitalWellbeingSettings.decode(sObj.getString("digitalWellbeingJson")) else current.digitalWellbeing,
                    widgetScreen = if (sObj.has("widgetScreenJson")) WidgetScreenSettings.decode(sObj.getString("widgetScreenJson")) else current.widgetScreen,
                    calendar = if (sObj.has("calendarPreferencesJson")) CalendarPreferences.decode(sObj.getString("calendarPreferencesJson")) else current.calendar,
                    appCategories = if (sObj.has("appCategoriesJson")) AppCategory.decode(sObj.getString("appCategoriesJson")) else current.appCategories,
                )
                settingsDao.saveSettings(restored.toEntity())
            }

            if (root.has("hidden_apps")) {
                val array = root.getJSONArray("hidden_apps")
                val hidden = (0 until array.length()).map { array.getString(it) }.distinct().map(::HiddenAppEntity)
                settingsDao.replaceHiddenApps(hidden)
            }

            val db = database.openHelper.writableDatabase
            val savedPopupKeys = mutableSetOf<String>()
            if (root.has("app_popups")) {
                db.delete("app_popups", null, null)
                val popupsArray = root.getJSONArray("app_popups")
                for (i in 0 until popupsArray.length()) {
                    val obj = popupsArray.getJSONObject(i)
                    val ownerKey = obj.getString("ownerKey")
                    val scrubbed = scrubBackupJson(obj.getString("itemsJson"))
                    itemsDao.savePopup(AppPopupEntity(ownerKey, scrubbed))
                    savedPopupKeys.add(ownerKey)
                }
            }

            if (root.has("folders")) {
                db.delete("folders", null, null)
                val foldersArray = root.getJSONArray("folders")
                for (i in 0 until foldersArray.length()) {
                    val fObj = foldersArray.getJSONObject(i)
                    val id = fObj.getString("id")
                    val appKeys = mutableListOf<String>()
                    val kArr = fObj.optJSONArray("appKeys")
                    if (kArr != null) {
                        for (k in 0 until kArr.length()) appKeys.add(kArr.getString(k))
                    }
                    settingsDao.upsertFolder(
                        LauncherFolderEntity(
                            id = id,
                            name = fObj.getString("name"),
                            placement = FolderPlacement.decode(fObj.optString("placement", FolderPlacement.Both.name)).name,
                            appListAtBottom = fObj.optBoolean("appListAtBottom", true),
                            autoHideApps = fObj.optBoolean("autoHideApps", false),
                            isAppStack = fObj.optBoolean("isAppStack", false),
                        )
                    )
                    val folderKey = "folder:$id"
                    if (folderKey !in savedPopupKeys && appKeys.isNotEmpty()) {
                        itemsDao.savePopup(AppPopupEntity(folderKey, PopupItem.encode(appKeys.map { PopupItem(it) })))
                    }
                }
            }

            if (root.has("custom_icons")) {
                db.delete("item_icons", null, null)
                val iconsArray = root.getJSONArray("custom_icons")
                for (i in 0 until iconsArray.length()) {
                    val obj = iconsArray.getJSONObject(i)
                    itemsDao.saveIcon(ItemIconEntity(obj.getString("itemKey"), obj.getString("iconJson")))
                }
            }
        }
        afterStep("database")

        if (root.has("custom_icon_files")) {
            val iconFilesDir = File(filesRoot, "item_icons").apply { mkdirs() }
            decodedIcons.forEach { (fileName, bytes) ->
                File(iconFilesDir, fileName).writeBytes(bytes)
            }
        }
        afterStep("files")

        if (root.has("custom_preferences") && customPreferencesName != null) {
            val prefs = context.getSharedPreferences(customPreferencesName, Context.MODE_PRIVATE)
            val setupDone = prefs.getBoolean("first_time_setup_done", true)
            if (!prefs.edit().clear().putBoolean("first_time_setup_done", setupDone).commit()) {
                throw LauncherBackupException("Could not write preferences.")
            }
            val customPrefs = LauncherCustomPreferences(prefs)
            if (!customPrefs.importFromJson(root.getJSONObject("custom_preferences"), commitSync = true)) {
                throw LauncherBackupException("Could not write preferences.")
            }
        }

        if (root.has("appearance")) {
            val appObj = root.getJSONObject("appearance")
            val ok = context.getSharedPreferences(LauncherPreferences.FILE_NAME, Context.MODE_PRIVATE).edit().apply {
                if (appObj.has("text_mode")) {
                    val modeName = appObj.getString("text_mode")
                    WallpaperTextMode.entries.firstOrNull { it.name == modeName }?.let { putString("text_mode", it.name) }
                }
                if (appObj.has("themed_icons")) {
                    putBoolean("themed_icons", appObj.getBoolean("themed_icons"))
                }
                if (appObj.has("renames")) {
                    val renamesObj = appObj.getJSONObject("renames")
                    renamesObj.keys().forEach { key ->
                        val label = renamesObj.getString(key)
                        if (label.isBlank()) remove("label:$key") else putString("label:$key", label.trim())
                    }
                }
                if (appObj.has("categories")) {
                    val catsObj = appObj.getJSONObject("categories")
                    catsObj.keys().forEach { key ->
                        val cat = catsObj.getString(key)
                        if (cat.isBlank()) remove("category:$key") else putString("category:$key", cat.trim())
                    }
                }
            }.commit()
            if (!ok) throw LauncherBackupException("Could not write preferences.")
        }

        val favsKey = if (root.has("favorite_keys")) "favorite_keys" else if (root.has("home_items")) "home_items" else null
        if (favsKey != null) {
            val hArray = root.getJSONArray(favsKey)
            val keys = (0 until hArray.length()).map { hArray.getString(it) }.filter(String::isNotBlank).distinct()
            val ok = context.getSharedPreferences(FavoritesStore.FILE_NAME, Context.MODE_PRIVATE).edit().apply {
                putString("favorite_component_order", JSONArray(keys).toString())
                putStringSet("favorite_components", keys.toSet())
            }.commit()
            if (!ok) throw LauncherBackupException("Could not write preferences.")
        }
        afterStep("preferences")
    }
}
