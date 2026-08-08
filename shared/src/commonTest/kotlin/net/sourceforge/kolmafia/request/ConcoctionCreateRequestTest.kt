package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterManager
import net.sourceforge.kolmafia.shop.CoinmasterRequest
import net.sourceforge.kolmafia.shop.ShopRequest

class ConcoctionCreateRequestTest {

    private fun createItemIngredients(
        retrieveFn: suspend (Int, Int) -> Int = { _, qty -> qty },
    ): CreateItemIngredients = CreateItemIngredients(StubRetrieveItemService(retrieveFn), null)

    private class StubRetrieveItemService(
        private val retrieveFn: suspend (Int, Int) -> Int = { _, qty -> qty },
    ) : RetrieveItemService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
    ) {
        override suspend fun retrieve(itemId: Int, qty: Int): Int = retrieveFn(itemId, qty)
    }

    private class StubCoinmasterManager(
        private val buyFn: suspend (Int, Int) -> Int,
    ) : CoinmasterManager(
        coinmasterRequest = CoinmasterRequest(HttpClient(MockEngine { respond("ok") })),
        inventoryManager = null,
        gameDatabase = null,
        client = HttpClient(MockEngine { respond("ok") }),
    ) {
        override suspend fun buyItem(itemId: Int, quantity: Int): Int = buyFn(itemId, quantity)
    }

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun create_routesStillToShopBuy() = runTest {
        val ginId = 93101
        val resultId = 93102
        registerItem(ginId, "still gin")
        registerItem(resultId, "still result booze")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "still result booze",
                resultQuantity = 1,
                methods = setOf("STILL", "ROW42"),
                ingredients = listOf(ConcoctionIngredient("still gin", 1)),
            ),
        )
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("<html>You acquire an item: <b>still result booze</b></html>", HttpStatusCode.OK)
        })
        val retrieve = StubRetrieveItemService()
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = retrieve,
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            createItemIngredients = CreateItemIngredients(retrieve, null),
            shopRequest = ShopRequest(client),
        )

        val result = createRequest.create(
            "still result booze",
            1,
            CharacterState(stillsAvailable = 1),
            Preferences(MapSettings()),
        )

        assertTrue(result.isSuccess)
        assertTrue(bodies.single().contains("whichrow=42"))
    }

    @Test
    fun create_routesCoinmasterToBuyItem() = runTest {
        val itemId = 93111
        registerItem(itemId, "coinmaster craft item")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "coinmaster craft item",
                resultQuantity = 1,
                methods = setOf("COINMASTER"),
                ingredients = emptyList(),
            ),
        )
        var bought: Pair<Int, Int>? = null
        val client = HttpClient(MockEngine { respond("ok") })
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService(),
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            coinmasterManager = StubCoinmasterManager { id, qty ->
                bought = id to qty
                qty
            },
        )

        val result = createRequest.create("coinmaster craft item", 3)

        assertTrue(result.isSuccess)
        assertEquals(itemId to 3, bought)
    }

    @Test
    fun create_unsupportedMethod_returnsFailure() = runTest {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "clip art poster",
                resultQuantity = 1,
                methods = setOf("CLIPART"),
                ingredients = emptyList(),
            ),
        )
        val client = HttpClient(MockEngine { respond("ok") })
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService(),
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
        )

        val result = createRequest.create("clip art poster", 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun create_routesClipArtToCampground() = runTest {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "Ur-Donut",
                resultQuantity = 1,
                methods = setOf("CLIPART"),
                ingredients = emptyList(),
                param = 0x010101,
            ),
        )
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("You acquire an item: <b>Ur-Donut</b>", HttpStatusCode.OK)
        })
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService(),
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            clipArtCreateRequest = ClipArtCreateRequest(client),
        )

        val result = createRequest.create("Ur-Donut", 1)

        assertTrue(result.isSuccess)
        assertTrue(bodies.single().contains("preaction=combinecliparts"))
    }

    @Test
    fun create_routesRollToRollingPin() = runTest {
        registerItem(159, "wad of dough")
        registerItem(301, "flat dough")
        registerItem(873, "rolling pin")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "flat dough",
                resultQuantity = 1,
                methods = setOf("ROLL"),
                ingredients = listOf(ConcoctionIngredient("wad of dough", 1)),
            ),
        )
        val usedItems = mutableListOf<Int>()
        val client = HttpClient(MockEngine { request ->
            usedItems += request.url.parameters["whichitem"]?.toInt() ?: -1
            respond("You acquire an item: <b>flat dough</b>", HttpStatusCode.OK)
        })
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            rollingPinCreateRequest = RollingPinCreateRequest(
                useItemRequest = UseItemRequest(client),
                retrieveItemService = StubRetrieveItemService { _, qty -> qty },
                gameDatabase = null,
            ),
        )

        val result = createRequest.create("flat dough", 1)

        assertTrue(result.isSuccess)
        assertEquals(listOf(873), usedItems)
    }

    @Test
    fun create_routesTerminalToExtrude() = runTest {
        registerItem(9034, "Source essence")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "browser cookie",
                resultQuantity = 1,
                methods = setOf("TERMINAL"),
                ingredients = listOf(ConcoctionIngredient("Source essence", 10)),
            ),
        )
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("You acquire an item: <b>browser cookie</b>", HttpStatusCode.OK)
        })
        val terminalRequest = TerminalRequest(
            client = client,
            campgroundRequest = CampgroundRequest(client),
            falloutShelterRequest = FalloutShelterRequest(client),
        )
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_campgroundHasSourceTerminal", true)
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            terminalExtrudeCreateRequest = TerminalExtrudeCreateRequest(
                terminalRequest = terminalRequest,
                createItemIngredients = CreateItemIngredients(
                    StubRetrieveItemService { _, qty -> qty },
                    gameDatabase = null,
                ),
            ),
        )

        val result = createRequest.create("browser cookie", 1, preferences = prefs)

        assertTrue(result.isSuccess)
        assertTrue(bodies.any { it.contains("action=terminal") })
        assertTrue(bodies.any { it.contains("whichchoice=1191") })
        assertTrue(bodies.any { it.contains("input=extrude") })
    }

    @Test
    fun create_routesSewerToGumUse() = runTest {
        registerItem(23, "chewing gum on a string")
        registerItem(2283, "seal-skull helmet")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "seal-skull helmet",
                resultQuantity = 1,
                methods = setOf("SEWER"),
                ingredients = listOf(ConcoctionIngredient("chewing gum on a string", 1)),
            ),
        )
        val usedItems = mutableListOf<Int>()
        val client = HttpClient(MockEngine { request ->
            usedItems += request.url.parameters["whichitem"]?.toInt() ?: -1
            respond("You acquire an item: <b>seal-skull helmet</b>", HttpStatusCode.OK)
        })
        val inventory = mutableMapOf<Int, Int>()
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            sewerCreateRequest = SewerCreateRequest(
                useItemRequest = UseItemRequest(client),
                closetRequest = ClosetRequest(client),
                createItemIngredients = CreateItemIngredients(
                    StubRetrieveItemService { _, qty -> qty },
                    gameDatabase = null,
                ),
                gameDatabase = null,
                inventoryCountById = { id -> inventory[id] ?: 0 },
            ),
        )

        val result = createRequest.create("seal-skull helmet", 1)

        assertTrue(result.isSuccess)
        assertEquals(listOf(23), usedItems)
    }

    @Test
    fun create_routesVykeaToChoiceChain() = runTest {
        registerItem(8730, "VYKEA instructions")
        registerItem(8729, "VYKEA hex key")
        registerItem(8725, "VYKEA plank")
        registerItem(8726, "VYKEA rail")
        registerItem(8727, "VYKEA bracket")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "level 1 bookshelf",
                resultQuantity = 1,
                methods = setOf("VYKEA"),
                ingredients = listOf(
                    ConcoctionIngredient("VYKEA instructions", 1),
                    ConcoctionIngredient("VYKEA plank", 5),
                    ConcoctionIngredient("VYKEA plank", 5),
                ),
            ),
        )
        val choicePosts = mutableListOf<String>()
        var choiceStep = 0
        val choiceSequence = listOf(1121, 1122, 1123)
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.toString().contains("inv_use.php") -> {
                    respond("""<input name="whichchoice" value="1120">""", HttpStatusCode.OK)
                }
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("choice.php") -> {
                    choicePosts += request.body.toByteArray().decodeToString()
                    val nextChoice = choiceSequence.getOrElse(choiceStep) { 0 }
                    if (choiceStep < choiceSequence.size) choiceStep++
                    respond("""<input name="whichchoice" value="$nextChoice">""", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            vykeaCreateRequest = VykeaCreateRequest(
                useItemRequest = UseItemRequest(client),
                choiceRequest = net.sourceforge.kolmafia.adventure.ChoiceRequest(client),
                retrieveItemService = StubRetrieveItemService { _, qty -> qty },
                createItemIngredients = CreateItemIngredients(
                    StubRetrieveItemService { _, qty -> qty },
                    gameDatabase = null,
                ),
                vykeaCompanionManager = net.sourceforge.kolmafia.vykea.VykeaCompanionManager(
                    Preferences(MapSettings()),
                ),
                gameDatabase = null,
            ),
        )

        val result = createRequest.create("level 1 bookshelf", 1)

        assertTrue(result.isSuccess)
        assertEquals(4, choicePosts.size)
        assertTrue(choicePosts[0].contains("whichchoice=1120"))
    }

    @Test
    fun create_routesMuseToMultiUse() = runTest {
        registerItem(88011, "smoked potsherd")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "pottery yo-yo",
                resultQuantity = 1,
                methods = setOf("MUSE"),
                ingredients = listOf(ConcoctionIngredient("smoked potsherd", 5)),
            ),
        )
        val multiUsePosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("multiuse.php") -> {
                    multiUsePosts += request.body.toByteArray().decodeToString()
                    respond("You acquire an item: <b>pottery yo-yo</b>", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            museCreateRequest = MuseCreateRequest(
                useItemRequest = UseItemRequest(client),
                createItemIngredients = CreateItemIngredients(
                    StubRetrieveItemService { _, qty -> qty },
                    gameDatabase = null,
                ),
                gameDatabase = null,
            ),
        )

        val result = createRequest.create("pottery yo-yo", 1)

        assertTrue(result.isSuccess)
        assertEquals(1, multiUsePosts.size)
        assertTrue(multiUsePosts.single().contains("whichitem=88011"))
        assertTrue(multiUsePosts.single().contains("quantity=5"))
    }

    @Test
    fun create_routesPhineasToVolcanoisland() = runTest {
        registerItem(88101, "sealhide hood")
        registerItem(88102, "hellseal brain")
        registerItem(88103, "hellseal sinew")
        registerItem(88104, "hellseal hide")
        registerItem(88105, "hellseal whisker")
        registerItem(88106, "hellseal claw")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "sealhide hood",
                resultQuantity = 1,
                methods = setOf("PHINEAS"),
                ingredients = listOf(
                    ConcoctionIngredient("hellseal brain", 3),
                    ConcoctionIngredient("hellseal sinew", 2),
                    ConcoctionIngredient("hellseal hide", 2),
                    ConcoctionIngredient("hellseal whisker", 3),
                    ConcoctionIngredient("hellseal claw", 5),
                ),
            ),
        )
        val phineasPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("volcanoisland.php") -> {
                    phineasPosts += request.body.toByteArray().decodeToString()
                    respond("You acquire an item: <b>sealhide hood</b>", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            phineasCreateRequest = PhineasCreateRequest(
                client = client,
                createItemIngredients = createItemIngredients { _, qty -> qty },
                gameDatabase = null,
            ),
        )

        val result = createRequest.create("sealhide hood", 1)

        assertTrue(result.isSuccess)
        assertEquals(1, phineasPosts.size)
        assertTrue(phineasPosts.single().contains("makewhich=88101"))
    }

    @Test
    fun create_routesStaffToGuild() = runTest {
        registerItem(88201, "big stirring stick")
        registerItem(88202, "menudo")
        registerItem(88203, "sangria")
        registerItem(88204, "hippy herbal tea")
        registerItem(88205, "concentrated magicalness pill")
        registerItem(88206, "magical mystery juice (3)")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "Staff of the Teapot Tempest",
                resultQuantity = 1,
                methods = setOf("STAFF"),
                ingredients = listOf(
                    ConcoctionIngredient("big stirring stick", 1),
                    ConcoctionIngredient("menudo", 1),
                    ConcoctionIngredient("sangria", 1),
                    ConcoctionIngredient("hippy herbal tea", 1),
                    ConcoctionIngredient("concentrated magicalness pill", 1),
                    ConcoctionIngredient("magical mystery juice (3)", 1),
                ),
            ),
        )
        val staffPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("guild.php") -> {
                    staffPosts += request.body.toByteArray().decodeToString()
                    respond("You acquire an item: <b>Staff of the Teapot Tempest</b>", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            staffCreateRequest = StaffCreateRequest(
                client = client,
                createItemIngredients = createItemIngredients { _, qty -> qty },
                gameDatabase = null,
            ),
        )

        val result = createRequest.create("Staff of the Teapot Tempest", 1)

        assertTrue(result.isSuccess)
        assertEquals(1, staffPosts.size)
        assertTrue(staffPosts.single().contains("whichstaff=88201"))
    }

    @Test
    fun create_routesTinkerToGnomes() = runTest {
        registerItem(88301, "flange")
        registerItem(88302, "cog")
        registerItem(88303, "sprocket")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "clockwork widget",
                resultQuantity = 1,
                methods = setOf("TINKER"),
                ingredients = listOf(
                    ConcoctionIngredient("flange", 1),
                    ConcoctionIngredient("cog", 1),
                    ConcoctionIngredient("sprocket", 1),
                ),
            ),
        )
        val tinkerPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("gnomes.php") -> {
                    tinkerPosts += request.body.toByteArray().decodeToString()
                    respond("Gnorman deftly assembles your items into something new.", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            gnomeTinkerCreateRequest = GnomeTinkerCreateRequest(
                client = client,
                createItemIngredients = createItemIngredients { _, qty -> qty },
                gameDatabase = null,
            ),
        )

        val result = createRequest.create("clockwork widget", 1)

        assertTrue(result.isSuccess)
        assertEquals(1, tinkerPosts.size)
        assertTrue(tinkerPosts.single().contains("item1=88301"))
    }

    @Test
    fun create_routesSushiToSushiPhp() = runTest {
        registerItem(89101, "beefy fish meat")
        registerItem(89102, "white rice")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "beefy nigiri",
                resultQuantity = 1,
                methods = setOf("SUSHI"),
                ingredients = listOf(
                    ConcoctionIngredient("beefy fish meat", 1),
                    ConcoctionIngredient("white rice", 1),
                ),
            ),
        )
        val sushiPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("sushi.php") -> {
                    sushiPosts += request.body.toByteArray().decodeToString()
                    respond("You eat the beefy nigiri. Delicious!", HttpStatusCode.OK)
                }
                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            sushiCreateRequest = SushiCreateRequest(
                client = client,
                createItemIngredients = createItemIngredients { _, qty -> qty },
                gameDatabase = null,
            ),
        )

        val result = createRequest.create("beefy nigiri", 1)

        assertTrue(result.isSuccess)
        assertEquals(1, sushiPosts.size)
        assertTrue(sushiPosts.single().contains("whichsushi=1"))
    }

    @Test
    fun create_stationCraft_callsMakeIngredientsBeforeCraft() = runTest {
        val ing1Id = 94001
        val ing2Id = 94002
        val resultId = 94003
        registerItem(ing1Id, "station ing a")
        registerItem(ing2Id, "station ing b")
        registerItem(resultId, "station result")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "station result",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(
                    ConcoctionIngredient("station ing a", 1),
                    ConcoctionIngredient("station ing b", 1),
                ),
            ),
        )
        val retrieved = mutableListOf<Pair<Int, Int>>()
        val retrieve = StubRetrieveItemService { id, qty ->
            retrieved += id to qty
            qty
        }
        val client = HttpClient(MockEngine { respond("<!-- cr:1x$ing1Id,$ing2Id=$resultId -->") })
        var craftCalled = false
        val craft = object : CraftRequest(client) {
            override suspend fun craft(mode: String, quantity: Int, itemId1: Int, itemId2: Int): Int {
                craftCalled = true
                assertTrue(retrieved.isNotEmpty())
                return quantity
            }
        }
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = retrieve,
            craftRequest = craft,
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            createItemIngredients = CreateItemIngredients(retrieve, null),
        )

        val result = createRequest.create("station result", 1)

        assertTrue(result.isSuccess)
        assertTrue(craftCalled)
    }

    @Test
    fun create_suseCraft_callsMakeIngredientsBeforeUse() = runTest {
        val sourceId = 95001
        val resultId = 95002
        registerItem(sourceId, "suse source")
        registerItem(resultId, "suse result")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "suse result",
                resultQuantity = 1,
                methods = setOf("SUSE"),
                ingredients = listOf(ConcoctionIngredient("suse source", 1)),
            ),
        )
        val retrieved = mutableListOf<Int>()
        val retrieve = StubRetrieveItemService { id, qty ->
            retrieved += id
            qty
        }
        var useCalled = false
        val client = HttpClient(MockEngine { respond("ok") })
        val use = object : UseItemRequest(client) {
            override suspend fun use(itemId: Int, quantity: Int): Result<String> {
                useCalled = true
                assertTrue(sourceId in retrieved)
                return Result.success("ok")
            }
        }
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = retrieve,
            craftRequest = CraftRequest(client),
            useItemRequest = use,
            gameDatabase = null,
            createItemIngredients = CreateItemIngredients(retrieve, null),
        )

        val result = createRequest.create("suse result", 1)

        assertTrue(result.isSuccess)
        assertTrue(useCalled)
    }

    @Test
    fun create_stationCraft_makeIngredientsFailure_returnsPartial() = runTest {
        val ing1Id = 94101
        val ing2Id = 94102
        registerItem(ing1Id, "fail ing a")
        registerItem(ing2Id, "fail ing b")
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "fail station result",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(
                    ConcoctionIngredient("fail ing a", 1),
                    ConcoctionIngredient("fail ing b", 1),
                ),
            ),
        )
        val client = HttpClient(MockEngine { respond("<!-- cr:1x$ing1Id,$ing2Id=94103 -->") })
        val retrieve = StubRetrieveItemService { _, _ -> 0 }
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = retrieve,
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            createItemIngredients = CreateItemIngredients(retrieve, null),
        )

        val result = createRequest.create("fail station result", 2)

        assertTrue(result.isFailure)
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
                autosellPrice = 100,
                plural = null,
            ),
        )
    }
}
