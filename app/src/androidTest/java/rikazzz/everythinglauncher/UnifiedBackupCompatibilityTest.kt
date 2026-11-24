package rikazzz.everythinglauncher

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import rikazzz.everythinglauncher.data.FavoritesStore
import rikazzz.everythinglauncher.data.HiddenAppEntity
import rikazzz.everythinglauncher.data.LauncherBackup
import rikazzz.everythinglauncher.data.LauncherCustomPreferences
import rikazzz.everythinglauncher.data.LauncherDatabase
import rikazzz.everythinglauncher.data.LauncherFolderEntity
import rikazzz.everythinglauncher.data.LauncherPreferences
import rikazzz.everythinglauncher.data.LauncherSettingsEntity

@RunWith(AndroidJUnit4::class)
class UnifiedBackupCompatibilityTest {
    @get:Rule val files = TemporaryFolder()
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: LauncherDatabase
    private lateinit var customPrefsName: String

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java).allowMainThreadQueries().build()
        customPrefsName = "custom_test_${UUID.randomUUID()}"
    }

    @After fun tearDown() {
        database.close()
        context.deleteSharedPreferences(customPrefsName)
    }

    private fun defaultBackup(afterStep: (String) -> Unit = {}) = LauncherBackup(
        context = context,
        database = database,
        filesRoot = files.root,
        preferenceNames = listOf(LauncherPreferences.FILE_NAME, FavoritesStore.FILE_NAME),
        afterStep = afterStep,
        customPreferencesName = customPrefsName,
    )

    private fun graceCompatibleBackup() = LauncherBackup(
        context = context,
        database = database,
        filesRoot = files.root,
        preferenceNames = listOf(LauncherPreferences.FILE_NAME, FavoritesStore.FILE_NAME),
        customPreferencesName = null,
    )

    @Test fun everythingExportIsValidGraceZipAndRestoresCustomPrefsInEverything() {
        val customPrefs = LauncherCustomPreferences(context.getSharedPreferences(customPrefsName, Context.MODE_PRIVATE))
        customPrefs.feedScreenEnabled = false
        customPrefs.feedRegion = "GB:en"
        customPrefs.defaultSearchEngine = "duckduckgo"
        runBlocking {
            database.settingsDao().saveSettings(
                LauncherSettingsEntity(
                    calendarAgenda = true, showBatteryPercentage = true, allowHapticFeedback = true,
                    useDynamicColors = true, themeColor = 42, darkMode = "Dark",
                )
            )
        }

        val exported = ByteArrayOutputStream().also { out ->
            runBlocking { defaultBackup().export(out) }
        }.toByteArray()

        // Verify zip structure matches Grace Launcher v1.3.0 strictly
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(exported)).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                entries[e.name] = zip.readBytes()
            }
        }
        assertEquals(setOf("manifest.json", "database.json", "preferences.json"), entries.keys)
        val manifest = JSONObject(String(entries.getValue("manifest.json")))
        assertEquals("grace-launcher-backup", manifest.getString("format"))
        assertEquals(1, manifest.getInt("formatVersion"))
        assertEquals(37, manifest.getInt("databaseVersion"))
        val prefsJson = JSONObject(String(entries.getValue("preferences.json")))
        assertEquals(setOf(LauncherPreferences.FILE_NAME, FavoritesStore.FILE_NAME), prefsJson.keys().asSequence().toSet())

        // Mutate state and restore using Grace-style importer (ignores el_custom: gracefully)
        runBlocking {
            database.settingsDao().saveSettings(
                LauncherSettingsEntity(
                    calendarAgenda = false, showBatteryPercentage = false, allowHapticFeedback = false,
                    useDynamicColors = false, themeColor = 99, darkMode = "Light",
                )
            )
        }
        runBlocking { graceCompatibleBackup().restore(ByteArrayInputStream(exported)) }
        runBlocking { assertEquals(42, database.settingsDao().readSettings()!!.themeColor) }

        // Mutate custom prefs and restore using Everything Launcher importer
        customPrefs.feedScreenEnabled = true
        customPrefs.feedRegion = "US:en"
        customPrefs.defaultSearchEngine = "google"
        runBlocking { defaultBackup().restore(ByteArrayInputStream(exported)) }
        assertFalse(customPrefs.feedScreenEnabled)
        assertEquals("GB:en", customPrefs.feedRegion)
        assertEquals("duckduckgo", customPrefs.defaultSearchEngine)
    }

    @Test fun graceZipRestoresWithoutWipingExistingCustomPrefs() {
        val customPrefs = LauncherCustomPreferences(context.getSharedPreferences(customPrefsName, Context.MODE_PRIVATE))
        customPrefs.feedRegion = "CA:en"
        customPrefs.stackLabelsEnabled = false

        runBlocking {
            database.settingsDao().replaceHiddenApps(listOf(HiddenAppEntity("grace.app/Main")))
        }
        val graceZip = ByteArrayOutputStream().also { out ->
            runBlocking { graceCompatibleBackup().export(out) }
        }.toByteArray()

        runBlocking {
            database.settingsDao().replaceHiddenApps(emptyList())
            defaultBackup().restore(ByteArrayInputStream(graceZip))
            assertEquals(listOf("grace.app/Main"), database.settingsDao().readHiddenApps())
        }
        assertEquals("CA:en", customPrefs.feedRegion)
        assertFalse(customPrefs.stackLabelsEnabled)
    }

    @Test fun legacyJsonRestoresAndRollsBackOnFailure() {
        val customPrefs = LauncherCustomPreferences(context.getSharedPreferences(customPrefsName, Context.MODE_PRIVATE))
        customPrefs.feedRegion = "AU:en"
        runBlocking {
            database.settingsDao().replaceHiddenApps(listOf(HiddenAppEntity("before.app/Main")))
        }

        val legacyJson = JSONObject().apply {
            put("version", 1)
            put("custom_preferences", JSONObject().apply {
                put("feed_region", "JP:ja")
                put("search_calculator_enabled", false)
            })
            put("hidden_apps", JSONArray().put("legacy.hidden/Main"))
            put("folders", JSONArray().put(JSONObject().apply {
                put("id", "f1")
                put("name", "Legacy")
                put("placement", "Both")
                put("appKeys", JSONArray().put("app.one/Main"))
            }))
            put("favorite_keys", JSONArray().put("app.one/Main"))
            put("settings", JSONObject().apply {
                put("themeColor", 777)
                put("amoledMode", true)
            })
        }.toString().toByteArray()

        runBlocking { defaultBackup().restore(ByteArrayInputStream(legacyJson)) }
        runBlocking {
            assertEquals(listOf("legacy.hidden/Main"), database.settingsDao().readHiddenApps())
            assertEquals(777, database.settingsDao().readSettings()!!.themeColor)
            assertTrue(database.settingsDao().readSettings()!!.amoledMode)
            assertEquals(listOf("f1"), database.settingsDao().readFolders().map { it.id })
        }
        assertEquals("JP:ja", customPrefs.feedRegion)
        assertFalse(customPrefs.searchCalculatorEnabled)

        // Simulate failure during legacy JSON restore and verify rollback
        val failing = defaultBackup { step -> if (step == "preferences") error("boom") }
        val mutatingJson = JSONObject().apply {
            put("hidden_apps", JSONArray().put("should.rollback/Main"))
        }.toString().toByteArray()
        var threw = false
        try {
            runBlocking { failing.restore(ByteArrayInputStream(mutatingJson)) }
        } catch (_: IllegalStateException) {
            threw = true
        }
        assertTrue(threw)
        runBlocking {
            assertEquals(listOf("legacy.hidden/Main"), database.settingsDao().readHiddenApps())
        }
        assertFalse(File(files.root, LauncherBackup.SNAPSHOT_FILE_NAME).exists())
    }
}
