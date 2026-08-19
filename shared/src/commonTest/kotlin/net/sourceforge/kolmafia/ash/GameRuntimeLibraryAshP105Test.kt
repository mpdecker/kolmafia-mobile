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
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.ThriftyRequest
import net.sourceforge.kolmafia.request.TrendyRequest

class GameRuntimeLibraryAshP105Test {

    @AfterTest
    fun tearDown() {
        ThriftyRequest.resetForTest()
        StandardRequest.resetForTest()
        TrendyRequest.resetForTest()
        ModifierDatabase.resetForTest()
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
    fun revision_phase150() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun available_amount_trendyExpiredItemIsZero() {
        TrendyRequest.parseResponse(
            """
            <tr class="expired">
            <td>2004-12</td><td>Items</td><td>expired hat</td></tr>
            """.trimIndent(),
        )
        val itemId = 8101
        val item = ItemData(
            id = itemId,
            name = "expired hat",
            descId = "desc8101",
            image = "h.gif",
            primaryUse = ItemPrimaryUse.HAT,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String): ItemData? =
                if (name.equals("expired hat", ignoreCase = true)) item else null
            override fun item(id: Int): ItemData? = if (id == itemId) item else null
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Trendy"))
        }
        val inv = TestInventoryManager(
            mapOf(itemId to InventoryItem(itemId, "expired hat", 4, ItemType.OTHER)),
        )
        val lib = GameRuntimeLibrary(
            character = char,
            inventoryManager = inv,
            storageRequest = FakeStorageRequest(mapOf(itemId to 3)),
            gameDatabase = db,
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_string(available_amount(to_item("expired hat"))));"""),
        )
    }

    @Test
    fun available_amount_thriftySeasonalItemIsZero() {
        ModifierDatabase.injectForTest(
            "Item",
            "seasonal trinket",
            """Last Available: "2021-06"""",
        )
        val itemId = 8102
        val item = ItemData(
            id = itemId,
            name = "seasonal trinket",
            descId = "desc8102",
            image = "t.gif",
            primaryUse = ItemPrimaryUse.USABLE,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String): ItemData? =
                if (name.equals("seasonal trinket", ignoreCase = true)) item else null
            override fun item(id: Int): ItemData? = if (id == itemId) item else null
        }
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Thrifty"))
        }
        val inv = TestInventoryManager(
            mapOf(itemId to InventoryItem(itemId, "seasonal trinket", 2, ItemType.OTHER)),
        )
        val lib = GameRuntimeLibrary(
            character = char,
            inventoryManager = inv,
            storageRequest = FakeStorageRequest(mapOf(itemId to 5)),
            gameDatabase = db,
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_string(available_amount(to_item("seasonal trinket"))));"""),
        )
    }
}
