package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ItemDatabasePropertyTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun tradeableGiftableDiscardableByAccessFlags() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 1001,
                name = "trade gift discard",
                descId = "d1",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'g', 'd'),
                autosellPrice = 10,
                plural = "trade gift discards",
            ),
        )
        assertTrue(ItemDatabase.isTradeable(1001))
        assertTrue(ItemDatabase.isGiftable(1001))
        assertTrue(ItemDatabase.isDiscardable(1001))
        assertFalse(ItemDatabase.isQuestItem(1001))
        assertTrue(ItemDatabase.isDisplayable(1001))
    }

    @Test
    fun questItemNotDisplayable() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 1002,
                name = "quest item",
                descId = "d2",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('q', 't', 'd'),
                autosellPrice = 0,
                plural = null,
            ),
        )
        assertTrue(ItemDatabase.isQuestItem(1002))
        assertFalse(ItemDatabase.isDisplayable(1002))
    }

    @Test
    fun virtualItemNotDisplayable() {
        assertTrue(ItemDatabase.isVirtualItem(7589))
        assertFalse(ItemDatabase.isDisplayable(7589))
    }

    @Test
    fun pluralNameUsesExplicitPluralOrFallback() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 1003,
                name = "seal tooth",
                descId = "d3",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 5,
                plural = "seal teeth",
            ),
        )
        assertEquals("seal teeth", ItemDatabase.getPluralName(1003))

        ItemDatabase.registerForTest(
            ItemData(
                id = 1004,
                name = "meat paste",
                descId = "d4",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 5,
                plural = null,
            ),
        )
        assertEquals("meat pastes", ItemDatabase.getPluralName(1004))
    }
}
