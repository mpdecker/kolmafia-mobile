package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.StorageRequest

class GameRuntimeLibraryAshP101Test {

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
    fun revision_phase144() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun available_amount_includesStorageCount() {
        val itemId = 9001
        val item = ItemData(
            id = itemId,
            name = "stored widget",
            descId = "desc9001",
            image = "w.gif",
            primaryUse = ItemPrimaryUse.USABLE,
            secondaryUses = emptySet(),
            access = emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String): ItemData? =
                if (name.equals("stored widget", ignoreCase = true)) item else null
            override fun item(id: Int): ItemData? = if (id == itemId) item else null
        }
        val inv = TestInventoryManager(
            mapOf(itemId to InventoryItem(itemId, "stored widget", 1, ItemType.OTHER)),
        )
        val lib = GameRuntimeLibrary(
            inventoryManager = inv,
            storageRequest = FakeStorageRequest(mapOf(itemId to 4)),
            gameDatabase = db,
        )
        assertEquals(
            "5",
            outputLib(lib, """print(to_string(available_amount(to_item("stored widget"))));"""),
        )
    }
}
