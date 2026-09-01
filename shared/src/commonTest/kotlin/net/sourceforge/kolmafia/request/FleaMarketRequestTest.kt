package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

class FleaMarketRequestTest {

    @Test
    fun buy_postsTownFleamarketWithBuyingWhichWhichitemHowmuch() = runTest {
        registerFleaItems()
        val requests = mutableListOf<CapturedRequest>()
        val client = buyClient(requests, listingHtml = LISTING_HTML, purchaseHtml = BUY_SUCCESS_HTML)
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(client)
        val preferences = Preferences(MapSettings())
        val request = FleaMarketRequest(
            client, inventory, character, sessionLogger(preferences), preferences,
        )

        val result = request.buy(CLOUDY_POTION, 1)

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        val post = requests.single { it.method == HttpMethod.Post }
        assertEquals("/town_fleamarket.php", post.path)
        assertForm(post.body, "buying", "Yep.")
        assertForm(post.body, "which", WHICH.toString())
        assertForm(post.body, "whichitem", CLOUDY_POTION.toString())
        assertForm(post.body, "howmuch", BUY_PRICE.toString())
        assertEquals(1, inventory.getCount(CLOUDY_POTION))
        assertEquals(STARTING_MEAT - BUY_PRICE, character.state.value.meat)
        assertTrue(
            preferences.getString(SessionLogger.SESSION_LOG_KEY, "").contains(
                "Purchased cloudy potion from Daryl Alenko ( #2395865 ) at the Flea Market for $BUY_PRICE meat.",
            ),
        )
    }

    @Test
    fun buy_serverRejectionPreservesInventoryAndMeat() = runTest {
        registerFleaItems()
        val client = buyClient(
            mutableListOf(),
            listingHtml = LISTING_HTML,
            purchaseHtml = BUY_REJECT_HTML,
        )
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(client)
        val preferences = Preferences(MapSettings())
        val request = FleaMarketRequest(
            client, inventory, character, sessionLogger(preferences), preferences,
        )

        val result = request.buy(CLOUDY_POTION, 1)

        assertTrue(result.isFailure)
        assertEquals(0, inventory.getCount(CLOUDY_POTION))
        assertEquals(STARTING_MEAT, character.state.value.meat)
        assertEquals("", preferences.getString(SessionLogger.SESSION_LOG_KEY, ""))
    }

    @Test
    fun buy_malformedHtmlPreservesInventoryAndMeat() = runTest {
        registerFleaItems()
        val client = buyClient(
            mutableListOf(),
            listingHtml = LISTING_HTML,
            purchaseHtml = "<html>the flea market is closed</html>",
        )
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(client)
        val request = FleaMarketRequest(client, inventory, character, null, null)

        val result = request.buy(CLOUDY_POTION, 1)

        assertTrue(result.isFailure)
        assertEquals(0, inventory.getCount(CLOUDY_POTION))
        assertEquals(STARTING_MEAT, character.state.value.meat)
    }

    @Test
    fun buy_rejectsInvalidQuantityOrUnresolvedItemWithoutHttp() = runTest {
        registerFleaItems()
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond(BUY_SUCCESS_HTML, HttpStatusCode.OK)
        })
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(client)
        val request = FleaMarketRequest(client, inventory, character, null, null)

        assertTrue(request.buy(CLOUDY_POTION, 0).isFailure)
        assertTrue(request.buy(CLOUDY_POTION, -1).isFailure)
        assertTrue(request.buy(MISSING_ITEM, 1).isFailure)
        assertEquals(0, calls)
        assertEquals(0, inventory.getCount(CLOUDY_POTION))
        assertEquals(STARTING_MEAT, character.state.value.meat)
    }

    @Test
    fun buy_twoIdenticalCallsEachGainAndSpend() = runTest {
        registerFleaItems()
        val client = buyClient(mutableListOf(), LISTING_HTML, BUY_SUCCESS_HTML)
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(client)
        val request = FleaMarketRequest(client, inventory, character, null, null)

        assertTrue(request.buy(CLOUDY_POTION, 1).isSuccess)
        assertTrue(request.buy(CLOUDY_POTION, 1).isSuccess)

        assertEquals(2, inventory.getCount(CLOUDY_POTION))
        assertEquals(STARTING_MEAT - BUY_PRICE * 2, character.state.value.meat)
    }

    @Test
    fun buy_thenVisitHook_doesNotDoubleGain() = runTest {
        registerFleaItems()
        val client = buyClient(mutableListOf(), LISTING_HTML, BUY_SUCCESS_HTML)
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(client)
        val request = FleaMarketRequest(client, inventory, character, null, null)
        val library = GameRuntimeLibrary(
            character = character,
            inventoryManager = inventory,
            fleaMarketRequest = request,
        )

        val result = request.buy(CLOUDY_POTION, 1)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        library.processVisitResponseHooks(result.getOrThrow(), BUY_URL)

        assertEquals(1, inventory.getCount(CLOUDY_POTION))
        assertEquals(STARTING_MEAT - BUY_PRICE, character.state.value.meat)
    }

    @Test
    fun visitHook_routesFleaBuyIdempotently() {
        registerFleaItems()
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(HttpClient(MockEngine { respond("") }))
        val library = GameRuntimeLibrary(
            character = character,
            inventoryManager = inventory,
        )

        library.processVisitResponseHooks(BUY_SUCCESS_HTML, BUY_URL)
        library.processVisitResponseHooks(BUY_SUCCESS_HTML, BUY_URL)

        assertEquals(1, inventory.getCount(CLOUDY_POTION))
        assertEquals(STARTING_MEAT - BUY_PRICE, character.state.value.meat)
    }

    @Test
    fun visitHook_malformedBuyResponseDoesNotMutate() {
        registerFleaItems()
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(HttpClient(MockEngine { respond("") }))
        val library = GameRuntimeLibrary(
            character = character,
            inventoryManager = inventory,
        )

        library.processVisitResponseHooks(
            "<html>the flea market is closed</html>",
            BUY_URL,
        )

        assertEquals(0, inventory.getCount(CLOUDY_POTION))
        assertEquals(STARTING_MEAT, character.state.value.meat)
    }

    @Test
    fun sell_postsTownSellfleaWithWhichitemSellpriceSelling() = runTest {
        registerFleaItems()
        val requests = mutableListOf<CapturedRequest>()
        val client = sellClient(requests, SELL_SUCCESS_HTML)
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(client, ELEVEN_LEAF_CLOVER to 2)
        val preferences = Preferences(MapSettings())
        val request = FleaMarketSellRequest(
            client, inventory, character, sessionLogger(preferences), preferences,
        )

        val result = request.sell(ELEVEN_LEAF_CLOVER, 1, SELL_PRICE)

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        val post = requests.single { it.method == HttpMethod.Post }
        assertEquals("/town_sellflea.php", post.path)
        assertForm(post.body, "whichitem", ELEVEN_LEAF_CLOVER.toString())
        assertForm(post.body, "sellprice", SELL_PRICE.toString())
        assertForm(post.body, "selling", "Yep.")
        assertEquals(1, inventory.getCount(ELEVEN_LEAF_CLOVER))
        assertEquals(STARTING_MEAT, character.state.value.meat, "listing must not invent sale-price meat")
        assertTrue(
            preferences.getString(SessionLogger.SESSION_LOG_KEY, "").contains(
                "Placed 11-leaf clover up for sale at the Flea Market for $SELL_PRICE meat.",
            ),
        )
    }

    @Test
    fun sell_serverRejectionPreservesInventoryAndMeat() = runTest {
        registerFleaItems()
        val client = sellClient(mutableListOf(), SELL_REJECT_HTML)
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(client, ELEVEN_LEAF_CLOVER to 2)
        val request = FleaMarketSellRequest(client, inventory, character, null, null)

        val result = request.sell(ELEVEN_LEAF_CLOVER, 1, SELL_PRICE)

        assertTrue(result.isFailure)
        assertEquals(2, inventory.getCount(ELEVEN_LEAF_CLOVER))
        assertEquals(STARTING_MEAT, character.state.value.meat)
    }

    @Test
    fun sell_malformedHtmlPreservesInventory() = runTest {
        registerFleaItems()
        val client = sellClient(mutableListOf(), "<html>not a flea market</html>")
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(client, ELEVEN_LEAF_CLOVER to 2)
        val request = FleaMarketSellRequest(client, inventory, character, null, null)

        val result = request.sell(ELEVEN_LEAF_CLOVER, 1, SELL_PRICE)

        assertTrue(result.isFailure)
        assertEquals(2, inventory.getCount(ELEVEN_LEAF_CLOVER))
        assertEquals(STARTING_MEAT, character.state.value.meat)
    }

    @Test
    fun sell_rejectsUnownedQuantityOrNonPositivePriceWithoutHttp() = runTest {
        registerFleaItems()
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond(SELL_SUCCESS_HTML, HttpStatusCode.OK)
        })
        val character = characterWithMeat(STARTING_MEAT)
        val owned = inventory(client, ELEVEN_LEAF_CLOVER to 1)
        val request = FleaMarketSellRequest(client, owned, character, null, null)

        assertTrue(request.sell(ELEVEN_LEAF_CLOVER, 2, SELL_PRICE).isFailure)
        assertTrue(request.sell(ELEVEN_LEAF_CLOVER, 0, SELL_PRICE).isFailure)
        assertTrue(request.sell(ELEVEN_LEAF_CLOVER, 1, 0).isFailure)
        assertTrue(request.sell(ELEVEN_LEAF_CLOVER, 1, -5).isFailure)
        assertTrue(request.sell(MISSING_ITEM, 1, SELL_PRICE).isFailure)
        assertEquals(0, calls)
        assertEquals(1, owned.getCount(ELEVEN_LEAF_CLOVER))
        assertEquals(STARTING_MEAT, character.state.value.meat)
    }

    @Test
    fun sell_twoIdenticalCallsEachRemoveOne() = runTest {
        registerFleaItems()
        val client = sellClient(mutableListOf(), SELL_SUCCESS_HTML)
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(client, ELEVEN_LEAF_CLOVER to 2)
        val request = FleaMarketSellRequest(client, inventory, character, null, null)

        assertTrue(request.sell(ELEVEN_LEAF_CLOVER, 1, SELL_PRICE).isSuccess)
        assertTrue(request.sell(ELEVEN_LEAF_CLOVER, 1, SELL_PRICE).isSuccess)

        assertEquals(0, inventory.getCount(ELEVEN_LEAF_CLOVER))
        assertEquals(STARTING_MEAT, character.state.value.meat)
    }

    @Test
    fun sell_thenVisitHook_doesNotDoubleConsume() = runTest {
        registerFleaItems()
        val client = sellClient(mutableListOf(), SELL_SUCCESS_HTML)
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(client, ELEVEN_LEAF_CLOVER to 2)
        val request = FleaMarketSellRequest(client, inventory, character, null, null)
        val library = GameRuntimeLibrary(
            character = character,
            inventoryManager = inventory,
            fleaMarketSellRequest = request,
        )

        val result = request.sell(ELEVEN_LEAF_CLOVER, 1, SELL_PRICE)
        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        library.processVisitResponseHooks(result.getOrThrow(), SELL_URL)

        assertEquals(1, inventory.getCount(ELEVEN_LEAF_CLOVER))
        assertEquals(STARTING_MEAT, character.state.value.meat)
    }

    @Test
    fun visitHook_routesFleaSellIdempotently() {
        registerFleaItems()
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(
            HttpClient(MockEngine { respond("") }),
            ELEVEN_LEAF_CLOVER to 2,
        )
        val library = GameRuntimeLibrary(
            character = character,
            inventoryManager = inventory,
        )

        library.processVisitResponseHooks(SELL_SUCCESS_HTML, SELL_URL)
        library.processVisitResponseHooks(SELL_SUCCESS_HTML, SELL_URL)

        assertEquals(1, inventory.getCount(ELEVEN_LEAF_CLOVER))
        assertEquals(STARTING_MEAT, character.state.value.meat)
    }

    @Test
    fun requestLogger_identifiesFleaBuyAndSellNotMall() {
        registerFleaItems()
        val preferences = Preferences(MapSettings())
        val logger = sessionLogger(preferences)
        RequestLogger.currentRound = { 0 }

        assertTrue(RequestLogger.registerRequest(BUY_URL, logger, preferences))
        assertTrue(RequestLogger.registerRequest(SELL_URL, logger, preferences))
        val lines = logger.recentLines().joinToString("\n")
        assertTrue(lines.contains("Purchasing cloudy potion from the Flea Market for $BUY_PRICE meat."))
        assertTrue(lines.contains("Placing 11-leaf clover up for sale at the Flea Market for $SELL_PRICE meat."))
        assertFalse(lines.contains("\nmall\n") || lines == "mall" || lines.contains("mall\n"))
        assertFalse(lines.split('\n').any { it.trim() == "mall" })
    }

    @Test
    fun buy_abortsWithoutHttpWhenInFightOrChoice() = runTest {
        registerFleaItems()
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            respond(BUY_SUCCESS_HTML, HttpStatusCode.OK)
        })
        val character = characterWithMeat(STARTING_MEAT)
        val inventory = inventory(client)
        val request = FleaMarketRequest(client, inventory, character, null, null)
        RequestAbortGate.forceAbort = true
        try {
            val result = request.buy(CLOUDY_POTION, 1)
            assertTrue(result.isFailure)
            assertEquals(0, calls)
            assertEquals(0, inventory.getCount(CLOUDY_POTION))
        } finally {
            RequestAbortGate.resetForTest()
        }
    }

    private data class CapturedRequest(val method: HttpMethod, val path: String, val body: String)

    private fun buyClient(
        requests: MutableList<CapturedRequest>,
        listingHtml: String,
        purchaseHtml: String,
    ): HttpClient = HttpClient(MockEngine { request ->
        val body = request.body.toByteArray().decodeToString()
        requests += CapturedRequest(request.method, request.url.encodedPath, body)
        when (request.method) {
            HttpMethod.Get -> respond(listingHtml, HttpStatusCode.OK)
            else -> respond(purchaseHtml, HttpStatusCode.OK)
        }
    })

    private fun sellClient(
        requests: MutableList<CapturedRequest>,
        html: String,
    ): HttpClient = HttpClient(MockEngine { request ->
        val body = request.body.toByteArray().decodeToString()
        requests += CapturedRequest(request.method, request.url.encodedPath, body)
        respond(html, HttpStatusCode.OK)
    })

    private fun sessionLogger(preferences: Preferences) = SessionLogger(preferences, GameEventBus())

    private fun characterWithMeat(meat: Int) = KoLCharacter().also { it.updateMeat(meat) }

    private fun inventory(
        client: HttpClient,
        vararg counts: Pair<Int, Int>,
    ): InventoryManager = InventoryManager(client, GameEventBus()).also { manager ->
        manager.applyParsedInventory(
            buildMap {
                counts.filter { it.second > 0 }.forEach { (id, qty) ->
                    put(id, InventoryItem(id, ItemDatabase.getItemName(id).ifEmpty { "item #$id" }, qty, ItemType.OTHER))
                }
            },
        )
    }

    private fun registerFleaItems() {
        registerItem(CLOUDY_POTION, "cloudy potion")
        registerItem(ELEVEN_LEAF_CLOVER, "11-leaf clover")
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
                autosellPrice = 5,
                plural = null,
            ),
        )
    }

    private fun assertForm(body: String, key: String, expected: String) {
        val actual = Regex("""(?:^|&)$key=([^&]+)""").find(body)?.groupValues?.get(1)
            ?.replace("+", " ")
            ?.replace("%2E", ".", ignoreCase = true)
        assertEquals(expected, actual, body)
    }

    companion object {
        private const val CLOUDY_POTION = 823
        private const val ELEVEN_LEAF_CLOVER = 10881
        private const val MISSING_ITEM = 88001
        private const val WHICH = 13
        private const val BUY_PRICE = 125
        private const val SELL_PRICE = 18000
        private const val STARTING_MEAT = 1000
        private const val BUY_URL =
            "town_fleamarket.php?buying=Yep.&which=$WHICH&whichitem=$CLOUDY_POTION&howmuch=$BUY_PRICE"
        private const val SELL_URL =
            "town_sellflea.php?whichitem=$ELEVEN_LEAF_CLOVER&sellprice=$SELL_PRICE&selling=Yep."
        private const val LISTING_HTML =
            """<form method=post action="town_fleamarket.php"><input type=hidden name=buying value="Yep."><input type=hidden name=which value=$WHICH><input type=hidden name=whichitem value=$CLOUDY_POTION><input type=hidden name=howmuch value=$BUY_PRICE></form>"""
        private const val BUY_SUCCESS_HTML =
            """<center>You purchase the item from Daryl Alenko ( #2395865 )<center><table class="item" rel="id=$CLOUDY_POTION&s=60&q=0&d=1&g=0&t=1&n=1&m=1&p=0&u=u"><tr><td></td><td valign=center class=effect>You acquire an item: <b>cloudy potion</b></td></tr></table>"""
        private const val BUY_REJECT_HTML = "You can't afford to buy that item."
        private const val SELL_SUCCESS_HTML =
            "You place your item for sale in the Flea Market.  It will be returned to you if it does not sell within 48 hours."
        private const val SELL_REJECT_HTML = "You don't have that item to sell."
    }
}
