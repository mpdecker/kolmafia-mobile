package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.StandardRewardDatabase

class GameRuntimeLibraryAshP179Test {

    @Test
    fun revision_phase195() {
        assertEquals("phase247", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun derivePulverization_mapsMossMaceToMossMulch() {
        StandardRewardDatabase.loadFromText(SAMPLE_REWARDS, SAMPLE_PULVERIZED)
        StandardRewardDatabase.derivePulverization()
        assertEquals(MOSS_MULCH, EquipmentDatabase.getPulverization(MOSS_MACE))
    }

    companion object {
        private const val MOSS_MACE = 11504
        private const val MOSS_MULCH = 11510

        private val SAMPLE_REWARDS = """
            11504	2024	norm	SC	ROW1454	moss mace
        """.trimIndent()

        private val SAMPLE_PULVERIZED = """
            11510	2024	norm	moss mulch
        """.trimIndent()
    }
}
