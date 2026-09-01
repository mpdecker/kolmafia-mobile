package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP125Test {

    private class TestInventoryManager(
        items: Map<Int, InventoryItem>,
    ) : InventoryManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = items))
        override val state = flow.asStateFlow()
    }

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun creatableTurns_threeArgSubtractsFreeSmithing() {
        registerItem(338, "tenderizing hammer")
        registerItem(6965, "warbear auto-anvil")
        registerItem(9520, "smith free target")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "smith free target",
                resultQuantity = 1,
                methods = setOf("SMITH", "HAMMER"),
                ingredients = emptyList(),
            ),
        )
        val inventory = TestInventoryManager(
            mapOf(
                338 to InventoryItem(338, "tenderizing hammer", 1, ItemType.OTHER),
                6965 to InventoryItem(6965, "warbear auto-anvil", 1, ItemType.OTHER),
            ),
        )
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(inventoryManager = inventory, preferences = prefs)
        assertEquals("1", outputLib(lib, """print(creatable_turns(to_item("smith free target")));""").trim())
        assertEquals("0", outputLib(lib, """print(creatable_turns(to_item("smith free target"), 1, 1));""").trim())
    }

    @Test
    fun revision_isphase170() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
