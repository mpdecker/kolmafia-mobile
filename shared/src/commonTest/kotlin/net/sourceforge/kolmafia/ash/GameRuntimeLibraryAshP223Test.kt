package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.session.WandDiscovery

class GameRuntimeLibraryAshP223Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun getZapWand_returnsPineWandWhenInInventory() {
        registerItem(WandDiscovery.PINE_WAND, "pine wand")
        val inventory = fakeInventoryManager(
            mapOf(WandDiscovery.PINE_WAND to InventoryItem(WandDiscovery.PINE_WAND, "pine wand", 1, ItemType.OTHER)),
        )
        val lib = GameRuntimeLibrary(
            inventoryManager = inventory,
            preferences = prefs(),
        )
        val out = outputLib(lib, "print(to_string(get_zap_wand()));")
        assertEquals("pine wand", out.trim())
    }

    @Test
    fun getZapWand_returnsEmptyWhenNoWand() {
        val lib = GameRuntimeLibrary(
            inventoryManager = fakeInventoryManager(emptyMap()),
            preferences = prefs(),
        )
        val out = outputLib(lib, "print(to_string(get_zap_wand()));")
        assertEquals("", out.trim())
    }

    @Test
    fun revision_isphase222() {
        assertEquals("phase450", GameRuntimeLibrary.REVISION)
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun fakeInventoryManager(items: Map<Int, InventoryItem>): InventoryManager =
        object : InventoryManager(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }), GameEventBus()) {
            private val flow = MutableStateFlow(InventoryState(items = items))
            override val state = flow.asStateFlow()
            override suspend fun fetchInventory() { /* no-op */ }
        }
}
