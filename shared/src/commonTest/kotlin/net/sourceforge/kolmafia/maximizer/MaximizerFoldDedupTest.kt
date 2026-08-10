package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.FoldGroup
import net.sourceforge.kolmafia.data.FoldGroupDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MaximizerFoldDedupTest {

    @BeforeTest
    fun setup() {
        FoldGroupDatabase.resetForTest()
    }

    @AfterTest
    fun teardown() {
        FoldGroupDatabase.resetForTest()
    }

    @Test
    fun availableCount_sameItemDecrementsAcrossExcludedSlots() {
        val assignment = mapOf(
            EquipmentSlot.HAT to ("shared hat" to 1.0),
        )
        val count = MaximizerFoldDedup.availableCount(
            itemName = "shared hat",
            assignment = assignment,
            baseCount = 1,
            foldablesEnabled = false,
            gameDatabase = stubDb(),
            excludeSlotsForSameItem = setOf(EquipmentSlot.HAT, EquipmentSlot.CONTAINER),
        )
        assertEquals(0, count)
    }

    @Test
    fun availableCount_foldGroupDecrementsWhenEnabled() {
        FoldGroupDatabase.registerGroupForTest(
            FoldGroup(hpDamagePct = 5, items = listOf("fold-a", "fold-b")),
        )
        val db = stubDb("fold-a", "fold-b")
        val assignment = mapOf(
            EquipmentSlot.HAT to ("fold-a" to 1.0),
        )
        val withFoldables = MaximizerFoldDedup.availableCount(
            itemName = "fold-b",
            assignment = assignment,
            baseCount = 1,
            foldablesEnabled = true,
            gameDatabase = db,
            excludeSlot = EquipmentSlot.CONTAINER,
        )
        val withoutFoldables = MaximizerFoldDedup.availableCount(
            itemName = "fold-b",
            assignment = assignment,
            baseCount = 1,
            foldablesEnabled = false,
            gameDatabase = db,
            excludeSlot = EquipmentSlot.CONTAINER,
        )
        assertEquals(0, withFoldables)
        assertEquals(1, withoutFoldables)
    }

    @Test
    fun availableCount_excludesCandidateSlotFromFoldDedup() {
        FoldGroupDatabase.registerGroupForTest(
            FoldGroup(hpDamagePct = 5, items = listOf("fold-a", "fold-b")),
        )
        val db = stubDb("fold-a", "fold-b")
        val assignment = mapOf(
            EquipmentSlot.CONTAINER to ("fold-b" to 5.0),
        )
        val count = MaximizerFoldDedup.availableCount(
            itemName = "fold-b",
            assignment = assignment,
            baseCount = 1,
            foldablesEnabled = true,
            gameDatabase = db,
            excludeSlot = EquipmentSlot.CONTAINER,
        )
        assertEquals(1, count)
    }

    private fun stubDb(vararg names: String): GameDatabase = object : GameDatabase() {
        override fun item(name: String): ItemData? =
            names.find { it.equals(name, ignoreCase = true) }?.let {
                ItemData(1, it, "", "", ItemPrimaryUse.CONTAINER, emptySet(), setOf('t'), 0, null)
            }
    }
}
