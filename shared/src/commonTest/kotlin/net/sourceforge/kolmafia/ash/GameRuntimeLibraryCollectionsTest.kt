package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
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
            private val shiny = ItemData(42, "shiny item", "desc", "item.gif",
                ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null)
            override fun item(id: Int): ItemData? = if (id == 42) shiny else null
            override fun item(name: String): ItemData? =
                if (name.equals("shiny item", ignoreCase = true)) shiny else null
        }
        val p = prefs()
        val lib = GameRuntimeLibrary(closetRequest = fakeCloset, gameDatabase = db, preferences = p)
        assertEquals("1", outputLib(lib, "print(to_string(count(get_closet())));"))
        assertEquals(3, CollectionCache.load(p, Preferences.CACHED_CLOSET)[42])
    }

    @Test
    fun getStorage_returnsLiveItemsFromFetchContents() {
        val fakeStorage = object : StorageRequest(
            HttpClient(MockEngine { respond("") })
        ) {
            override suspend fun fetchContents(): Map<Int, Int> = mapOf(99 to 7)
        }
        val db = object : GameDatabase() {
            private val haggard = ItemData(99, "haggard item", "desc", "hag.gif",
                ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null)
            override fun item(id: Int): ItemData? = if (id == 99) haggard else null
            override fun item(name: String): ItemData? =
                if (name.equals("haggard item", ignoreCase = true)) haggard else null
        }
        val p = prefs()
        val lib = GameRuntimeLibrary(storageRequest = fakeStorage, gameDatabase = db, preferences = p)
        assertEquals("1", outputLib(lib, "print(to_string(count(get_storage())));"))
        assertEquals(7, CollectionCache.load(p, Preferences.CACHED_STORAGE)[99])
    }

    @Test
    fun getCachedCloset_readsPreferenceSnapshot() {
        val p = prefs()
        CollectionCache.save(p, Preferences.CACHED_CLOSET, mapOf(42 to 5))
        val db = object : GameDatabase() {
            private val shiny = ItemData(42, "shiny item", "desc", "item.gif",
                ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null)
            override fun item(id: Int): ItemData? = if (id == 42) shiny else null
            override fun item(name: String): ItemData? =
                if (name.equals("shiny item", ignoreCase = true)) shiny else null
        }
        val lib = GameRuntimeLibrary(preferences = p, gameDatabase = db)
        assertEquals("1", outputLib(lib, "print(to_string(count(get_cached_closet())));"))
        assertEquals("5", outputLib(lib, """print(to_string(closet_amount(to_item("shiny item"))));"""))
    }

    @Test
    fun closetAmount_zeroWhenCacheMissesItem() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("0", outputLib(lib, """print(to_string(closet_amount(to_item("missing"))));"""))
    }

    @Test
    fun storageAmount_readsCachedSnapshot() {
        val p = prefs()
        CollectionCache.save(p, Preferences.CACHED_STORAGE, mapOf(99 to 7))
        val db = object : GameDatabase() {
            private val haggard = ItemData(99, "haggard item", "desc", "hag.gif",
                ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null)
            override fun item(id: Int): ItemData? = if (id == 99) haggard else null
            override fun item(name: String): ItemData? =
                if (name.equals("haggard item", ignoreCase = true)) haggard else null
        }
        val lib = GameRuntimeLibrary(preferences = p, gameDatabase = db)
        assertEquals("7", outputLib(lib, """print(to_string(storage_amount(to_item("haggard item"))));"""))
    }

    @Test
    fun stashAndDisplayAmount_readCachedSnapshots() {
        val p = prefs()
        CollectionCache.save(p, Preferences.CACHED_STASH, mapOf(1 to 2))
        CollectionCache.save(p, Preferences.CACHED_DISPLAY, mapOf(1 to 4))
        val db = object : GameDatabase() {
            private val tiny = ItemData(1, "tiny item", "desc", "tiny.gif",
                ItemPrimaryUse.NONE, emptySet(), setOf('t', 'd'), 0, null)
            override fun item(id: Int): ItemData? = if (id == 1) tiny else null
            override fun item(name: String): ItemData? =
                if (name.equals("tiny item", ignoreCase = true)) tiny else null
        }
        val lib = GameRuntimeLibrary(preferences = p, gameDatabase = db)
        assertEquals("2", outputLib(lib, """print(to_string(stash_amount(to_item("tiny item"))));"""))
        assertEquals("4", outputLib(lib, """print(to_string(display_amount(to_item("tiny item"))));"""))
    }

}
