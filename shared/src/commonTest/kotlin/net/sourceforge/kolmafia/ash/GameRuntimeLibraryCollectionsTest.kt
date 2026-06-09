package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.StorageRequest

class GameRuntimeLibraryCollectionsTest {

    @Test
    fun getInventory_emptyWhenNoInventoryManager() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(count(get_inventory())));"))
    }

    @Test
    fun getCloset_returnsEmptyWhenRequestIsNull() {
        // GameRuntimeLibrary.forTesting() sets closetRequest = null; should return empty aggregate
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(count(get_closet())));"))
    }

    @Test
    fun getStorage_returnsEmptyWhenRequestIsNull() {
        // GameRuntimeLibrary.forTesting() sets storageRequest = null; should return empty aggregate
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(), "print(to_string(count(get_storage())));"))
    }

    @Test
    fun getStash_returnsEmptyAggregate() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(),
                "print(to_string(count(get_stash())));"))
    }

    @Test
    fun getDisplay_returnsEmptyAggregate() {
        assertEquals("0",
            outputLib(GameRuntimeLibrary.forTesting(),
                "print(to_string(count(get_display())));"))
    }

    @Test
    fun getCloset_returnsLiveItemsFromFetchContents() {
        // Override fetchContents() to return a known map without hitting the network
        val fakeCloset = object : ClosetRequest(
            HttpClient(MockEngine { respond("") })
        ) {
            override suspend fun fetchContents(): Map<Int, Int> = mapOf(42 to 3)
        }
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = if (id == 42)
                ItemData(42, "shiny item", "desc", "item.gif",
                    ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null)
            else null
        }
        val lib = GameRuntimeLibrary(closetRequest = fakeCloset, gameDatabase = db)
        assertEquals("1", outputLib(lib, "print(to_string(count(get_closet())));"))
    }

    @Test
    fun getStorage_returnsLiveItemsFromFetchContents() {
        val fakeStorage = object : StorageRequest(
            HttpClient(MockEngine { respond("") })
        ) {
            override suspend fun fetchContents(): Map<Int, Int> = mapOf(99 to 7)
        }
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = if (id == 99)
                ItemData(99, "haggard item", "desc", "hag.gif",
                    ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null)
            else null
        }
        val lib = GameRuntimeLibrary(storageRequest = fakeStorage, gameDatabase = db)
        assertEquals("1", outputLib(lib, "print(to_string(count(get_storage())));"))
    }

}
