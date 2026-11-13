package rikazzz.everythinglauncher

import rikazzz.everythinglauncher.data.FolderPlacement
import rikazzz.everythinglauncher.data.LauncherFolder
import rikazzz.everythinglauncher.data.ListAppearance
import rikazzz.everythinglauncher.data.RecentlyInstalledFolderId
import rikazzz.everythinglauncher.ui.drawer.AppListModel
import rikazzz.everythinglauncher.ui.drawer.DrawerItem
import rikazzz.everythinglauncher.ui.drawer.GraceSection
import org.junit.Assert.*
import org.junit.Test

class RecentlyInstalledSettingsTest {
    @Test fun defaultsKeepTheFolderVisibleAndUseItsLocalizedName() {
        val settings = ListAppearance()
        assertTrue(settings.showRecentlyInstalled)
        assertNull(settings.recentlyInstalledName)
        assertEquals("New apps", settings.copy(recentlyInstalledName = "  New apps  ").normalized().recentlyInstalledName)
        assertNull(settings.copy(recentlyInstalledName = "  ").normalized().recentlyInstalledName)
    }

    @Test fun hidingAndShowingPreservesTheNameAndOtherListSettings() {
        val original = ListAppearance(recentlyInstalledName = "New apps", appSpacing = 20, fontSize = 24)
        val hidden = original.copy(showRecentlyInstalled = false).normalized()
        assertEquals("New apps", hidden.recentlyInstalledName)
        assertEquals(original, hidden.copy(showRecentlyInstalled = true))
    }

    @Test fun hidingTheRecentFolderDoesNotRemoveUserFoldersOrLeaveAnEmptySection() {
        val ordinary = LauncherFolder("one", "One", emptyList(), FolderPlacement.AppList)
        val recent = LauncherFolder(RecentlyInstalledFolderId, "New apps", emptyList(), FolderPlacement.AppList)
        val visible = AppListModel(emptyList(), listOf(ordinary), recentlyInstalledFolder = recent)
        assertEquals(listOf(ordinary, recent), visible.items.filterIsInstance<DrawerItem.Folder>().map { it.folder })
        assertTrue(GraceSection in visible.letters)

        val hidden = AppListModel(emptyList(), listOf(ordinary), recentlyInstalledFolder = null)
        assertEquals(listOf(ordinary), hidden.items.filterIsInstance<DrawerItem.Folder>().map { it.folder })
        assertFalse(GraceSection in hidden.letters)
    }
}
