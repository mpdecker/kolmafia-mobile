package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP140Test {

    @Test
    fun revision_phase173() {
        assertEquals("phase460", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun isNpcItem_oneArgReturnsTrueWhenListed() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 9911,
                name = "corpus npc widget",
                descId = "d9911",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        NpcStoreDatabase.loadFromText(
            "General Store\tgeneralstore\tcorpus npc widget\t100\n",
        )
        val lib = GameRuntimeLibrary()
        assertEquals(
            "true",
            outputLib(lib, """print(is_npc_item(9911));""").trim(),
        )
    }
}
