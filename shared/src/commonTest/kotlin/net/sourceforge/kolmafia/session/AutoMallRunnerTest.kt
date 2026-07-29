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
import net.sourceforge.kolmafia.request.ManageStoreRequest

class AutoMallRunnerTest {

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun automall_skipsMementoMeatPasteAndSingletonWhenRonin() = runTest {
        registerItem(PROFITABLE, "seal tooth", 50)
        registerItem(MEMENTO, "tiny plastic test memento", 100)
        registerItem(MEAT_PASTE, "meat paste", 1)
        registerItem(SINGLETON, "bugbear beanie", 30)

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

        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(roninleft = "10"))

        val manager = JunkListManager(GameDatabase())
        manager.loadListsForTest(
            junkNames = emptyList(),
            singletonNames = listOf("bugbear beanie"),
            mementoNames = listOf("tiny plastic test memento"),
            profitableNames = listOf(
                "seal tooth",
                "tiny plastic test memento",
                "meat paste",
                "bugbear beanie",
            ),
        )

        val inventory = TestInventoryManager(
            mapOf(
                PROFITABLE to InventoryItem(PROFITABLE, "seal tooth", 2, ItemType.OTHER),
                MEMENTO to InventoryItem(MEMENTO, "tiny plastic test memento", 1, ItemType.OTHER),
                MEAT_PASTE to InventoryItem(MEAT_PASTE, "meat paste", 5, ItemType.OTHER),
                SINGLETON to InventoryItem(SINGLETON, "bugbear beanie", 2, ItemType.OTHER),
            ),
        )

        AutoMallRunner(
            junkListManager = manager,
            inventoryManager = inventory,
            manageStoreRequest = ManageStoreRequest(HttpClient(engine)),
            character = char,
            gameDatabase = GameDatabase(),
        ).automall()

        assertEquals(1, requests.size)
        assertTrue(requests[0].contains("itemid=$PROFITABLE"))
        assertTrue(requests[0].contains("price=50"))
        assertTrue(requests[0].contains("quantity=2"))
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
        private const val PROFITABLE = 200
        private const val MEMENTO = 201
        private const val MEAT_PASTE = AutoMallRunner.MEAT_PASTE
        private const val SINGLETON = 202
    }
}
