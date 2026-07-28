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
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
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

class GameRuntimeLibraryAshP124Test {

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
    fun getIngredients_femaleRecipe_emptyForMaleCharacter() {
        registerItem(9401, "pink brew")
        registerItem(9402, "pink malt")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "pink brew",
                resultQuantity = 1,
                methods = setOf("COMBINE", "FEMALE"),
                ingredients = listOf(ConcoctionIngredient("pink malt", 1)),
            ),
        )
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    name = "Tester",
                    gender = "male",
                    path = "Standard",
                ),
            )
        }
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("0", outputLib(lib, """print(count(get_ingredients(to_item("pink brew"))));""").trim())
    }

    @Test
    fun creatableTurns_smithRecipeCostsOneTurn() {
        registerItem(338, "tenderizing hammer")
        registerItem(9410, "smith product")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "smith product",
                resultQuantity = 1,
                methods = setOf("SMITH", "HAMMER"),
                ingredients = emptyList(),
            ),
        )
        val inventory = TestInventoryManager(
            mapOf(338 to InventoryItem(338, "tenderizing hammer", 1, ItemType.OTHER)),
        )
        val lib = GameRuntimeLibrary(inventoryManager = inventory)
        assertEquals("1", outputLib(lib, """print(creatable_turns(to_item("smith product")));""").trim())
    }

    @Test
    fun creatableTurns_twoArgUsesCount() {
        registerItem(9411, "combine widget")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "combine widget",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        val lib = GameRuntimeLibrary()
        assertEquals("0", outputLib(lib, """print(creatable_turns(to_item("combine widget"), 3));""").trim())
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
