package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.ConcoctionCraftQueue
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ConcoctionOrganAmounts.QueueBucket
import net.sourceforge.kolmafia.data.ConcoctionQueueBudget
import net.sourceforge.kolmafia.data.ConcoctionQueueContext
import net.sourceforge.kolmafia.data.ConcoctionRefreshContext
import net.sourceforge.kolmafia.data.ConcoctionRuntimeState
import net.sourceforge.kolmafia.data.ConsumableData
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.ConsumableQuality
import net.sourceforge.kolmafia.data.ConsumableType
import net.sourceforge.kolmafia.data.DailyLimitDatabase
import net.sourceforge.kolmafia.data.DailyLimitEntry
import net.sourceforge.kolmafia.data.DailyLimitKind
import net.sourceforge.kolmafia.data.HotDogAvailability
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.data.SpeakeasyAvailability
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CafePurchaseRequest
import net.sourceforge.kolmafia.request.CafeRequest
import net.sourceforge.kolmafia.request.ChewRequest
import net.sourceforge.kolmafia.request.ChezSnooteeRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ConcoctionCreateRequest
import net.sourceforge.kolmafia.request.CraftRequest
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import net.sourceforge.kolmafia.request.HellKitchenRequest
import net.sourceforge.kolmafia.request.MicroBreweryRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.StillSuitRequest

class ConcoctionQueueRunnerTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ConsumableDatabase.resetForTest()
        DailyLimitDatabase.resetForTest()
        ConcoctionCraftQueue.resetForTest()
        ConcoctionQueueBudget.resetForTest()
        HotDogAvailability.resetForTest()
        SpeakeasyAvailability.resetForTest()
        StandardRequest.resetForTest()
    }

    @Test
    fun handleQueue_eatHotDog_postsOncePerQueuedQuantity() = runTest {
        setupBasicHotDogQueue(quantity = 2)
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("You gain some stats.")
        })
        val runner = ConcoctionQueueRunner(ClanLoungeRequest(client))

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
        )

        assertTrue(result.isSuccess)
        assertEquals(2, bodies.size)
        assertTrue(bodies.all { it.contains("preaction=eathotdog") })
        assertTrue(bodies.all { it.contains("whichdog=-92") })
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_drinkSpeakeasy_postsOncePerQueuedQuantity() = runTest {
        setupLuckyLindyQueue(quantity = 2)
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("You gain some stats.")
        })
        val runner = ConcoctionQueueRunner(ClanLoungeRequest(client))

        val result = runner.handleQueue(
            bucket = QueueBucket.BOOZE,
            type = ConcoctionConsumptionType.DRINK,
        )

        assertTrue(result.isSuccess)
        assertEquals(2, bodies.size)
        assertTrue(bodies.all { it.contains("preaction=speakeasydrink") })
        assertTrue(bodies.all { it.contains("drink=4") })
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.BOOZE))
    }

    @Test
    fun handleQueue_clanItemsRestricted_failsBeforeHttp() = runTest {
        setupBasicHotDogQueue(quantity = 1)
        StandardRequest.parseResponse(
            """<b>Clan Items</b><p><span class="i">Clan hot dog stand,</span><p>""",
        )
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("ok")
        })
        val runner = ConcoctionQueueRunner(ClanLoungeRequest(client))
        val state = CharacterState(isHardcore = true, roninLeft = 0)

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
            state = state,
        )

        assertTrue(result.isFailure)
        assertEquals(0, bodies.size)
    }

    @Test
    fun handleQueue_inventoryFood_retrievesThenEats() = runTest {
        val itemId = 88001
        val name = "queue test food"
        setupInventoryFoodQueue(itemId, name, quantity = 1)
        val eatUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            eatUrls += req.url.toString()
            respond("You eat the food.")
        })
        val retrieve = StubRetrieveItemService { id, qty ->
            assertEquals(itemId, id)
            assertEquals(1, qty)
            qty
        }
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            eatFoodRequest = EatFoodRequest(client),
            retrieveItemService = retrieve,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, eatUrls.size)
        assertTrue(eatUrls.single().contains("inv_eat.php"))
        assertTrue(eatUrls.single().contains("whichitem=$itemId"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_inventoryBooze_retrievesThenDrinks() = runTest {
        val itemId = 88002
        val name = "queue test booze"
        setupInventoryBoozeQueue(itemId, name, quantity = 2)
        val drinkUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            drinkUrls += req.url.toString()
            respond("You drink the booze.")
        })
        val retrieve = StubRetrieveItemService { _, qty -> qty }
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            drinkBoozeRequest = DrinkBoozeRequest(client),
            retrieveItemService = retrieve,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.BOOZE,
            type = ConcoctionConsumptionType.DRINK,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, drinkUrls.size)
        assertTrue(drinkUrls.single().contains("inv_booze.php"))
        assertTrue(drinkUrls.single().contains("whichitem=$itemId"))
        assertTrue(drinkUrls.single().contains("quantity=2"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.BOOZE))
    }

    @Test
    fun handleQueue_inventorySpleen_retrievesThenChews() = runTest {
        val itemId = 88020
        val name = "queue test spleen"
        setupInventorySpleenQueue(itemId, name, quantity = 1)
        val chewUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            chewUrls += req.url.toString()
            respond("You chew the spleen item.")
        })
        val retrieve = StubRetrieveItemService { id, qty ->
            assertEquals(itemId, id)
            assertEquals(1, qty)
            qty
        }
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            chewRequest = ChewRequest(client),
            retrieveItemService = retrieve,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.SPLEEN,
            type = ConcoctionConsumptionType.SPLEEN,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, chewUrls.size)
        assertTrue(chewUrls.single().contains("inv_spleen.php"))
        assertTrue(chewUrls.single().contains("whichitem=$itemId"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.SPLEEN))
    }

    @Test
    fun handleQueue_inventorySpleen_quantity2_singleChewCall() = runTest {
        val itemId = 88021
        val name = "queue test spleen batch"
        setupInventorySpleenQueue(itemId, name, quantity = 2)
        val chewUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            chewUrls += req.url.toString()
            respond("You chew the spleen item.")
        })
        val retrieve = StubRetrieveItemService { _, qty -> qty }
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            chewRequest = ChewRequest(client),
            retrieveItemService = retrieve,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.SPLEEN,
            type = ConcoctionConsumptionType.SPLEEN,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, chewUrls.size)
        assertTrue(chewUrls.single().contains("inv_spleen.php"))
        assertTrue(chewUrls.single().contains("whichitem=$itemId"))
        assertTrue(chewUrls.single().contains("quantity=2"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.SPLEEN))
    }

    @Test
    fun handleQueue_insufficientSpleenCapacity_failsBeforeHttp() = runTest {
        val itemId = 88022
        val name = "queue spleen capacity gate"
        setupInventorySpleenQueue(itemId, name, quantity = 1, spleenHit = 2)
        val chewUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            chewUrls += req.url.toString()
            respond("ok")
        })
        val retrieve = StubRetrieveItemService { _, qty -> qty }
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            chewRequest = ChewRequest(client),
            retrieveItemService = retrieve,
        )
        val state = CharacterState(spleenUsed = 14, spleenLimit = 15)

        val result = runner.handleQueue(
            bucket = QueueBucket.SPLEEN,
            type = ConcoctionConsumptionType.SPLEEN,
            state = state,
        )

        assertTrue(result.isFailure)
        assertEquals(0, chewUrls.size)
    }

    @Test
    fun handleQueue_spleenRetrieveShortfall_failsBeforeChewHttp() = runTest {
        val itemId = 88023
        val name = "queue spleen retrieve shortfall"
        setupInventorySpleenQueue(itemId, name, quantity = 2)
        val chewUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            chewUrls += req.url.toString()
            respond("ok")
        })
        val retrieve = StubRetrieveItemService { _, _ -> 1 }
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            chewRequest = ChewRequest(client),
            retrieveItemService = retrieve,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.SPLEEN,
            type = ConcoctionConsumptionType.SPLEEN,
        )

        assertTrue(result.isFailure)
        assertEquals(0, chewUrls.size)
    }

    @Test
    fun handleQueue_retrieveShortfall_failsBeforeEatHttp() = runTest {
        val itemId = 88003
        val name = "queue retrieve shortfall food"
        setupInventoryFoodQueue(itemId, name, quantity = 2)
        val eatUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            eatUrls += req.url.toString()
            respond("ok")
        })
        val retrieve = StubRetrieveItemService { _, _ -> 1 }
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            eatFoodRequest = EatFoodRequest(client),
            retrieveItemService = retrieve,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
        )

        assertTrue(result.isFailure)
        assertEquals(0, eatUrls.size)
    }

    @Test
    fun handleQueue_mixedFifo_foodBeforeHotDog() = runTest {
        val itemId = 88004
        val foodName = "queue fifo food"
        setupInventoryFoodRefresh(itemId, foodName)
        setupBasicHotDogRefresh()
        val refreshContext = ConcoctionRefreshContext(
            characterState = CharacterState(),
            preferences = Preferences(MapSettings()),
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        val context = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        assertTrue(ConcoctionCraftQueue.push(foodName, 1, context))
        assertTrue(ConcoctionCraftQueue.push("basic hot dog", 1, context))

        val requestLog = mutableListOf<Pair<HttpMethod, String>>()
        val client = HttpClient(MockEngine { req ->
            requestLog += req.method to req.url.toString()
            if (req.method == HttpMethod.Post) {
                respond("You gain some stats.")
            } else {
                respond("You eat the food.")
            }
        })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            eatFoodRequest = EatFoodRequest(client),
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
        )

        assertTrue(result.isSuccess)
        assertEquals(2, requestLog.size)
        assertTrue(requestLog[0].second.contains("inv_eat.php"))
        assertTrue(requestLog[1].second.contains("clan_viplounge.php"))
        assertEquals(HttpMethod.Get, requestLog[0].first)
        assertEquals(HttpMethod.Post, requestLog[1].first)
    }

    @Test
    fun handleQueue_craftOnlyCombine_postsCraftPhp() = runTest {
        val resultName = "queue craft only combine"
        val ing1Id = 88101
        val ing2Id = 88102
        setupCraftOnlyCombineQueue(
            resultName = resultName,
            ing1Id = ing1Id,
            ing1Name = "craft ing one",
            ing2Id = ing2Id,
            ing2Name = "craft ing two",
        )
        val craftUrls = mutableListOf<String>()
        val eatUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            val url = req.url.toString()
            if (url.contains("craft.php")) {
                craftUrls += url
                respond("<!-- cr:1x$ing1Id,$ing2Id=999 -->")
            } else {
                eatUrls += url
                respond("ok")
            }
        })
        val retrieve = StubRetrieveItemService { _, qty -> qty }
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = retrieve,
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
        )
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            concoctionCreateRequest = createRequest,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, craftUrls.size)
        assertTrue(craftUrls.single().contains("craft.php"))
        assertEquals(0, eatUrls.size)
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_hellsKitchen_postsCafePhp() = runTest {
        setupCafeOnlyQueue(
            name = "Jumbo Dr. Lucifer",
            type = ConsumableType.FOOD,
            quantity = 1,
            state = CharacterState(zodiacSign = "Bad Moon", meat = 500),
        )
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("You gain some stats.")
        })
        val runner = buildCafeRunner(client)
        val state = CharacterState(zodiacSign = "Bad Moon", meat = 500)

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
            state = state,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, bodies.size)
        assertTrue(bodies.single().contains("cafeid=3"))
        assertTrue(bodies.single().contains("action=CONSUME"))
        assertTrue(bodies.single().contains("whichitem=571"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_chezSnootee_postsCafePhp() = runTest {
        setupCafeOnlyQueue(
            name = "Peche a la Frog",
            type = ConsumableType.FOOD,
            quantity = 2,
            state = CharacterState(zodiacSign = "Blender", meat = 500),
        )
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("You gain some stats.")
        })
        val runner = buildCafeRunner(client)
        val state = CharacterState(zodiacSign = "Blender", meat = 500)

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
            state = state,
        )

        assertTrue(result.isSuccess)
        assertEquals(2, bodies.size)
        assertTrue(bodies.all { it.contains("cafeid=1") })
        assertTrue(bodies.all { it.contains("whichitem=-1") })
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_microBrewery_postsCafePhp() = runTest {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastDesertUnlock", 0)
        setupCafeOnlyQueue(
            name = "Petite Porter",
            type = ConsumableType.DRINK,
            quantity = 1,
            state = CharacterState(zodiacSign = "Wombat", meat = 500, ascensionNumber = 0),
            prefs = prefs,
        )
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("You gain some stats.")
        })
        val runner = buildCafeRunner(client)
        val state = CharacterState(zodiacSign = "Wombat", meat = 500, ascensionNumber = 0)

        val result = runner.handleQueue(
            bucket = QueueBucket.BOOZE,
            type = ConcoctionConsumptionType.DRINK,
            state = state,
            preferences = prefs,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, bodies.size)
        assertTrue(bodies.single().contains("cafeid=2"))
        assertTrue(bodies.single().contains("whichitem=-1"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.BOOZE))
    }

    @Test
    fun handleQueue_cafeInsufficientMeat_failsBeforeHttp() = runTest {
        setupCafeOnlyQueue(
            name = "Peche a la Frog",
            type = ConsumableType.FOOD,
            quantity = 1,
            state = CharacterState(zodiacSign = "Blender", meat = 10),
        )
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("ok")
        })
        val runner = buildCafeRunner(client)
        val state = CharacterState(zodiacSign = "Blender", meat = 10)

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
            state = state,
        )

        assertTrue(result.isFailure)
        assertEquals(0, bodies.size)
    }

    @Test
    fun handleQueue_unknownCafeItem_skipsWithoutHttp() = runTest {
        setupCafeOnlyQueue(
            name = "not a cafe item",
            type = ConsumableType.FOOD,
            quantity = 1,
            state = CharacterState(meat = 500),
        )
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("ok")
        })
        val runner = buildCafeRunner(client)

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
        )

        assertTrue(result.isSuccess)
        assertEquals(0, bodies.size)
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_stillsuitDistillate_postsDistillAndChoice() = runTest {
        setupDistillateQueue(quantity = 1, familiarSweat = 10, stillsuitCount = 1)
        val urls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            urls += req.url.toString()
            respond("ok")
        })
        val prefs = Preferences(MapSettings())
        prefs.setInt("familiarSweat", 10)
        val runner = buildStillSuitRunner(client, stillsuitCount = 1)

        val result = runner.handleQueue(
            bucket = QueueBucket.BOOZE,
            type = ConcoctionConsumptionType.DRINK,
            preferences = prefs,
            state = CharacterState(),
        )

        assertTrue(result.isSuccess)
        assertEquals(2, urls.size)
        assertTrue(urls[0].contains("inventory.php"))
        assertTrue(urls[0].contains("action=distill"))
        assertTrue(urls[1].contains("whichchoice=1476"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.BOOZE))
    }

    @Test
    fun handleQueue_stillsuitInsufficientSweat_failsBeforeHttp() = runTest {
        setupDistillateQueue(quantity = 1, familiarSweat = 5, stillsuitCount = 1)
        val urls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            urls += req.url.toString()
            respond("ok")
        })
        val prefs = Preferences(MapSettings())
        prefs.setInt("familiarSweat", 5)
        val runner = buildStillSuitRunner(client, stillsuitCount = 1)

        val result = runner.handleQueue(
            bucket = QueueBucket.BOOZE,
            type = ConcoctionConsumptionType.DRINK,
            preferences = prefs,
            state = CharacterState(),
        )

        assertTrue(result.isFailure)
        assertEquals(0, urls.size)
    }

    @Test
    fun handleQueue_stillsuitNoSuit_failsBeforeHttp() = runTest {
        setupDistillateQueue(quantity = 1, familiarSweat = 10, stillsuitCount = 0)
        val urls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            urls += req.url.toString()
            respond("ok")
        })
        val prefs = Preferences(MapSettings())
        prefs.setInt("familiarSweat", 10)
        val runner = buildStillSuitRunner(client, stillsuitCount = 0)

        val result = runner.handleQueue(
            bucket = QueueBucket.BOOZE,
            type = ConcoctionConsumptionType.DRINK,
            preferences = prefs,
            state = CharacterState(),
        )

        assertTrue(result.isFailure)
        assertEquals(0, urls.size)
    }

    @Test
    fun handleQueue_stillsuitQuantityTwo_postsTwoDistillPairs() = runTest {
        setupDistillateQueue(quantity = 2, familiarSweat = 10, stillsuitCount = 1)
        val urls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            urls += req.url.toString()
            respond("ok")
        })
        val prefs = Preferences(MapSettings())
        prefs.setInt("familiarSweat", 10)
        val runner = buildStillSuitRunner(client, stillsuitCount = 1)

        val result = runner.handleQueue(
            bucket = QueueBucket.BOOZE,
            type = ConcoctionConsumptionType.DRINK,
            preferences = prefs,
            state = CharacterState(),
        )

        assertTrue(result.isSuccess)
        assertEquals(4, urls.size)
        assertEquals(2, urls.count { it.contains("action=distill") })
        assertEquals(2, urls.count { it.contains("whichchoice=1476") })
    }

    @Test
    fun handleQueue_craftShortfall_failsBeforeEatHttp() = runTest {
        val resultName = "queue craft shortfall food"
        setupCraftOnlyCombineQueue(
            resultName = resultName,
            ing1Id = 88103,
            ing1Name = "shortfall ing one",
            ing2Id = 88104,
            ing2Name = "shortfall ing two",
        )
        val eatUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            eatUrls += req.url.toString()
            respond("You can't craft that.")
        })
        val retrieve = StubRetrieveItemService { _, qty -> qty }
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            concoctionCreateRequest = ConcoctionCreateRequest(
                retrieveItemService = retrieve,
                craftRequest = CraftRequest(client),
                useItemRequest = UseItemRequest(client),
                gameDatabase = null,
            ),
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
        )

        assertTrue(result.isFailure)
        assertEquals(0, eatUrls.count { it.contains("inv_eat.php") })
    }

    @Test
    fun pushHotDog_clanItemsRestricted_returnsFalse() {
        setupBasicHotDogRefresh()
        StandardRequest.parseResponse(
            """<b>Clan Items</b><p><span class="i">Clan hot dog stand,</span><p>""",
        )
        val refreshContext = ConcoctionRefreshContext(
            characterState = CharacterState(isHardcore = true, roninLeft = 0),
            preferences = Preferences(MapSettings()),
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        val context = ConcoctionQueueContext.fromRefreshContext(refreshContext)

        assertFalse(ConcoctionCraftQueue.push("basic hot dog", 1, context))
    }

    private fun setupBasicHotDogRefresh() {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "basic hot dog",
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.CRAPPY,
                advMin = 1,
                advMax = 1,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        HotDogAvailability.addForTest("basic hot dog")
    }

    private fun setupBasicHotDogQueue(quantity: Int) {
        setupBasicHotDogRefresh()
        val refreshContext = ConcoctionRefreshContext(
            characterState = CharacterState(),
            preferences = Preferences(MapSettings()),
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        val context = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        assertTrue(ConcoctionCraftQueue.push("basic hot dog", quantity, context))
    }

    private fun setupLuckyLindyQueue(quantity: Int) {
        registerSpeakeasyDrink(7592, "Lucky Lindy")
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = "Lucky Lindy",
                type = ConsumableType.DRINK,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.AWESOME,
                advMin = 1,
                advMax = 1,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        SpeakeasyAvailability.addLoungeId(4)
        val refreshContext = ConcoctionRefreshContext(
            characterState = CharacterState(meat = 500),
            preferences = Preferences(MapSettings()),
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        val context = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        assertTrue(ConcoctionCraftQueue.push("Lucky Lindy", quantity, context))
    }

    private fun setupInventoryFoodRefresh(itemId: Int, name: String) {
        registerItem(itemId, name)
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = name,
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 1,
                advMax = 1,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        registerCraftTarget(name)
    }

    private fun setupInventoryFoodQueue(itemId: Int, name: String, quantity: Int) {
        setupInventoryFoodRefresh(itemId, name)
        val refreshContext = ConcoctionRefreshContext(
            characterState = CharacterState(),
            preferences = Preferences(MapSettings()),
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        val context = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        assertTrue(ConcoctionCraftQueue.push(name, quantity, context))
    }

    private fun setupInventoryBoozeQueue(itemId: Int, name: String, quantity: Int) {
        registerItem(itemId, name)
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = name,
                type = ConsumableType.DRINK,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 1,
                advMax = 1,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        registerCraftTarget(name)
        val refreshContext = ConcoctionRefreshContext(
            characterState = CharacterState(),
            preferences = Preferences(MapSettings()),
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        val context = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        assertTrue(ConcoctionCraftQueue.push(name, quantity, context))
    }

    private fun setupInventorySpleenQueue(
        itemId: Int,
        name: String,
        quantity: Int,
        spleenHit: Int = 1,
    ) {
        registerItem(itemId, name)
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = name,
                type = ConsumableType.SPLEEN,
                amount = spleenHit,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 1,
                advMax = 1,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        registerCraftTarget(name)
        val refreshContext = ConcoctionRefreshContext(
            characterState = CharacterState(),
            preferences = Preferences(MapSettings()),
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        val context = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        assertTrue(ConcoctionCraftQueue.push(name, quantity, context))
    }

    private fun registerCraftTarget(name: String) {
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = name,
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.setRuntimeForTest(
            name.lowercase(),
            ConcoctionRuntimeState(initial = 1, creatable = 1),
        )
    }

    private fun setupCraftOnlyCombineQueue(
        resultName: String,
        ing1Id: Int,
        ing1Name: String,
        ing2Id: Int,
        ing2Name: String,
        quantity: Int = 1,
    ) {
        registerItem(ing1Id, ing1Name)
        registerItem(ing2Id, ing2Name)
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = resultName,
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 1,
                advMax = 1,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = resultName,
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(
                    ConcoctionIngredient(ing1Name, 1),
                    ConcoctionIngredient(ing2Name, 1),
                ),
            ),
        )
        ConcoctionDatabase.setRuntimeForTest(
            resultName.lowercase(),
            ConcoctionRuntimeState(initial = 0, creatable = 1),
        )
        val refreshContext = ConcoctionRefreshContext(
            characterState = CharacterState(),
            preferences = Preferences(MapSettings()),
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        val context = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        assertTrue(ConcoctionCraftQueue.push(resultName, quantity, context))
    }

    private fun setupCafeOnlyQueue(
        name: String,
        type: ConsumableType,
        quantity: Int,
        state: CharacterState,
        prefs: Preferences = Preferences(MapSettings()),
    ) {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = name,
                type = type,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 1,
                advMax = 1,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = name,
                resultQuantity = 1,
                methods = emptySet(),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.setRuntimeForTest(
            name.lowercase(),
            ConcoctionRuntimeState(initial = 0, creatable = 0),
        )
        val refreshContext = ConcoctionRefreshContext(
            characterState = state,
            preferences = prefs,
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        val context = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        assertTrue(ConcoctionCraftQueue.push(name, quantity, context))
    }

    private fun buildCafeRunner(client: HttpClient): ConcoctionQueueRunner {
        val cafeRequest = CafeRequest(client)
        val hellKitchen = HellKitchenRequest(cafeRequest)
        val chezSnootee = ChezSnooteeRequest(hellKitchen)
        val microBrewery = MicroBreweryRequest(hellKitchen)
        val cafePurchase = CafePurchaseRequest(hellKitchen, chezSnootee, microBrewery)
        return ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            cafePurchaseRequest = cafePurchase,
        )
    }

    private fun buildStillSuitRunner(
        client: HttpClient,
        stillsuitCount: Int,
    ): ConcoctionQueueRunner {
        val stillSuit = TestStillSuitRequest(
            client = client,
            inventoryCountFn = { id ->
                if (id == StillSuitRequest.STILLSUIT_ITEM_ID) stillsuitCount else 0
            },
        )
        return ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            stillSuitRequest = stillSuit,
        )
    }

    private fun setupDistillateQueue(
        quantity: Int,
        familiarSweat: Int,
        stillsuitCount: Int,
    ) {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = StillSuitRequest.DISTILLATE_NAME,
                type = ConsumableType.DRINK,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.CRAPPY,
                advMin = 0,
                advMax = 0,
                muscMin = 0,
                muscMax = 0,
                mystMin = 0,
                mystMax = 0,
                moxieMin = 0,
                moxieMax = 0,
                notes = "",
            ),
        )
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = StillSuitRequest.DISTILLATE_NAME,
                resultQuantity = 1,
                methods = setOf("STILLSUIT"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.setRuntimeForTest(
            StillSuitRequest.DISTILLATE_NAME.lowercase(),
            ConcoctionRuntimeState(initial = 0, creatable = 0),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt("familiarSweat", familiarSweat)
        val refreshContext = ConcoctionRefreshContext(
            characterState = CharacterState(),
            preferences = prefs,
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        val context = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        assertTrue(ConcoctionCraftQueue.push(StillSuitRequest.DISTILLATE_NAME, quantity, context))
    }

    private class TestStillSuitRequest(
        client: HttpClient,
        private val inventoryCountFn: (Int) -> Int,
        private val isEquippedFn: (Int) -> Boolean = { false },
    ) : StillSuitRequest(client) {
        override suspend fun distill(
            name: String,
            type: ConcoctionConsumptionType,
            state: CharacterState?,
            prefs: Preferences?,
            inventoryCountById: (Int) -> Int,
            isEquipped: (Int) -> Boolean,
        ): Result<Unit> = super.distill(
            name = name,
            type = type,
            state = state,
            prefs = prefs,
            inventoryCountById = inventoryCountFn,
            isEquipped = isEquippedFn,
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
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
    }

    private fun registerSpeakeasyDrink(id: Int, name: String) {
        registerItem(id, name)
        DailyLimitDatabase.registerEntryForTest(
            DailyLimitEntry(
                kind = DailyLimitKind.DRINK,
                itemId = id,
                trackingProperty = "_speakeasyDrinksDrunk",
                maxValue = 3,
            ),
        )
    }

    private class StubRetrieveItemService(
        private val retrieveFn: suspend (Int, Int) -> Int,
    ) : RetrieveItemService(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
    ) {
        override suspend fun retrieve(itemId: Int, qty: Int): Int = retrieveFn(itemId, qty)
    }
}
