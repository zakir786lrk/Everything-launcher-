package rikazzz.everythinglauncher.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import rikazzz.everythinglauncher.data.*
import rikazzz.everythinglauncher.data.weather.WeatherConnectionPreferences
import rikazzz.everythinglauncher.ui.LauncherActions
import rikazzz.everythinglauncher.ui.LauncherUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    suspend fun exportBackup(context: Context, uiState: LauncherUiState): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            root.put("version", 1)
            root.put("timestamp", System.currentTimeMillis())
            root.put("timestamp_formatted", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

            // 1. Custom Preferences
            val customPrefs = LauncherCustomPreferences(context)
            root.put("custom_preferences", customPrefs.exportToJson())

            // 2. Launcher Preferences (appearance)
            val prefs = LauncherPreferences(context)
            val appearanceObj = JSONObject().apply {
                put("text_mode", prefs.textMode.name)
                put("themed_icons", prefs.themedIcons)
                val renamesObj = JSONObject()
                prefs.renames().forEach { (k, v) -> renamesObj.put(k, v) }
                put("renames", renamesObj)
                val categoriesObj = JSONObject()
                prefs.categories().forEach { (k, v) -> categoriesObj.put(k, v) }
                put("categories", categoriesObj)
            }
            root.put("appearance", appearanceObj)

            // 3. Settings
            val settings = uiState.settings
            val settingsObj = JSONObject().apply {
                put("clockEnabled", settings.clockEnabled)
                put("calendarAboveClock", settings.calendarAboveClock)
                put("mediaAlwaysVisible", settings.mediaAlwaysVisible)
                put("mediaKeepWhilePlaying", settings.mediaKeepWhilePlaying)
                put("dismissResistance", settings.dismissResistance)
                put("dismissThresholdDp", settings.dismissThresholdDp)
                put("mediaAppKey", settings.mediaAppKey)
                put("calendarAgenda", settings.calendarAgenda)
                put("showBatteryPercentage", settings.showBatteryPercentage)
                put("allowHapticFeedback", settings.allowHapticFeedback)
                put("allowScreenRotation", settings.allowScreenRotation)
                put("useDynamicColors", settings.useDynamicColors)
                put("themeColor", settings.themeColor)
                put("paletteStyle", settings.paletteStyle.name)
                put("darkMode", settings.darkMode.name)
                put("amoledMode", settings.amoledMode)
                put("iconPackPackage", settings.iconPackPackage)
                settings.iconPackPackages.takeIf { it.isNotEmpty() }?.let { put("iconPackPackagesJson", JSONArray(it).toString()) }
                settings.iconDesign?.let { put("iconDesignJson", it.encode()) }
                put("mediaPlayer", settings.mediaPlayer)
                put("weatherEnabled", settings.weatherEnabled)
                put("weatherForecastDays", settings.weatherForecastDays)
                put("weatherLocationId", settings.weatherLocationId)
                put("clockAppKey", settings.clockAppKey)
                put("hideStatusBar", settings.hideStatusBar)
                put("hideAlphabet", settings.hideAlphabet)
                put("hideFavoriteNames", settings.hideFavoriteNames)
                put("dimWallpaper", settings.dimWallpaper)
                put("wallpaperDimAmount", settings.wallpaperDimAmount)
                put("blurWallpaper", settings.blurWallpaper)
                put("wallpaperBlurRadius", settings.wallpaperBlurRadius)
                put("appFontId", settings.appFontId)
                put("fontLibraryJson", settings.fontLibrary.encode())
                put("applyFontToSettings", settings.applyFontToSettings)
                put("preferNonAccessibility", settings.preferNonAccessibility)
                put("mediaStyleJson", settings.mediaStyle.encode())
                put("graceButtonJson", settings.graceButton.encode())
                put("weatherConnectionJson", settings.weatherConnection.encode())
                put("clockStyleJson", settings.clockStyle.encode())
                put("agendaAppearanceJson", settings.agendaAppearance.encode())
                put("homeLayoutJson", settings.homeLayout.encode())
                put("fontColorsJson", settings.fontColors.encode())
                put("listAppearanceJson", settings.listAppearance.encode())
                put("privateSpaceJson", settings.privateSpace.encode())
                put("workProfileJson", settings.workProfile.encode())
                put("homeGesturesJson", settings.homeGestures.encode())
                put("searchJson", settings.search.encode())
                put("shortcutsFoldersJson", settings.shortcutsFolders.encode())
                put("digitalWellbeingJson", settings.digitalWellbeing.encode())
                put("widgetScreenJson", settings.widgetScreen.encode())
                put("calendarPreferencesJson", settings.calendar.encode())
                settings.appCategories?.let { put("appCategoriesJson", AppCategory.encode(it)) }
            }
            root.put("settings", settingsObj)

            // 4. Hidden Apps
            val hiddenArray = JSONArray()
            uiState.hiddenAppKeys.forEach { hiddenArray.put(it) }
            root.put("hidden_apps", hiddenArray)

            // 5. Folders
            val foldersArray = JSONArray()
            uiState.folders.forEach { folder ->
                val fObj = JSONObject().apply {
                    put("id", folder.id)
                    put("name", folder.name)
                    put("placement", folder.placement.name)
                    put("appListAtBottom", folder.appListAtBottom)
                    put("autoHideApps", folder.autoHideApps)
                    put("isAppStack", folder.isAppStack)
                    val apps = JSONArray()
                    folder.appKeys.forEach { apps.put(it) }
                    put("appKeys", apps)
                }
                foldersArray.put(fObj)
            }
            root.put("folders", foldersArray)

            // 6. Home favorites order & favorite keys
            val favs = uiState.favoriteOrder.ifEmpty { uiState.favoriteKeys.toList() }
            val favsArray = JSONArray()
            favs.forEach { favsArray.put(it) }
            root.put("favorite_keys", favsArray)

            val homeItemsArray = JSONArray()
            uiState.homeItems.forEach { homeItemsArray.put(it.key) }
            root.put("home_items", homeItemsArray)

            // 7. Custom Icons & Icon Files
            val db = LauncherDatabase.getInstance(context)
            val itemsDao = db.itemsDao()
            val iconsList = itemsDao.readIcons()
            val customIconsArray = JSONArray()
            iconsList.forEach { iconEntity ->
                customIconsArray.put(JSONObject().apply {
                    put("itemKey", iconEntity.itemKey)
                    put("iconJson", iconEntity.iconJson)
                })
            }
            root.put("custom_icons", customIconsArray)

            val iconFilesDir = File(context.filesDir, "item_icons")
            if (iconFilesDir.exists() && iconFilesDir.isDirectory) {
                val iconFilesObj = JSONObject()
                iconFilesDir.listFiles { f -> f.isFile && f.name.endsWith(".png") }?.forEach { f ->
                    try {
                        val bytes = f.readBytes()
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        iconFilesObj.put(f.name, base64)
                    } catch (_: Exception) {}
                }
                root.put("custom_icon_files", iconFilesObj)
            }

            // 8. App Popups (widgets & shortcuts inside app popups)
            val popupsList = itemsDao.readPopups()
            val popupsArray = JSONArray()
            popupsList.forEach { popup ->
                popupsArray.put(JSONObject().apply {
                    put("ownerKey", popup.ownerKey)
                    put("itemsJson", popup.itemsJson)
                })
            }
            root.put("app_popups", popupsArray)

            val fileName = "EverythingLauncher_Backup_$timestamp.json"
            val downloadDir = File("/sdcard/Download").takeIf { it.exists() && it.canWrite() }
                ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val backupFile = File(downloadDir, fileName)
            backupFile.writeText(root.toString(2))
            backupFile
        }
    }

    suspend fun importBackup(context: Context, uri: Uri, actions: LauncherActions): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().readText()
            } ?: throw IllegalStateException("Could not read backup file")

            val root = JSONObject(content)

            // 1. Custom Preferences
            if (root.has("custom_preferences")) {
                val customPrefs = LauncherCustomPreferences(context)
                customPrefs.importFromJson(root.getJSONObject("custom_preferences"))
            }

            // 2. Launcher Preferences (appearance)
            if (root.has("appearance")) {
                val appObj = root.getJSONObject("appearance")
                val prefs = LauncherPreferences(context)
                if (appObj.has("text_mode")) {
                    val modeName = appObj.getString("text_mode")
                    WallpaperTextMode.entries.firstOrNull { it.name == modeName }?.let { prefs.textMode(it) }
                }
                if (appObj.has("themed_icons")) {
                    val themed = appObj.getBoolean("themed_icons")
                    prefs.themedIcons(themed)
                    actions.themedIcons(themed)
                }
                if (appObj.has("renames")) {
                    val renamesObj = appObj.getJSONObject("renames")
                    renamesObj.keys().forEach { key ->
                        prefs.rename(key, renamesObj.getString(key))
                    }
                }
                if (appObj.has("categories")) {
                    val catsObj = appObj.getJSONObject("categories")
                    catsObj.keys().forEach { key ->
                        prefs.categorize(key, catsObj.getString(key))
                    }
                }
            }

            // 3. Settings
            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                actions.updateSettings { current ->
                    current.copy(
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
                        paletteStyle = sObj.optString("paletteStyle").let { decodePaletteStyle(it) },
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
                        homeLayout = if (sObj.has("homeLayoutJson")) HomeLayout.decode(sObj.getString("homeLayoutJson")) else current.homeLayout,
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
                }
            }

            // 4. Hidden Apps
            if (root.has("hidden_apps")) {
                val array = root.getJSONArray("hidden_apps")
                val hiddenSet = mutableSetOf<String>()
                for (i in 0 until array.length()) hiddenSet.add(array.getString(i))
                actions.setHiddenApps(hiddenSet)
            }

            // 5. Folders
            if (root.has("folders")) {
                val foldersArray = root.getJSONArray("folders")
                for (i in 0 until foldersArray.length()) {
                    val fObj = foldersArray.getJSONObject(i)
                    val appKeys = mutableListOf<String>()
                    val kArr = fObj.optJSONArray("appKeys")
                    if (kArr != null) {
                        for (k in 0 until kArr.length()) appKeys.add(kArr.getString(k))
                    }
                    val folder = LauncherFolder(
                        id = fObj.getString("id"),
                        name = fObj.getString("name"),
                        appKeys = appKeys,
                        placement = FolderPlacement.valueOf(fObj.optString("placement", FolderPlacement.Both.name)),
                        appListAtBottom = fObj.optBoolean("appListAtBottom", true),
                        autoHideApps = fObj.optBoolean("autoHideApps", false),
                        isAppStack = fObj.optBoolean("isAppStack", false)
                    )
                    actions.saveFolder(folder)
                }
            }

            // 6. Home items & favorites order
            val favsKey = if (root.has("favorite_keys")) "favorite_keys" else if (root.has("home_items")) "home_items" else null
            if (favsKey != null) {
                val hArray = root.getJSONArray(favsKey)
                val keys = mutableListOf<String>()
                for (i in 0 until hArray.length()) keys.add(hArray.getString(i))
                actions.reorderFavorites(keys)
            }

            // 7. Custom Icons & Icon Files
            val db = LauncherDatabase.getInstance(context)
            val itemsDao = db.itemsDao()

            if (root.has("custom_icons")) {
                val iconsArray = root.getJSONArray("custom_icons")
                for (i in 0 until iconsArray.length()) {
                    val obj = iconsArray.getJSONObject(i)
                    itemsDao.saveIcon(ItemIconEntity(obj.getString("itemKey"), obj.getString("iconJson")))
                }
            }

            if (root.has("custom_icon_files")) {
                val iconFilesObj = root.getJSONObject("custom_icon_files")
                val iconFilesDir = File(context.filesDir, "item_icons")
                if (!iconFilesDir.exists()) iconFilesDir.mkdirs()
                iconFilesObj.keys().forEach { fileName ->
                    try {
                        val base64 = iconFilesObj.getString(fileName)
                        val bytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
                        File(iconFilesDir, fileName).writeBytes(bytes)
                    } catch (_: Exception) {}
                }
            }

            // 8. App Popups
            if (root.has("app_popups")) {
                val popupsArray = root.getJSONArray("app_popups")
                for (i in 0 until popupsArray.length()) {
                    val obj = popupsArray.getJSONObject(i)
                    itemsDao.savePopup(AppPopupEntity(obj.getString("ownerKey"), obj.getString("itemsJson")))
                }
            }

            root.optString("timestamp_formatted", "Success")
        }
    }

    fun shareBackup(context: Context, file: File) {
        val uri: Uri = try {
            FileProvider.getUriForFile(context, "rikazzz.everythinglauncher.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Everything Launcher Backup").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
