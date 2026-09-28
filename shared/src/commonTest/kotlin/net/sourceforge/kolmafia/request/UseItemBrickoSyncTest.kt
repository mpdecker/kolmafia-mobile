package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.session.InventoryActionSync
import kotlin.test.Test
import kotlin.test.assertEquals

class UseItemBrickoSyncTest {

    private fun inventory(): InventoryManager =
        InventoryManager(
            HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            GameEventBus(),
        )

    @Test
    fun breakApart_consumesTheNamedBricko() {
        val inventory = inventory()
        inventory.gainItemLocally(4476, 2)
        UseItemBrickoSync.parse(
            "You break apart your BRICKO ooze.",
            inventory,
            itemIdForName = { name -> if (name == "BRICKO ooze") 4476 else null },
        )
        assertEquals(1, inventory.getCount(4476))
    }

    @Test
    fun otherText_leavesTheBricko() {
        val inventory = inventory()
        inventory.gainItemLocally(4476, 1)
        UseItemBrickoSync.parse(
            "You decide to leave it alone.",
            inventory,
            itemIdForName = { 4476 },
        )
        assertEquals(1, inventory.getCount(4476))
    }

    @Test
    fun unknownName_leavesInventory() {
        val inventory = inventory()
        inventory.gainItemLocally(4476, 1)
        UseItemBrickoSync.parse(
            "You break apart your mystery brick.",
            inventory,
            itemIdForName = { null },
        )
        assertEquals(1, inventory.getCount(4476))
    }

    @Test
    fun inventoryAction_breakbrickoConsumesTheNamedItem() {
        val item = ItemData(
            id = 4476,
            name = "BRICKO ooze",
            descId = "d4476",
            image = "bricko.gif",
            primaryUse = ItemPrimaryUse.NONE,
            secondaryUses = emptySet(),
            access = setOf('t', 'd'),
            autosellPrice = 0,
            plural = null,
        )
        ItemDatabase.registerForTest(item)
        try {
            val inventory = inventory()
            inventory.gainItemLocally(4476, 1)
            val handled = InventoryActionSync.parse(
                "inventory.php?action=breakbricko&whichitem=4476",
                "You break apart your BRICKO ooze.",
                inventory,
            )
            assertEquals(true, handled)
            assertEquals(0, inventory.getCount(4476))
        } finally {
            ItemDatabase.resetForTest()
        }
    }
}
