package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.inventory.JunkListManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PulverizeRequestTest {

    @Test
    fun parseResponse_returnsQuantityOnSuccess() {
        val qty = PulverizeRequest.parseResponse(
            urlString = "craft.php?action=pulverize&smashitem=123&qty=2",
            responseText = "You smash the item.",
        )
        assertEquals(2, qty)
    }

    @Test
    fun parseResponse_returnsZeroOnFailureText() {
        val qty = PulverizeRequest.parseResponse(
            urlString = "craft.php?action=pulverize&smashitem=123&qty=1",
            responseText = "That's not something you can pulverize.",
        )
        assertEquals(0, qty)
    }

    @Test
    fun pulverize_postsCraftPhpAndConsumesInventory() = runBlocking {
        registerWeapon(TEST_ITEM, "test sword")
        EquipmentDatabase.registerForTest(
            TEST_ITEM,
            net.sourceforge.kolmafia.data.EquipmentData("test sword", 120, null, 1, "sword"),
        )

        val calls = mutableListOf<String>()
        val engine = MockEngine { request ->
            val body = (request.body as? TextContent)?.text.orEmpty()
            calls += "${request.url.encodedPath}?$body"
            respond("Smash!", HttpStatusCode.OK)
        }
        val client = HttpClient(engine)
        val inventory = fakeInventoryManager(
            mapOf(
                PulverizeRequest.TENDER_HAMMER to InventoryItem(
                    PulverizeRequest.TENDER_HAMMER,
                    "tenderizing hammer",
                    1,
                    ItemType.WEAPON,
                ),
                TEST_ITEM to InventoryItem(TEST_ITEM, "test sword", 3, ItemType.WEAPON),
            ),
        )
        val request = PulverizeRequest(client, inventory)

        val result = request.pulverize(TEST_ITEM, 2)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(2, result.getOrNull())
        assertEquals(1, inventory.state.value.items[TEST_ITEM]?.quantity)
        assertTrue(calls.isNotEmpty())
        assertTrue(calls.first().contains("craft.php"))
    }

    @Test
    fun pulverize_retrievesHammerAndItemBeforeSmash() = runBlocking {
        registerWeapon(TEST_ITEM, "test sword")
        registerWeapon(PulverizeRequest.TENDER_HAMMER, "tenderizing hammer")
        EquipmentDatabase.registerForTest(
            TEST_ITEM,
            net.sourceforge.kolmafia.data.EquipmentData("test sword", 120, null, 1, "sword"),
        )

        val retrieveCalls = mutableListOf<Pair<Int, Int>>()
        val engine = MockEngine {
            respond("Smash!", HttpStatusCode.OK)
        }
        val client = HttpClient(engine)
        val inventory = fakeInventoryManager(emptyMap())
        val retrieveService = object : RetrieveItemService(null, null, null, null, null, null, null, null, null, null, null) {
            override suspend fun retrieve(itemId: Int, qty: Int): Int {
                retrieveCalls += itemId to qty
                when (itemId) {
                    PulverizeRequest.TENDER_HAMMER -> inventory.putItem(
                        PulverizeRequest.TENDER_HAMMER,
                        "tenderizing hammer",
                        1,
                        ItemType.WEAPON,
                    )
                    TEST_ITEM -> inventory.putItem(TEST_ITEM, "test sword", qty, ItemType.WEAPON)
                }
                return qty
            }
        }
        val request = PulverizeRequest(client, inventory, retrieveService)

        val result = request.pulverize(TEST_ITEM, 2)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(2, result.getOrNull())
        assertEquals(
            listOf(
                PulverizeRequest.TENDER_HAMMER to 1,
                TEST_ITEM to 2,
            ),
            retrieveCalls,
        )
        assertEquals(null, inventory.state.value.items[TEST_ITEM])
    }

    @Test
    fun pulverize_returnsZeroWhenHammerUnavailableAfterRetrieve() = runBlocking {
        registerWeapon(TEST_ITEM, "test sword")
        EquipmentDatabase.registerForTest(
            TEST_ITEM,
            net.sourceforge.kolmafia.data.EquipmentData("test sword", 120, null, 1, "sword"),
        )

        val engine = MockEngine {
            respond("Smash!", HttpStatusCode.OK)
        }
        val request = PulverizeRequest(
            client = HttpClient(engine),
            inventoryManager = fakeInventoryManager(
                mapOf(TEST_ITEM to InventoryItem(TEST_ITEM, "test sword", 2, ItemType.WEAPON)),
            ),
            retrieveItemService = object : RetrieveItemService(null, null, null, null, null, null, null, null, null, null, null) {
                override suspend fun retrieve(itemId: Int, qty: Int): Int = 0
            },
        )

        val result = request.pulverize(TEST_ITEM, 2)
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun pulverize_roninSmashAddsItemToJunkList() = runBlocking {
        registerWeapon(TEST_ITEM, "test sword")
        EquipmentDatabase.registerForTest(
            TEST_ITEM,
            net.sourceforge.kolmafia.data.EquipmentData("test sword", 120, null, 1, "sword"),
        )

        val engine = MockEngine {
            respond("Smash!", HttpStatusCode.OK)
        }
        val client = HttpClient(engine)
        val inventory = fakeInventoryManager(
            mapOf(
                PulverizeRequest.TENDER_HAMMER to InventoryItem(
                    PulverizeRequest.TENDER_HAMMER,
                    "tenderizing hammer",
                    1,
                    ItemType.WEAPON,
                ),
                TEST_ITEM to InventoryItem(TEST_ITEM, "test sword", 2, ItemType.WEAPON),
            ),
        )
        val prefs = Preferences(MapSettings())
        val junkList = JunkListManager(GameDatabase()).also { it.load(prefs) }
        val character = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    name = "Player",
                    classId = CharacterClass.SEAL_CLUBBER.id.toString(),
                    roninleft = "5",
                ),
            )
        }
        val request = PulverizeRequest(
            client = client,
            inventoryManager = inventory,
            character = character,
            junkListManager = junkList,
        )

        val result = request.pulverize(TEST_ITEM, 1)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertTrue(junkList.contains(TEST_ITEM))
        assertTrue(prefs.getString(JunkListManager.PREF_KEY, "").contains("test sword"))
    }

    private fun registerWeapon(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.WEAPON,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    private fun fakeInventoryManager(items: Map<Int, InventoryItem>): TestInventoryManager =
        TestInventoryManager(items)

    private class TestInventoryManager(
        items: Map<Int, InventoryItem>,
    ) : InventoryManager(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = items))
        override val state = flow.asStateFlow()
        override suspend fun fetchInventory() { /* no-op for tests */ }
        override fun consumeItemLocally(itemId: Int, quantity: Int) {
            if (quantity <= 0) return
            val current = flow.value
            val item = current.items[itemId] ?: return
            val remaining = item.quantity - quantity
            val updated = current.items.toMutableMap()
            if (remaining <= 0) {
                updated.remove(itemId)
            } else {
                updated[itemId] = item.copy(quantity = remaining)
            }
            flow.value = current.copy(items = updated)
        }

        fun putItem(itemId: Int, name: String, quantity: Int, type: ItemType) {
            val current = flow.value
            val existing = current.items[itemId]
            val updated = current.items.toMutableMap()
            if (existing == null) {
                updated[itemId] = InventoryItem(itemId, name, quantity, type)
            } else {
                updated[itemId] = existing.copy(quantity = existing.quantity + quantity)
            }
            flow.value = current.copy(items = updated)
        }
    }

    companion object {
        private const val TEST_ITEM = 9001
    }
}
