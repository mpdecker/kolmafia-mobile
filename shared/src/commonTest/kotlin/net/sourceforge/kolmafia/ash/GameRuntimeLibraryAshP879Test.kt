package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.quest.FolderHolderChoiceSync

class GameRuntimeLibraryAshP879Test {
    @Test
    fun folderAddConsumesUrlFolderAndParsesSlots() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        var folders = emptyList<Int>()
        val html = "You carefully place your new folder in the holder. Contents of your Folder Holder otherimages/folders/folder21.gif Folders in your Inventory"
        assertTrue(FolderHolderChoiceSync.apply(774, 1, "choice.php?whichchoice=774&option=1&folder=21", html, { id, qty -> consumed += id to qty }, { folders = it }))
        assertEquals(listOf(FolderHolderChoiceSync.FOLDER_01 + 20 to 1), consumed)
        assertEquals(listOf(FolderHolderChoiceSync.FOLDER_01 + 20), folders)
    }
}
