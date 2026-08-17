package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.UntinkerRequest

class GameRuntimeLibraryAshP234Test {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
        UntinkerRequest.resetForTest()
    }

    @Test
    fun revision_phase222() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun cliUntinker_noArgs_completesQuest() {
        val urls = mutableListOf<String>()
        val engine = MockEngine { request ->
            val payload = requestPayload(request)
            urls += payload
            when {
                payload.contains("preaction=screwquest") ->
                    respond("I'm just lost without my screwdriver", HttpStatusCode.OK)
                payload.contains("dk_innabox") ->
                    respond("ok", HttpStatusCode.OK)
                payload.contains("action=fv_untinker") ->
                    respond("<select name=whichitem></select>", HttpStatusCode.OK)
                else -> respond("ok", HttpStatusCode.OK)
            }
        }

        val char = KoLCharacter()
        char.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(sign = "Mongoose"),
        )

        val lib = GameRuntimeLibrary(
            character = char,
            untinkerRequest = UntinkerRequest(
                client = HttpClient(engine),
                character = char,
            ),
        )

        runLib(lib, """cli_execute("untinker");""")
        assertTrue(urls.any { it.contains("screwquest") || it.contains("preaction=screwquest") })
    }

    @Test
    fun cliUntinker_item_routesToLegionScrewdriverWhenAccessible() {
        registerUntinkerItem(MEAT_PASTE_ITEM, "meat paste item")
        registerItem(MEAT_PASTE_ITEM, "meat paste item")
        registerItem(UntinkerRequest.LOATHING_LEGION_SCREWDRIVER, "Loathing Legion universal screwdriver")

        val urls = mutableListOf<String>()
        val engine = MockEngine { request ->
            urls += request.url.toString()
            respond("You acquire an item: meat paste", HttpStatusCode.OK)
        }

        val inventory = TestInventoryManager(
            mapOf(
                MEAT_PASTE_ITEM to InventoryItem(MEAT_PASTE_ITEM, "meat paste item", 1, ItemType.OTHER),
                UntinkerRequest.LOATHING_LEGION_SCREWDRIVER to InventoryItem(
                    UntinkerRequest.LOATHING_LEGION_SCREWDRIVER,
                    "Loathing Legion universal screwdriver",
                    1,
                    ItemType.OTHER,
                ),
            ),
        )

        val db = GameDatabase()
        val lib = GameRuntimeLibrary(
            character = KoLCharacter(),
            inventoryManager = inventory,
            gameDatabase = db,
            untinkerRequest = UntinkerRequest(
                client = HttpClient(engine),
                inventoryManager = inventory,
                gameDatabase = db,
            ),
        )

        runLib(lib, """cli_execute("untinker meat paste item");""")
        assertEquals(1, urls.size)
        assertTrue(urls[0].contains("action=screw"))
        assertTrue(urls[0].contains("whichitem=${UntinkerRequest.LOATHING_LEGION_SCREWDRIVER}"))
    }

    private class TestInventoryManager(
        items: Map<Int, InventoryItem>,
    ) : InventoryManager(HttpClient(MockEngine { respond("ok") }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = items))
        override val state = flow.asStateFlow()

        override suspend fun fetchInventory() {
        }

        override fun consumeItemLocally(itemId: Int, quantity: Int) {
            val map = flow.value.items.toMutableMap()
            val current = map[itemId]?.quantity ?: 0
            val next = (current - quantity).coerceAtLeast(0)
            if (next == 0) map.remove(itemId) else map[itemId] = map[itemId]!!.copy(quantity = next)
            flow.value = flow.value.copy(items = map)
        }
    }

    private fun registerUntinkerItem(id: Int, name: String) {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = name,
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("meat paste", 1)),
            ),
        )
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
                access = emptySet(),
                autosellPrice = 0,
                plural = null,
            ),
        )
    }

    private fun requestPayload(request: io.ktor.client.request.HttpRequestData): String = runBlocking {
        val body = if (request.method == HttpMethod.Post) {
            request.body.toByteArray().decodeToString()
        } else {
            ""
        }
        buildString {
            append(request.url.toString())
            if (body.isNotBlank()) {
                append('&')
                append(body)
            }
        }
    }

    companion object {
        private const val MEAT_PASTE_ITEM = 1001
    }
}
