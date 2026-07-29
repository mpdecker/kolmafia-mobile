package net.sourceforge.kolmafia.session

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.inventory.JunkListManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.request.CraftRequest

class QuarkRunnerTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun quark_picksHighestPricePasteableItemAndPostsCraft() = runTest {
        registerItem(QuarkRunner.UNSTABLE_QUARK, "unstable quark", 0)
        registerItem(CHEAP, "cheap paste item", 25)
        registerItem(BEST, "best paste item", 100)
        registerPasteable("cheap paste item")
        registerPasteable("best paste item")

        val craftBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            val body = if (request.method == HttpMethod.Post) {
                request.body.toByteArray().decodeToString()
            } else {
                ""
            }
            craftBodies += body
            respond("<!-- cr:1x0,0=1 -->", HttpStatusCode.OK)
        }

        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(sign = "Mongoose", mp = "0", mpmax = "100"))

        val manager = JunkListManager(GameDatabase())
        manager.loadListsForTest(junkNames = listOf("cheap paste item", "best paste item"))

        val inventory = TestInventoryManager(
            mapOf(
                QuarkRunner.UNSTABLE_QUARK to InventoryItem(
                    QuarkRunner.UNSTABLE_QUARK,
                    "unstable quark",
                    1,
                    ItemType.OTHER,
                ),
                CHEAP to InventoryItem(CHEAP, "cheap paste item", 1, ItemType.OTHER),
                BEST to InventoryItem(BEST, "best paste item", 1, ItemType.OTHER),
            ),
        )

        val messages = mutableListOf<String>()
        val ok = QuarkRunner(
            junkListManager = manager,
            inventoryManager = inventory,
            craftRequest = CraftRequest(HttpClient(engine)),
            retrieveItemService = RetrieveItemService(
                inventoryManager = inventory,
                closetRequest = null,
                storageRequest = null,
                npcBuyRequest = null,
                mallManager = null,
                gameDatabase = GameDatabase(),
            ),
            character = char,
            gameDatabase = GameDatabase(),
        ).quark(print = messages::add)

        assertTrue(ok)
        assertEquals(1, craftBodies.size)
        assertTrue(craftBodies[0].contains("a=${QuarkRunner.UNSTABLE_QUARK}"))
        assertTrue(craftBodies[0].contains("b=$BEST"))
        assertTrue(messages.any { it.contains("best paste item") })
    }

    @Test
    fun quark_skipsSingletonWithQtyOne() = runTest {
        registerItem(QuarkRunner.UNSTABLE_QUARK, "unstable quark", 0)
        registerItem(SINGLETON, "singleton paste item", 50)
        registerPasteable("singleton paste item")

        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(sign = "Mongoose", mp = "0", mpmax = "100"))

        val manager = JunkListManager(GameDatabase())
        manager.loadListsForTest(
            junkNames = listOf("singleton paste item"),
            singletonNames = listOf("singleton paste item"),
        )

        val inventory = TestInventoryManager(
            mapOf(
                QuarkRunner.UNSTABLE_QUARK to InventoryItem(
                    QuarkRunner.UNSTABLE_QUARK,
                    "unstable quark",
                    1,
                    ItemType.OTHER,
                ),
                SINGLETON to InventoryItem(SINGLETON, "singleton paste item", 1, ItemType.OTHER),
            ),
        )

        val messages = mutableListOf<String>()
        val ok = QuarkRunner(
            junkListManager = manager,
            inventoryManager = inventory,
            craftRequest = CraftRequest(HttpClient(MockEngine { respond("ok") })),
            retrieveItemService = RetrieveItemService(
                inventoryManager = inventory,
                closetRequest = null,
                storageRequest = null,
                npcBuyRequest = null,
                mallManager = null,
                gameDatabase = GameDatabase(),
            ),
            character = char,
            gameDatabase = GameDatabase(),
        ).quark(print = messages::add)

        assertFalse(ok)
        assertEquals(listOf("No suitable quark-pasteable items found."), messages)
    }

    private class TestInventoryManager(
        items: Map<Int, InventoryItem>,
    ) : InventoryManager(HttpClient(MockEngine { respond("ok") }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = items))
        override val state = flow.asStateFlow()
    }

    private fun registerItem(id: Int, name: String, autosellPrice: Int) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = autosellPrice,
                plural = null,
            ),
        )
    }

    private fun registerPasteable(name: String) {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "result of $name",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient(name, 1)),
            ),
        )
    }

    companion object {
        private const val CHEAP = 301
        private const val BEST = 302
        private const val SINGLETON = 303
    }
}
