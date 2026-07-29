package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.WandDiscovery

class ZapRequestTest {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
    }

    @Test
    fun parseResponse_returnsSuccessWithAcquiredItem() {
        val parsed = ZapRequest.parseResponse(
            urlString = "wand.php?action=zap&whichwand=1268&whichitem=9001",
            responseText = "You acquire an item: <b>zap target</b>",
        )
        assertTrue(parsed.success)
        assertEquals(9001, parsed.consumedItemId)
        assertEquals("zap target", parsed.acquiredItemName)
        assertFalse(parsed.wandExploded)
    }

    @Test
    fun parseResponse_returnsFailureOnNothingHappens() {
        val parsed = ZapRequest.parseResponse(
            urlString = "wand.php?action=zap&whichwand=1268&whichitem=9001",
            responseText = "nothing happens",
        )
        assertFalse(parsed.success)
    }

    @Test
    fun parseResponse_detectsWandExplosion() {
        val parsed = ZapRequest.parseResponse(
            urlString = "wand.php?action=zap&whichwand=1268&whichitem=9001",
            responseText = "Your wand abruptly explodes. You acquire an item: <b>zap target</b>",
        )
        assertTrue(parsed.success)
        assertTrue(parsed.wandExploded)
    }

    @Test
    fun zap_postsWandPhpAndUpdatesInventoryAndPrefs() = runBlocking {
        registerItem(WandDiscovery.PINE_WAND, "pine wand")
        registerItem(SOURCE_ITEM, "zap source")
        registerItem(TARGET_ITEM, "zap target")

        val calls = mutableListOf<String>()
        val engine = MockEngine { request ->
            val body = (request.body as? TextContent)?.text.orEmpty()
            calls += "${request.url.encodedPath}?$body"
            respond("You acquire an item: <b>zap target</b>", HttpStatusCode.OK)
        }
        val settings = MapSettings()
        val preferences = Preferences(settings)
        val inventory = fakeInventoryManager(
            mapOf(
                WandDiscovery.PINE_WAND to InventoryItem(WandDiscovery.PINE_WAND, "pine wand", 1, ItemType.OTHER),
                SOURCE_ITEM to InventoryItem(SOURCE_ITEM, "zap source", 1, ItemType.OTHER),
            ),
        )
        val character = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(daycount = "42"))
        }
        val request = ZapRequest(
            client = HttpClient(engine),
            inventoryManager = inventory,
            preferences = preferences,
            character = character,
        )

        val result = request.zap(SOURCE_ITEM)
        assertTrue(result.isSuccess)
        assertEquals(TARGET_ITEM, result.getOrNull())
        assertEquals(null, inventory.state.value.items[SOURCE_ITEM])
        assertEquals(1, preferences.getInt("_zapCount", 0))
        assertTrue(calls.any { it.contains("wand.php") })
    }

    @Test
    fun zap_explosionConsumesWandAndResetsZapCount() = runBlocking {
        registerItem(WandDiscovery.PINE_WAND, "pine wand")
        registerItem(SOURCE_ITEM, "zap source")
        registerItem(TARGET_ITEM, "zap target")

        val engine = MockEngine {
            respond(
                "Your wand abruptly explodes. You acquire an item: <b>zap target</b>",
                HttpStatusCode.OK,
            )
        }
        val settings = MapSettings()
        val preferences = Preferences(settings)
        preferences.setInt("_zapCount", 5)
        val inventory = fakeInventoryManager(
            mapOf(
                WandDiscovery.PINE_WAND to InventoryItem(WandDiscovery.PINE_WAND, "pine wand", 1, ItemType.OTHER),
                SOURCE_ITEM to InventoryItem(SOURCE_ITEM, "zap source", 1, ItemType.OTHER),
            ),
        )
        val character = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(daycount = "99"))
        }
        val request = ZapRequest(
            client = HttpClient(engine),
            inventoryManager = inventory,
            preferences = preferences,
            character = character,
        )

        val result = request.zap(SOURCE_ITEM)
        assertTrue(result.isSuccess)
        assertEquals(TARGET_ITEM, result.getOrNull())
        assertEquals(null, inventory.state.value.items[WandDiscovery.PINE_WAND])
        assertEquals(0, preferences.getInt("_zapCount", -1))
        assertEquals(99, preferences.getInt("lastZapperWandExplosionDay", -3))
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
        }

    companion object {
        private const val SOURCE_ITEM = 9401
        private const val TARGET_ITEM = 9402
    }
}
