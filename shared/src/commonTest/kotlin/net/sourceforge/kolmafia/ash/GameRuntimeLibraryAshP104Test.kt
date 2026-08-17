package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.ThriftyRequest

class GameRuntimeLibraryAshP104Test {

    @AfterTest
    fun tearDown() {
        ThriftyRequest.resetForTest()
        StandardRequest.resetForTest()
    }

    private class TestInventoryManager(
        items: Map<Int, InventoryItem>,
    ) : InventoryManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = items))
        override val state = flow.asStateFlow()
    }

    private class FakeStorageRequest(
        private val contents: Map<Int, Int>,
    ) : StorageRequest(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })) {
        override suspend fun fetchRawContents(): Map<Int, Int> = contents
    }

    @Test
    fun revision_phase146() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun available_amount_excludesThriftyBlockedStorage() {
        ThriftyRequest.parseResponse(
            """<b>Items</b><p><span class="i">allowed snack,</span><span class="i">other</span><p>""",
        )
        val itemId = 8001
        val item = ItemData(
            id = itemId,
            name = "thrifty-blocked widget",
            descId = "desc8001",
            image = "w.gif",
            primaryUse = ItemPrimaryUse.USABLE,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String): ItemData? =
                if (name.equals("thrifty-blocked widget", ignoreCase = true)) item else null
            override fun item(id: Int): ItemData? = if (id == itemId) item else null
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Thrifty"))
        }
        val inv = TestInventoryManager(
            mapOf(itemId to InventoryItem(itemId, "thrifty-blocked widget", 1, ItemType.OTHER)),
        )
        val lib = GameRuntimeLibrary(
            character = char,
            inventoryManager = inv,
            storageRequest = FakeStorageRequest(mapOf(itemId to 6)),
            gameDatabase = db,
        )
        assertEquals(
            "1",
            outputLib(lib, """print(to_string(available_amount(to_item("thrifty-blocked widget"))));"""),
        )
    }

    @Test
    fun available_amount_restrictedItemIsZero() {
        StandardRequest.parseResponse(
            """<b>Items</b><p><span class="i">forbidden relic,</span><span class="i">other</span><p>""",
        )
        val itemId = 8002
        val item = ItemData(
            id = itemId,
            name = "forbidden relic",
            descId = "desc8002",
            image = "r.gif",
            primaryUse = ItemPrimaryUse.USABLE,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String): ItemData? =
                if (name.equals("forbidden relic", ignoreCase = true)) item else null
            override fun item(id: Int): ItemData? = if (id == itemId) item else null
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(hardcore = "1", roninleft = "0"),
            )
        }
        val inv = TestInventoryManager(
            mapOf(itemId to InventoryItem(itemId, "forbidden relic", 3, ItemType.OTHER)),
        )
        val lib = GameRuntimeLibrary(
            character = char,
            inventoryManager = inv,
            storageRequest = FakeStorageRequest(mapOf(itemId to 9)),
            gameDatabase = db,
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_string(available_amount(to_item("forbidden relic"))));"""),
        )
    }
}
