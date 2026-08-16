package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP260Test {

    private class TestInventoryManager(
        items: Map<Int, InventoryItem>,
    ) : InventoryManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = items))
        override val state = flow.asStateFlow()
    }

    @Test
    fun revision_phase245() {
        assertEquals("phase485", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun coinmasterBracket_nicknameShopidTokenAndSells() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("dimemaster", outputLib(lib, """print(to_coinmaster("dimemaster")["nickname"]);""").trim())
        assertEquals("shore", outputLib(lib, """print(to_coinmaster("shore")["shopid"]);""").trim())
        assertEquals(
            "Shore Inc. Ship Trip Scrip",
            outputLib(lib, """print(to_coinmaster("shore")["token"]);""").trim(),
        )
        assertEquals("true", outputLib(lib, """print(to_coinmaster("dimemaster")["sells"]);""").trim())
    }

    @Test
    fun coinmasterBracket_availableTokensFromPrefAndInventory() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setInt("availableDimes", 15)
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "15",
            outputLib(lib, """print(to_coinmaster("dimemaster")["available_tokens"]);""").trim(),
        )

        val scripId = ItemDatabase.getByName("Shore Inc. Ship Trip Scrip")?.id
            ?: error("Shore scrip item not loaded")
        val inv = TestInventoryManager(
            mapOf(scripId to InventoryItem(scripId, "Shore Inc. Ship Trip Scrip", 4, ItemType.OTHER)),
        )
        val libWithInv = GameRuntimeLibrary(gameDatabase = db, inventoryManager = inv)
        assertEquals(
            "4",
            outputLib(libWithInv, """print(to_coinmaster("shore")["available_tokens"]);""").trim(),
        )
    }
}
