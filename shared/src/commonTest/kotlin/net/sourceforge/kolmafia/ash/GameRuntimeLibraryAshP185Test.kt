package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.PulverizeFlags

class GameRuntimeLibraryAshP185Test {

    @Test
    fun revision_phase195() {
        assertEquals("phase220", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun loadPulverizeFromText_wiresUpgradeAndClusterRows() {
        registerItem(HOT_POWDER, "hot powder")
        registerItem(FRYING_BRAINPAN, "frying brainpan")
        EquipmentDatabase.loadPulverizeFromText(
            """
                hot powder	upgrade
                frying brainpan	hot cluster
            """.trimIndent(),
        )

        val upgrade = EquipmentDatabase.getPulverization(HOT_POWDER)
        assertTrue((upgrade and PulverizeFlags.MALUS_UPGRADE) != 0)
        val cluster = EquipmentDatabase.getPulverization(FRYING_BRAINPAN)
        assertTrue((cluster and PulverizeFlags.YIELD_1C) != 0)
        assertTrue((cluster and PulverizeFlags.ELEM_HOT) != 0)
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.WEAPON,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    companion object {
        private const val HOT_POWDER = 1439
        private const val FRYING_BRAINPAN = 6538
    }
}
