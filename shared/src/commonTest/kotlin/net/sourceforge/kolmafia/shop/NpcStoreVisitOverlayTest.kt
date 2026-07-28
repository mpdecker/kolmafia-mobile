package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.data.NpcStoreItem

class NpcStoreVisitOverlayTest {

    @AfterTest
    fun cleanup() {
        NpcStoreVisitOverlay.resetForTest()
    }

    @Test
    fun registerMeatRow_lookupAndLine() {
        val added = NpcStoreVisitOverlay.registerMeatRow(
            storeKey = "armory",
            storeName = "Armory and Leggery",
            itemId = 99001,
            itemName = "test meat sword",
            price = 1234,
            rowId = 9999,
        )
        assertTrue(added)

        val entry = NpcStoreVisitOverlay.itemEntry(99001)
        assertNotNull(entry)
        assertEquals("armory", entry.first.storeKey)
        assertEquals(NpcStoreItem("test meat sword", 1234), entry.second)
        assertEquals(
            "Armory and Leggery\tarmory\ttest meat sword\t1234\tROW9999",
            NpcStoreVisitOverlay.toNpcStoreLine(99001),
        )
    }

    @Test
    fun registerMeatRow_idempotent() {
        assertTrue(
            NpcStoreVisitOverlay.registerMeatRow(
                storeKey = "armory",
                storeName = "Armory and Leggery",
                itemId = 99002,
                itemName = "duplicate sword",
                price = 50,
                rowId = 100,
            ),
        )
        assertEquals(
            false,
            NpcStoreVisitOverlay.registerMeatRow(
                storeKey = "armory",
                storeName = "Armory and Leggery",
                itemId = 99002,
                itemName = "duplicate sword",
                price = 50,
                rowId = 100,
            ),
        )
    }

    @Test
    fun npcStoreDatabaseChecksOverlay() {
        NpcStoreVisitOverlay.registerMeatRow(
            storeKey = "armory",
            storeName = "Armory and Leggery",
            itemId = 99003,
            itemName = "overlay blade",
            price = 75,
            rowId = 101,
        )
        assertNotNull(NpcStoreDatabase.itemEntry(99003))
        assertNotNull(NpcStoreDatabase.storeForItem("overlay blade"))
    }
}
