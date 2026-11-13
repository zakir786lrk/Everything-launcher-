package rikazzz.everythinglauncher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import rikazzz.everythinglauncher.data.IconColor
import rikazzz.everythinglauncher.ui.theme.LauncherAppearance
import rikazzz.everythinglauncher.ui.theme.wallpaperTint
import org.junit.Assert.*
import org.junit.Test

class WallpaperTintTest {
    private val primary = Color(0xFF6750A4)

    @Test fun defaultTintKeepsContrastForBothTextModes() {
        val behindLightText = LauncherAppearance(darkText = false).wallpaperTint(primary)
        val behindDarkText = LauncherAppearance(darkText = true).wallpaperTint(primary)
        assertTrue(behindLightText.luminance() < .1f)
        assertTrue(behindDarkText.luminance() > .8f)
        assertNotEquals(Color.Black, behindLightText)
    }

    @Test fun explicitColorIsNotRetintedForEitherTextMode() {
        val custom = IconColor(0xFF274B62.toInt())
        listOf(false, true).forEach { dark ->
            assertEquals(custom.argb, LauncherAppearance(darkText = dark).wallpaperTint(primary, custom).toArgb())
        }
    }

    @Test fun themeAndRetiredDynamicChoicesFollowTheThemePalette() {
        listOf(primary, Color(0xFFAA5500)).forEach { seed ->
            val appearance = LauncherAppearance()
            assertEquals(seed, appearance.wallpaperTint(seed, IconColor.Theme))
            assertEquals(seed, appearance.wallpaperTint(seed, IconColor.fromStored(0, dynamic = true)))
        }
    }
}
