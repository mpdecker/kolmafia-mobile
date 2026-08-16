package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class GameRuntimeLibraryAshP183Test {

    @Test
    fun revision_phase195() {
        assertEquals("phase490", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun loadPulverizeFromText_wiresEpicWadMapping() {
        registerItem(CHESTERS_SUNGLASSES, "Chester's sunglasses")
        registerItem(EPIC_WAD, "epic wad")
        EquipmentDatabase.loadPulverizeFromText(
            """
                Chester's sunglasses	epic wad
            """.trimIndent(),
        )
        assertEquals(EPIC_WAD, EquipmentDatabase.getPulverization(CHESTERS_SUNGLASSES))
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.ACCESSORY,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    companion object {
        private const val CHESTERS_SUNGLASSES = 3383
        private const val EPIC_WAD = 3316
    }
}
