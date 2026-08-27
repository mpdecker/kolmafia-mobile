package net.sourceforge.kolmafia.item

import kotlin.test.Test
import kotlin.test.assertEquals

class RetrieveItemSimulatorTest {

    @Test
    fun simRetrieve_prefersFreepullOverStorage() {
        val ctx = RetrieveItemSimulator.Context(
            inventoryCount = { 0 },
            closetContents = emptyMap(),
            storageContents = mapOf(1 to 5),
            freepullContents = mapOf(1 to 2),
            displayContents = emptyMap(),
            stashContents = emptyMap(),
        )
        assertEquals("free pull", RetrieveItemSimulator.simRetrieve(1, 1, ctx))
    }

    @Test
    fun simRetrieve_removeWhenUseEquipped() {
        val ctx = RetrieveItemSimulator.Context(
            inventoryCount = { 0 },
            closetContents = emptyMap(),
            storageContents = emptyMap(),
            displayContents = emptyMap(),
            stashContents = emptyMap(),
            equippedCount = { 1 },
            useEquipped = true,
        )
        assertEquals("remove", RetrieveItemSimulator.simRetrieve(1, 1, ctx))
    }

    @Test
    fun simRetrieve_inventoryOnlyWhenUseEquippedFalse() {
        val ctx = RetrieveItemSimulator.Context(
            inventoryCount = { 0 },
            closetContents = emptyMap(),
            storageContents = emptyMap(),
            displayContents = emptyMap(),
            stashContents = emptyMap(),
            equippedCount = { 1 },
            useEquipped = false,
        )
        assertEquals("fail", RetrieveItemSimulator.simRetrieve(1, 1, ctx))
    }

    @Test
    fun simRetrieve_createOrBuyWhenCheaper() {
        val ctx = RetrieveItemSimulator.Context(
            inventoryCount = { 0 },
            closetContents = emptyMap(),
            storageContents = emptyMap(),
            displayContents = emptyMap(),
            stashContents = emptyMap(),
            canCreate = { true },
            cheaperToBuy = { _, _ -> true },
        )
        assertEquals("create or buy", RetrieveItemSimulator.simRetrieve(1, 1, ctx))
    }
}
