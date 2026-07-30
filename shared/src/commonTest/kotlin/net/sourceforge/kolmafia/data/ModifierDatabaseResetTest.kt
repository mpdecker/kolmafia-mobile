package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModifierDatabaseResetTest {

    private val testItemId = 9_000_003
    private val itemName = "modifier-reset-item"

    @BeforeTest
    fun setUp() {
        ItemDatabase.registerForTest(
            ItemData(
                id = testItemId,
                name = itemName,
                descId = "reset-test",
                image = "test.gif",
                primaryUse = ItemPrimaryUse.ACCESSORY,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 50,
                plural = null,
            ),
        )
        ModifierDatabase.injectForTest("Item", itemName, "Muscle: +5")
    }

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun updateItem_resolvesById() {
        assertTrue(ModifierDatabase.updateItem(testItemId, "Meat Drop: +10"))
        assertTrue(ModifierDatabase.getItem(itemName)?.modifiers?.contains("Meat Drop: +10") == true)
    }

    @Test
    fun resetOverrides_restoresBundledAfterOverride() {
        ModifierDatabase.overrideModifier("Item", itemName, "Meat Drop: +20")
        assertTrue(ModifierDatabase.getItem(itemName)?.modifiers?.contains("Meat Drop: +20") == true)
        ModifierDatabase.resetOverrides()
        assertEquals("Muscle: +5", ModifierDatabase.getItem(itemName)?.modifiers)
    }
}
