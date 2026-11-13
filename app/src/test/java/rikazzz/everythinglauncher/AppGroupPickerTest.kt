package rikazzz.everythinglauncher

import rikazzz.everythinglauncher.data.FolderPlacement
import rikazzz.everythinglauncher.data.LauncherFolder
import rikazzz.everythinglauncher.data.PopupItem
import rikazzz.everythinglauncher.ui.settings.groupPickerExcludedIds
import org.junit.Assert.*
import org.junit.Test

class AppGroupPickerTest {
    private fun folder(id: String, vararg members: String) = LauncherFolder(id, id, members.toList(), FolderPlacement.None)

    @Test fun excludesSelfAndTransitiveAncestorsButNotOtherFolders() {
        val folders = listOf(folder("self"), folder("parent", "folder:self"), folder("grandparent", "folder:parent"), folder("other"))
        assertEquals(setOf("self", "parent", "grandparent"), groupPickerExcludedIds("folder:self", folders, emptyMap()))
    }

    @Test fun usesEditedPopupMembershipInsteadOfOldFolderDefaults() {
        val folders = listOf(folder("self"), folder("old-parent", "folder:self"), folder("new-parent"))
        val popups = mapOf("folder:old-parent" to emptyList(), "folder:new-parent" to listOf(PopupItem("folder:self")))
        assertEquals(setOf("self", "new-parent"), groupPickerExcludedIds("folder:self", folders, popups))
    }

    @Test fun appAndWidgetEditorsCanPickAnyUserFolder() {
        val folders = listOf(folder("one", "test.app/Main"), folder("two"))
        assertTrue(groupPickerExcludedIds("test.app/Main", folders, emptyMap()).isEmpty())
        assertTrue(groupPickerExcludedIds("rikazzz.everythinglauncher/widget-screen", folders, emptyMap()).isEmpty())
    }
}
