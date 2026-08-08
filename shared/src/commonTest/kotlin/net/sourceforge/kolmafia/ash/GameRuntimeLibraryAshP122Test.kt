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
import net.sourceforge.kolmafia.data.ConcoctionRefreshContext
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

    @Test
    fun creatableAmount_usesRuntimeAfterRefresh_nestedChild() {
        registerItem(5201, "nested dish")
        registerItem(5202, "nested sauce")
        registerItem(5203, "nested base")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "nested base",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "nested sauce",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("nested base", 1)),
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "nested dish",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(
                    ConcoctionIngredient("nested sauce", 1),
                    ConcoctionIngredient("nested base", 1),
                ),
            ),
        )
        val inventory = TestInventoryManager(
            mapOf(
                5203 to InventoryItem(5203, "nested base", 3, ItemType.OTHER),
            ),
        )
        val lib = GameRuntimeLibrary(inventoryManager = inventory)
        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext.fromAggregatedCounts(mapOf(5203 to 3)),
        )
        assertEquals("1", outputLib(lib, """print(creatable_amount(to_item("nested dish")));""").trim())
    }

    @Test
    fun getIngredients_interchangeableIngredient_returnsSwappedItemId() {
        registerItem(41, "schlitz", autosell = 10)
        registerItem(81, "willer", autosell = 20)
        registerItem(6001, "schlitz cocktail")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "schlitz cocktail",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("schlitz", 1)),
            ),
        )
        val inventory = TestInventoryManager(
            mapOf(
                81 to InventoryItem(81, "willer", 3, ItemType.OTHER),
            ),
        )
        val lib = GameRuntimeLibrary(inventoryManager = inventory)
        assertEquals(
            "true",
            outputLib(
                lib,
                """
                int[item] ing = get_ingredients(to_item("schlitz cocktail"));
                print(ing[to_item("willer")] == 1 && ing[to_item("schlitz")] == 0);
                """.trimIndent(),
            ).trim(),
        )
    }

    private fun registerItem(id: Int, name: String, autosell: Int = 1) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = autosell,
                plural = null,
            ),
        )
    }
}
