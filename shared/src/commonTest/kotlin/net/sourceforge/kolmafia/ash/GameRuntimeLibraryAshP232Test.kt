package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
import net.sourceforge.kolmafia.request.ManageStoreRequest
import net.sourceforge.kolmafia.session.AutoMallRunner
import net.sourceforge.kolmafia.session.QuarkRunner

class GameRuntimeLibraryAshP232Test {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
    }

    @Test
    fun revision_phase222() {
        assertEquals("phase230", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun cliAutomall_postsProfitableInventoryToStore() {
        registerItem(PROFITABLE, "seal tooth", 50)

        val requests = mutableListOf<String>()
        val engine = MockEngine { request ->
            val body = if (request.method == HttpMethod.Post) {
                request.body.toByteArray().decodeToString()
            } else {
                ""
            }
            requests += body
            respond("ok", HttpStatusCode.OK)
        }

        val manager = JunkListManager(GameDatabase())
        manager.loadListsForTest(
            junkNames = emptyList(),
            profitableNames = listOf("seal tooth"),
        )

        val inventory = TestInventoryManager(
            mapOf(PROFITABLE to InventoryItem(PROFITABLE, "seal tooth", 3, ItemType.OTHER)),
        )

        val lib = GameRuntimeLibrary(
            character = KoLCharacter(),
            inventoryManager = inventory,
            gameDatabase = GameDatabase(),
            manageStoreRequest = ManageStoreRequest(HttpClient(engine)),
            autoMallRunner = AutoMallRunner(
                junkListManager = manager,
                inventoryManager = inventory,
                manageStoreRequest = ManageStoreRequest(HttpClient(engine)),
                character = KoLCharacter(),
                gameDatabase = GameDatabase(),
            ),
        )

        runLib(lib, """cli_execute("automall");""")
        assertEquals(1, requests.size)
        assertTrue(requests[0].contains("itemid=$PROFITABLE"))
    }

    @Test
    fun cliQuark_craftsWithBestJunkItem() {
        registerItem(QuarkRunner.UNSTABLE_QUARK, "unstable quark", 0)
        registerItem(BEST, "best paste item", 100)
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "quark result",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("best paste item", 1)),
            ),
        )

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
        manager.loadListsForTest(junkNames = listOf("best paste item"))

        val inventory = TestInventoryManager(
            mapOf(
                QuarkRunner.UNSTABLE_QUARK to InventoryItem(
                    QuarkRunner.UNSTABLE_QUARK,
                    "unstable quark",
                    1,
                    ItemType.OTHER,
                ),
                BEST to InventoryItem(BEST, "best paste item", 1, ItemType.OTHER),
            ),
        )

        val lib = GameRuntimeLibrary(
            character = char,
            inventoryManager = inventory,
            gameDatabase = GameDatabase(),
            craftRequest = CraftRequest(HttpClient(engine)),
            quarkRunner = QuarkRunner(
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
            ),
        )

        runLib(lib, """cli_execute("quark");""")
        assertEquals(1, craftBodies.size)
        assertTrue(craftBodies[0].contains("b=$BEST"))
        assertTrue(lib.lastCliOutput.toString().contains("best paste item"))
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

    companion object {
        private const val PROFITABLE = 400
        private const val BEST = 401
    }
}
