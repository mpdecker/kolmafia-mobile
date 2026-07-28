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
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings

class GameRuntimeLibraryAshP122Test {

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
    fun getIngredients_returnsIngredientCounts() {
        registerItem(5001, "crafted soda")
        registerItem(5002, "seltzer")
        registerItem(5003, "sweetener")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "crafted soda",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(
                    ConcoctionIngredient("seltzer", 1),
                    ConcoctionIngredient("sweetener", 1),
                ),
            ),
        )
        val lib = GameRuntimeLibrary()
        val output = outputLib(
            lib,
            """
            int[item] ing = get_ingredients(to_item("crafted soda"));
            print(count(ing));
            """.trimIndent(),
        ).trim()
        assertEquals("2", output)
    }

    @Test
    fun creatableAmount_limitedByInventory() {
        registerItem(5101, "hot wing")
        registerItem(5102, "wing sauce")
        registerItem(5103, "chicken wing")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "hot wing",
                resultQuantity = 1,
                methods = setOf("COOK"),
                ingredients = listOf(
                    ConcoctionIngredient("wing sauce", 1),
                    ConcoctionIngredient("chicken wing", 2),
                ),
            ),
        )
        val inventory = TestInventoryManager(
            mapOf(
                5102 to InventoryItem(5102, "wing sauce", 5, ItemType.OTHER),
                5103 to InventoryItem(5103, "chicken wing", 4, ItemType.OTHER),
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("hasOven", true)
        val lib = GameRuntimeLibrary(inventoryManager = inventory, preferences = prefs)
        assertEquals("2", outputLib(lib, """print(creatable_amount(to_item("hot wing")));""").trim())
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
