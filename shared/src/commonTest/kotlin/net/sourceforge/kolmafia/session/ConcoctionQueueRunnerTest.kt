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
import net.sourceforge.kolmafia.data.CafeAccessibility
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
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
import net.sourceforge.kolmafia.data.FloundryAvailability
import net.sourceforge.kolmafia.data.HotDogAvailability
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.data.SpeakeasyAvailability
import net.sourceforge.kolmafia.item.CreateItemIngredients
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CafePurchaseRequest
import net.sourceforge.kolmafia.request.CafeRequest
import net.sourceforge.kolmafia.request.ChewRequest
import net.sourceforge.kolmafia.request.CrimboCafeRequest
import net.sourceforge.kolmafia.request.ChezSnooteeRequest
import net.sourceforge.kolmafia.request.ClipArtCreateRequest
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.FalloutShelterRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.SewerCreateRequest
import net.sourceforge.kolmafia.request.VykeaChoiceMapper
import net.sourceforge.kolmafia.request.VykeaCreateRequest
import net.sourceforge.kolmafia.request.MuseCreateRequest
import net.sourceforge.kolmafia.request.PhineasCreateRequest
import net.sourceforge.kolmafia.request.StaffCreateRequest
import net.sourceforge.kolmafia.request.GnomeTinkerCreateRequest
import net.sourceforge.kolmafia.request.SushiCreateRequest
import net.sourceforge.kolmafia.request.TerminalExtrudeCreateRequest
import net.sourceforge.kolmafia.request.TerminalRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ConcoctionCreateRequest
import net.sourceforge.kolmafia.request.CraftRequest
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import net.sourceforge.kolmafia.request.FloundryRequest
import net.sourceforge.kolmafia.request.HellKitchenRequest
import net.sourceforge.kolmafia.request.MicroBreweryRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.StillSuitRequest
import net.sourceforge.kolmafia.shop.ShopRequest

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
        FloundryAvailability.resetForTest()
        StandardRequest.resetForTest()
        ConsumptionHelperState.resetForTest()
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
    fun handleQueue_inventoryPotion_retrievesThenUses() = runTest {
        val itemId = 88030
        val name = "queue test potion"
        setupInventoryPotionQueue(itemId, name, quantity = 1)
        val useUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            useUrls += req.url.toString()
            respond("You use the potion.")
        })
        val retrieve = StubRetrieveItemService { id, qty ->
            assertEquals(itemId, id)
            assertEquals(1, qty)
            qty
        }
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            useItemRequest = UseItemRequest(client),
            retrieveItemService = retrieve,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.POTION,
            type = ConcoctionConsumptionType.USE,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, useUrls.size)
        assertTrue(useUrls.single().contains("inv_use.php"))
        assertTrue(useUrls.single().contains("whichitem=$itemId"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.POTION))
    }

    @Test
    fun handleQueue_inventoryPotion_quantity2_singleUseCall() = runTest {
        val itemId = 88031
        val name = "queue test potion batch"
        setupInventoryPotionQueue(itemId, name, quantity = 2)
        val useUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            useUrls += req.url.toString()
            respond("You use the potion.")
        })
        val retrieve = StubRetrieveItemService { _, qty -> qty }
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            useItemRequest = UseItemRequest(client),
            retrieveItemService = retrieve,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.POTION,
            type = ConcoctionConsumptionType.USE,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, useUrls.size)
        assertTrue(useUrls.single().contains("quantity=2"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.POTION))
    }

    @Test
    fun handleQueue_robocoreWithoutPotionUpgrade_failsBeforeHttp() = runTest {
        val itemId = 88032
        val name = "queue robocore potion gate"
        setupInventoryPotionQueue(itemId, name, quantity = 1)
        val useUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            useUrls += req.url.toString()
            respond("ok")
        })
        val retrieve = StubRetrieveItemService { _, qty -> qty }
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            useItemRequest = UseItemRequest(client),
            retrieveItemService = retrieve,
        )
        val prefs = Preferences(MapSettings())
        val state = CharacterState(challengePath = AscensionPath.YOU_ROBOT.apiName)

        val result = runner.handleQueue(
            bucket = QueueBucket.POTION,
            type = ConcoctionConsumptionType.USE,
            preferences = prefs,
            state = state,
        )

        assertTrue(result.isFailure)
        assertEquals(0, useUrls.size)
    }

    @Test
    fun handleQueue_crimboCafeFood_purchasesViaCafePhp() = runTest {
        val name = "Peppermint Nutrition Block"
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(CafeAccessibility.CRIMBO_CAFE_AVAILABLE_PREF, true)
        setupCafeOnlyQueue(
            name = name,
            type = ConsumableType.FOOD,
            quantity = 1,
            state = CharacterState(meat = 500),
            prefs = prefs,
        )
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("You gain some stats.")
        })
        val runner = buildCafeRunner(client)

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
            preferences = prefs,
            state = CharacterState(meat = 500),
        )

        assertTrue(result.isSuccess)
        assertEquals(1, bodies.size)
        assertTrue(bodies.single().contains("cafeid=10"))
        assertTrue(bodies.single().contains("whichitem=-104"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_ghostBinge_retrievesThenBinges() = runTest {
        val itemId = 88040
        val name = "queue ghost food"
        setupInventoryFoodQueueWithPrimaryUse(itemId, name, ItemPrimaryUse.FOOD, quantity = 2)
        val bingeUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bingeUrls += req.url.toString()
            respond("The ghost eats.")
        })
        val retrieve = StubRetrieveItemService { id, qty ->
            assertEquals(itemId, id)
            assertEquals(2, qty)
            qty
        }
        val familiarManager = ghostFamiliarManager(client)
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            useItemRequest = UseItemRequest(client),
            retrieveItemService = retrieve,
            familiarManager = familiarManager,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.GLUTTONOUS_GHOST,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, bingeUrls.size)
        assertTrue(bingeUrls.single().contains("familiarbinger.php"))
        assertTrue(bingeUrls.single().contains("whichitem=$itemId"))
        assertTrue(bingeUrls.single().contains("action=binge"))
        assertTrue(bingeUrls.single().contains("qty=2"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_ghostBinge_wrongFamiliar_failsBeforeHttp() = runTest {
        val itemId = 88041
        val name = "queue ghost wrong fam"
        setupInventoryFoodQueueWithPrimaryUse(itemId, name, ItemPrimaryUse.FOOD, quantity = 1)
        val bingeUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bingeUrls += req.url.toString()
            respond("ok")
        })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            useItemRequest = UseItemRequest(client),
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            familiarManager = hoboFamiliarManager(client),
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.GLUTTONOUS_GHOST,
        )

        assertTrue(result.isFailure)
        assertEquals(0, bingeUrls.size)
    }

    @Test
    fun handleQueue_hoboBinge_skipsNonBoozeItems() = runTest {
        val itemId = 88042
        val name = "queue hobo skip food"
        setupInventoryFoodQueueWithPrimaryUse(itemId, name, ItemPrimaryUse.FOOD, quantity = 1)
        val bingeUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bingeUrls += req.url.toString()
            respond("ok")
        })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            useItemRequest = UseItemRequest(client),
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            familiarManager = hoboFamiliarManager(client),
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.BOOZE,
            type = ConcoctionConsumptionType.SPIRIT_HOBO,
        )

        assertTrue(result.isSuccess)
        assertEquals(0, bingeUrls.size)
    }

    @Test
    fun handleQueue_hoboBinge_retrievesThenBinges() = runTest {
        val itemId = 88043
        val name = "queue hobo booze"
        setupInventoryBoozeQueueWithPrimaryUse(itemId, name, ItemPrimaryUse.DRINK, quantity = 1)
        val bingeUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bingeUrls += req.url.toString()
            respond("The hobo drinks.")
        })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            useItemRequest = UseItemRequest(client),
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            familiarManager = hoboFamiliarManager(client),
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.BOOZE,
            type = ConcoctionConsumptionType.SPIRIT_HOBO,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, bingeUrls.size)
        assertTrue(bingeUrls.single().contains("familiarbinger.php"))
        assertTrue(bingeUrls.single().contains("whichitem=$itemId"))
    }

    @Test
    fun handleQueue_slimelingBinge_retrievesThenBinges() = runTest {
        val itemId = 88044
        val name = "queue slimeling weapon"
        setupInventoryFoodQueueWithPrimaryUse(itemId, name, ItemPrimaryUse.WEAPON, quantity = 1)
        val feedUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            feedUrls += req.url.toString()
            respond("The slimeling eats.")
        })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            useItemRequest = UseItemRequest(client),
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            familiarManager = slimelingFamiliarManager(client),
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.SLIMELING,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, feedUrls.size)
        assertTrue(feedUrls.single().contains("familiarbinger.php"))
        assertTrue(feedUrls.single().contains("action=binge"))
    }

    @Test
    fun handleQueue_robortenderRobooze_retrievesThenRoboozes() = runTest {
        val itemId = 88045
        val name = "queue robortender booze"
        setupInventoryBoozeQueueWithPrimaryUse(itemId, name, ItemPrimaryUse.DRINK, quantity = 2)
        val feedUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            feedUrls += req.url.toString()
            respond("The robortender drinks.")
        })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            useItemRequest = UseItemRequest(client),
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            familiarManager = robortenderFamiliarManager(client),
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.BOOZE,
            type = ConcoctionConsumptionType.ROBORTENDER,
        )

        assertTrue(result.isSuccess)
        assertEquals(2, feedUrls.size)
        assertTrue(feedUrls.all { it.contains("inventory.php") })
        assertTrue(feedUrls.all { it.contains("action=robooze") })
        assertTrue(feedUrls.all { it.contains("whichitem=$itemId") })
    }

    @Test
    fun handleQueue_robortenderRobooze_skipsNonBoozeItems() = runTest {
        val itemId = 88046
        val name = "queue robo skip food"
        setupInventoryFoodQueueWithPrimaryUse(itemId, name, ItemPrimaryUse.FOOD, quantity = 1)
        val feedUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            feedUrls += req.url.toString()
            respond("ok")
        })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            useItemRequest = UseItemRequest(client),
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            familiarManager = robortenderFamiliarManager(client),
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.BOOZE,
            type = ConcoctionConsumptionType.ROBORTENDER,
        )

        assertTrue(result.isSuccess)
        assertEquals(0, feedUrls.size)
    }

    @Test
    fun handleQueue_stockingMimicCandy_retrievesThenFeedsCandy() = runTest {
        val itemId = 88047
        val name = "queue mimic candy"
        setupInventoryCandyQueue(itemId, name, quantity = 1)
        val feedUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            feedUrls += req.url.toString()
            respond("The mimic eats candy.")
        })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            useItemRequest = UseItemRequest(client),
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            familiarManager = stockingMimicFamiliarManager(client),
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.POTION,
            type = ConcoctionConsumptionType.STOCKING_MIMIC,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, feedUrls.size)
        assertTrue(feedUrls.single().contains("familiarbinger.php"))
        assertTrue(feedUrls.single().contains("action=candy"))
    }

    @Test
    fun handleQueue_slimelingBinge_wrongFamiliar_failsBeforeHttp() = runTest {
        val itemId = 88048
        val name = "queue slimeling wrong fam"
        setupInventoryFoodQueueWithPrimaryUse(itemId, name, ItemPrimaryUse.WEAPON, quantity = 1)
        val feedUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            feedUrls += req.url.toString()
            respond("ok")
        })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            useItemRequest = UseItemRequest(client),
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            familiarManager = ghostFamiliarManager(client),
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.SLIMELING,
        )

        assertTrue(result.isFailure)
        assertEquals(0, feedUrls.size)
    }

    @Test
    fun handleQueue_floundry_purchasesViaClanLounge() = runTest {
        val name = "carpe"
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(ClanLoungeSync.CLAN_HAS_FLOUNDRY_PREF, true)
        StandardRequest.parseResponse(
            """<b>Clan Items</b><p><span class="i">Clan Floundry,</span><p>""",
        )
        setupFloundryQueue(name, quantity = 1, prefs = prefs)
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("You acquire an item: carpe.")
        })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            floundryRequest = FloundryRequest(client),
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
            preferences = prefs,
            state = CharacterState(),
        )

        assertTrue(result.isSuccess)
        assertEquals(1, bodies.size)
        assertTrue(bodies.single().contains("preaction=buyfloundryitem"))
        assertTrue(bodies.single().contains("whichitem=9001"))
        assertTrue(prefs.getBoolean(FloundryRequest.FLOUNDRY_ITEM_CREATED_PREF, false))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_floundryUnavailable_failsBeforeHttp() = runTest {
        val name = "carpe"
        val prefs = Preferences(MapSettings())
        setupFloundryQueue(name, quantity = 1, prefs = prefs)
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("ok")
        })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            floundryRequest = FloundryRequest(client),
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
            preferences = prefs,
            state = CharacterState(),
        )

        assertTrue(result.isFailure)
        assertEquals(0, bodies.size)
    }

    @Test
    fun handleQueue_eatFailure_requeuesWhenAddCreationQueueTrue() = runTest {
        val itemId = 88044
        val name = "queue requeue food"
        setupInventoryFoodQueueWithPrimaryUse(itemId, name, ItemPrimaryUse.FOOD, quantity = 1)
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("addCreationQueue", true)
        val client = HttpClient(MockEngine { respond("error", status = io.ktor.http.HttpStatusCode.InternalServerError) })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            eatFoodRequest = EatFoodRequest(client),
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
            preferences = prefs,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_eatFailure_noRequeueWhenAddCreationQueueFalse() = runTest {
        val itemId = 88045
        val name = "queue no requeue food"
        setupInventoryFoodQueueWithPrimaryUse(itemId, name, ItemPrimaryUse.FOOD, quantity = 1)
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("addCreationQueue", false)
        val client = HttpClient(MockEngine { respond("error", status = io.ktor.http.HttpStatusCode.InternalServerError) })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            eatFoodRequest = EatFoodRequest(client),
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
            preferences = prefs,
        )

        assertTrue(result.isFailure)
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_eatAbort_partialRequeueRestoresRemainingQuantity() = runTest {
        val itemId = 2338
        val name = "black pudding partial requeue"
        setupInventoryFoodQueueWithPrimaryUse(itemId, name, ItemPrimaryUse.FOOD, quantity = 3)
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("addCreationQueue", true)
        var calls = 0
        val client = HttpClient(MockEngine {
            calls++
            if (calls == 1) {
                respond("You eat the pudding.")
            } else {
                respond("You're too full to eat that.")
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
            preferences = prefs,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
        assertEquals(2, ConcoctionCraftQueue.entries(QueueBucket.FOOD).single().quantity)
    }

    @Test
    fun handleQueue_foodHelper_queuesWithoutEatHttp() = runTest {
        val helperId = 5459
        val helperName = "fudge spork queue helper"
        setupInventoryFoodQueueWithPrimaryUse(helperId, helperName, ItemPrimaryUse.FOOD_HELPER, quantity = 1)
        val eatUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            eatUrls += req.url.toString()
            respond("ok")
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
        assertEquals(0, eatUrls.size)
        assertEquals(5459 to 1, ConsumptionHelperState.currentFoodHelper())
    }

    @Test
    fun handleQueue_eatFailure_requeuesHelperBeforePartialFood() = runTest {
        val helperId = 5459
        val helperName = "fudge spork requeue helper"
        val foodId = 88046
        val foodName = "requeue helper food"
        setupInventoryFoodQueueWithPrimaryUse(helperId, helperName, ItemPrimaryUse.FOOD_HELPER, quantity = 1)
        setupInventoryFoodQueueWithPrimaryUse(foodId, foodName, ItemPrimaryUse.FOOD, quantity = 2)
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("addCreationQueue", true)
        val client = HttpClient(MockEngine { respond("error", status = io.ktor.http.HttpStatusCode.InternalServerError) })
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            eatFoodRequest = EatFoodRequest(client),
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
            preferences = prefs,
        )

        assertTrue(result.isSuccess)
        assertEquals(2, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
        val entries = ConcoctionCraftQueue.entries(QueueBucket.FOOD)
        assertEquals(helperName, entries[0].resultName)
        assertEquals(1, entries[0].quantity)
        assertEquals(foodName, entries[1].resultName)
        assertEquals(2, entries[1].quantity)
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
            createItemIngredients = CreateItemIngredients(retrieve, null),
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
    fun handleQueue_noneInventory_retrievesWithoutEating() = runTest {
        val itemId = 88051
        val name = "queue create only food"
        setupInventoryFoodQueue(itemId, name, quantity = 1)
        val httpUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            httpUrls += req.url.toString()
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
            type = ConcoctionConsumptionType.NONE,
        )

        assertTrue(result.isSuccess)
        assertEquals(0, httpUrls.size)
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_noneCraftOnly_craftsWithoutEating() = runTest {
        val resultName = "queue create only combine"
        val ing1Id = 88151
        val ing2Id = 88152
        setupCraftOnlyCombineQueue(
            resultName = resultName,
            ing1Id = ing1Id,
            ing1Name = "create ing one",
            ing2Id = ing2Id,
            ing2Name = "create ing two",
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
            createItemIngredients = CreateItemIngredients(retrieve, null),
        )
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            concoctionCreateRequest = createRequest,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.NONE,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, craftUrls.size)
        assertEquals(0, eatUrls.size)
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_noneStillCraftOnly_postsStillShopBuy() = runTest {
        val resultName = "queue still craft booze"
        val ginId = 88171
        setupStillCraftQueue(
            resultName = resultName,
            ginId = ginId,
            ginName = "queue still gin",
            row = 267,
        )
        val shopBodies = mutableListOf<String>()
        val craftUrls = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            val url = req.url.toString()
            if (url.contains("shop.php") || req.body.toByteArray().isNotEmpty()) {
                shopBodies += req.body.toByteArray().decodeToString()
                respond("You acquire an item: <b>$resultName</b>")
            } else if (url.contains("craft.php")) {
                craftUrls += url
                respond("ok")
            } else {
                respond("ok")
            }
        })
        val retrieve = StubRetrieveItemService { id, qty ->
            assertEquals(ginId, id)
            qty
        }
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = retrieve,
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            createItemIngredients = CreateItemIngredients(retrieve, null),
            shopRequest = ShopRequest(client),
        )
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            concoctionCreateRequest = createRequest,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.BOOZE,
            type = ConcoctionConsumptionType.NONE,
            state = CharacterState(stillsAvailable = 1),
        )

        assertTrue(result.isSuccess)
        assertEquals(1, shopBodies.size)
        assertTrue(shopBodies.single().contains("whichshop=still"))
        assertTrue(shopBodies.single().contains("whichrow=267"))
        assertEquals(0, craftUrls.size)
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.BOOZE))
    }

    @Test
    fun handleQueue_noneClipArtCraftOnly_postsCampgroundCombine() = runTest {
        val resultName = "queue clip art donut"
        setupClipArtCraftQueue(resultName = resultName, param = 0x010101)
        val campgroundBodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            if (req.body.toByteArray().isNotEmpty()) {
                campgroundBodies += req.body.toByteArray().decodeToString()
            }
            respond("You acquire an item: <b>$resultName</b>")
        })
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            clipArtCreateRequest = ClipArtCreateRequest(client),
        )
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            concoctionCreateRequest = createRequest,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.NONE,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, campgroundBodies.size)
        assertTrue(campgroundBodies.single().contains("preaction=combinecliparts"))
        assertTrue(campgroundBodies.single().contains("clip1=1"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_noneTerminalCraftOnly_postsTerminalExtrude() = runTest {
        val resultName = "browser cookie"
        setupTerminalCraftQueue(resultName = resultName)
        val requestBodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            if (req.body.toByteArray().isNotEmpty()) {
                requestBodies += req.body.toByteArray().decodeToString()
            }
            respond("You acquire an item: <b>$resultName</b>")
        })
        val terminalRequest = TerminalRequest(
            client = client,
            campgroundRequest = CampgroundRequest(client),
            falloutShelterRequest = FalloutShelterRequest(client),
        )
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
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_campgroundHasSourceTerminal", true)
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            concoctionCreateRequest = createRequest,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.NONE,
            preferences = prefs,
            state = CharacterState(),
        )

        assertTrue(result.isSuccess)
        assertTrue(requestBodies.any { it.contains("action=terminal") })
        assertTrue(requestBodies.any { it.contains("whichchoice=1191") })
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_noneSewerCraftOnly_usesChewingGum() = runTest {
        val resultName = "seal-skull helmet"
        setupSewerCraftQueue(resultName = resultName)
        val usedItems = mutableListOf<Int>()
        val client = HttpClient(MockEngine { req ->
            usedItems += req.url.parameters["whichitem"]?.toInt() ?: -1
            respond("You acquire an item: <b>$resultName</b>")
        })
        val inventory = mutableMapOf<Int, Int>()
        val sewerDb = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) =
                if (name == resultName) {
                    ItemData(
                        id = 2283,
                        name = resultName,
                        descId = "d2283",
                        image = "img",
                        primaryUse = ItemPrimaryUse.NONE,
                        secondaryUses = emptySet(),
                        access = setOf('t', 'd'),
                        autosellPrice = 1,
                        plural = null,
                    )
                } else {
                    null
                }
        }
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = sewerDb,
            sewerCreateRequest = SewerCreateRequest(
                useItemRequest = UseItemRequest(client),
                closetRequest = ClosetRequest(client),
                createItemIngredients = CreateItemIngredients(
                    StubRetrieveItemService { _, qty -> qty },
                    gameDatabase = sewerDb,
                ),
                gameDatabase = sewerDb,
                inventoryCountById = { id -> inventory[id] ?: 0 },
            ),
        )
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            concoctionCreateRequest = createRequest,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.NONE,
            state = CharacterState(),
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf(23), usedItems)
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_noneVykeaCraftOnly_postsChoiceChain() = runTest {
        val resultName = "level 1 bookshelf"
        setupVykeaCraftQueue(resultName = resultName)
        val choicePosts = mutableListOf<String>()
        var choiceStep = 0
        val choiceSequence = listOf(1121, 1122, 1123)
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.toString().contains("inv_use.php") -> {
                    respond("""<input name="whichchoice" value="1120">""")
                }
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("choice.php") -> {
                    choicePosts += request.body.toByteArray().decodeToString()
                    val nextChoice = choiceSequence.getOrElse(choiceStep) { 0 }
                    if (choiceStep < choiceSequence.size) choiceStep++
                    respond("""<input name="whichchoice" value="$nextChoice">""")
                }
                else -> respond("ok")
            }
        })
        ItemDatabase.registerForTest(
            ItemData(
                id = VykeaChoiceMapper.INSTRUCTIONS_ID,
                name = "VYKEA instructions",
                descId = "d8730",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = VykeaChoiceMapper.HEX_KEY_ID,
                name = "VYKEA hex key",
                descId = "d8729",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = VykeaChoiceMapper.PLANK_ID,
                name = "VYKEA plank",
                descId = "d8725",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = VykeaChoiceMapper.RAIL_ID,
                name = "VYKEA rail",
                descId = "d8726",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = VykeaChoiceMapper.BRACKET_ID,
                name = "VYKEA bracket",
                descId = "d8727",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
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
                accessibleCount = { id ->
                    if (id == VykeaChoiceMapper.HEX_KEY_ID) 1 else 0
                },
            ),
        )
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            concoctionCreateRequest = createRequest,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.NONE,
            state = CharacterState(),
        )

        assertTrue(result.isSuccess)
        assertEquals(4, choicePosts.size)
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_noneMuseCraftOnly_postsMultiUse() = runTest {
        val resultName = "pottery yo-yo"
        setupMuseCraftQueue(resultName = resultName)
        val multiUsePosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("multiuse.php") -> {
                    multiUsePosts += request.body.toByteArray().decodeToString()
                    respond("You acquire an item: <b>$resultName</b>")
                }
                else -> respond("ok")
            }
        })
        ItemDatabase.registerForTest(
            ItemData(
                id = 88021,
                name = "smoked potsherd",
                descId = "d88021",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
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
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            concoctionCreateRequest = createRequest,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.NONE,
            state = CharacterState(),
        )

        assertTrue(result.isSuccess)
        assertEquals(1, multiUsePosts.size)
        assertTrue(multiUsePosts.single().contains("whichitem=88021"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_nonePhineasCraftOnly_postsVolcanoisland() = runTest {
        val resultName = "sealhide hood"
        setupPhineasCraftQueue(resultName = resultName)
        val phineasPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("volcanoisland.php") -> {
                    phineasPosts += request.body.toByteArray().decodeToString()
                    respond("You acquire an item: <b>$resultName</b>")
                }
                else -> respond("ok")
            }
        })
        registerPhineasIngredients()
        val gameDatabase = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = if (name == resultName) {
                net.sourceforge.kolmafia.data.ItemData(
                    id = 88111,
                    name = resultName,
                    descId = "d88111",
                    image = "img",
                    primaryUse = ItemPrimaryUse.NONE,
                    secondaryUses = emptySet(),
                    access = setOf('t', 'd'),
                    autosellPrice = 100,
                    plural = null,
                )
            } else {
                null
            }
        }
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = gameDatabase,
            phineasCreateRequest = PhineasCreateRequest(
                client = client,
                createItemIngredients = CreateItemIngredients(
                    StubRetrieveItemService { _, qty -> qty },
                    gameDatabase,
                ),
                gameDatabase = gameDatabase,
            ),
        )
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            concoctionCreateRequest = createRequest,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.NONE,
            state = null,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, phineasPosts.size)
        assertTrue(phineasPosts.single().contains("makewhich=88111"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_noneStaffCraftOnly_postsGuild() = runTest {
        val resultName = "Staff of the Teapot Tempest"
        setupStaffCraftQueue(resultName = resultName)
        val staffPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("guild.php") -> {
                    staffPosts += request.body.toByteArray().decodeToString()
                    respond("You acquire an item: <b>$resultName</b>")
                }
                else -> respond("ok")
            }
        })
        registerStaffIngredients()
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            staffCreateRequest = StaffCreateRequest(
                client = client,
                createItemIngredients = CreateItemIngredients(
                    StubRetrieveItemService { _, qty -> qty },
                    null,
                ),
                gameDatabase = null,
            ),
        )
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            concoctionCreateRequest = createRequest,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.NONE,
            state = null,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, staffPosts.size)
        assertTrue(staffPosts.single().contains("whichstaff=88201"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_noneTinkerCraftOnly_postsGnomes() = runTest {
        val resultName = "clockwork widget"
        setupTinkerCraftQueue(resultName = resultName)
        val tinkerPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("gnomes.php") -> {
                    tinkerPosts += request.body.toByteArray().decodeToString()
                    respond("Gnorman deftly assembles your items into something new.")
                }
                else -> respond("ok")
            }
        })
        registerTinkerIngredients()
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            gnomeTinkerCreateRequest = GnomeTinkerCreateRequest(
                client = client,
                createItemIngredients = CreateItemIngredients(
                    StubRetrieveItemService { _, qty -> qty },
                    null,
                ),
                gameDatabase = null,
            ),
        )
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            concoctionCreateRequest = createRequest,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.NONE,
            state = null,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, tinkerPosts.size)
        assertTrue(tinkerPosts.single().contains("item1=88301"))
        assertEquals(0, ConcoctionCraftQueue.depth(QueueBucket.FOOD))
    }

    @Test
    fun handleQueue_eatSushiCraftOnly_postsSushiPhpNoFollowUpEat() = runTest {
        val resultName = "beefy nigiri"
        setupSushiCraftQueue(resultName = resultName)
        val sushiPosts = mutableListOf<String>()
        val eatPosts = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            when {
                request.method == HttpMethod.Post && request.url.encodedPath.endsWith("sushi.php") -> {
                    sushiPosts += request.body.toByteArray().decodeToString()
                    respond("You eat the beefy nigiri. Delicious!")
                }
                request.url.toString().contains("eatfood") ||
                    request.url.toString().contains("inv_use.php") -> {
                    eatPosts += request.url.toString()
                    respond("You gain some stats.")
                }
                else -> respond("ok")
            }
        })
        registerSushiIngredients()
        val createRequest = ConcoctionCreateRequest(
            retrieveItemService = StubRetrieveItemService { _, qty -> qty },
            craftRequest = CraftRequest(client),
            useItemRequest = UseItemRequest(client),
            gameDatabase = null,
            sushiCreateRequest = SushiCreateRequest(
                client = client,
                createItemIngredients = CreateItemIngredients(
                    StubRetrieveItemService { _, qty -> qty },
                    null,
                ),
                gameDatabase = null,
            ),
        )
        val runner = ConcoctionQueueRunner(
            clanLoungeRequest = ClanLoungeRequest(client),
            eatFoodRequest = EatFoodRequest(client),
            concoctionCreateRequest = createRequest,
        )

        val result = runner.handleQueue(
            bucket = QueueBucket.FOOD,
            type = ConcoctionConsumptionType.EAT,
            state = null,
        )

        assertTrue(result.isSuccess)
        assertEquals(1, sushiPosts.size)
        assertTrue(sushiPosts.single().contains("whichsushi=1"))
        assertEquals(0, eatPosts.size)
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
                createItemIngredients = CreateItemIngredients(retrieve, null),
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

    private fun setupStillCraftQueue(
        resultName: String,
        ginId: Int,
        ginName: String,
        row: Int,
        quantity: Int = 1,
    ) {
        registerItem(ginId, ginName)
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = resultName,
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
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = resultName,
                resultQuantity = 1,
                methods = setOf("STILL", "ROW$row"),
                ingredients = listOf(ConcoctionIngredient(ginName, 1)),
            ),
        )
        ConcoctionDatabase.setRuntimeForTest(
            resultName.lowercase(),
            ConcoctionRuntimeState(initial = 0, creatable = 1),
        )
        val refreshContext = ConcoctionRefreshContext(
            characterState = CharacterState(stillsAvailable = 1),
            preferences = Preferences(MapSettings()),
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        val context = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        assertTrue(ConcoctionCraftQueue.push(resultName, quantity, context))
    }

    private fun setupClipArtCraftQueue(
        resultName: String,
        param: Int,
        quantity: Int = 1,
    ) {
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
                methods = setOf("CLIPART"),
                ingredients = emptyList(),
                param = param,
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

    private fun setupTerminalCraftQueue(
        resultName: String,
        quantity: Int = 1,
    ) {
        ItemDatabase.registerForTest(
            ItemData(
                id = 9034,
                name = "Source essence",
                descId = "d9034",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
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
                methods = setOf("TERMINAL"),
                ingredients = listOf(ConcoctionIngredient("Source essence", 10)),
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

    private fun setupSewerCraftQueue(
        resultName: String = "seal-skull helmet",
        quantity: Int = 1,
    ) {
        ItemDatabase.registerForTest(
            ItemData(
                id = 23,
                name = "chewing gum on a string",
                descId = "d23",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 25,
                plural = null,
            ),
        )
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
                methods = setOf("SEWER"),
                ingredients = listOf(ConcoctionIngredient("chewing gum on a string", 1)),
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

    private fun setupVykeaCraftQueue(
        resultName: String = "level 1 bookshelf",
        quantity: Int = 1,
    ) {
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
                methods = setOf("VYKEA"),
                ingredients = listOf(
                    ConcoctionIngredient("VYKEA instructions", 1),
                    ConcoctionIngredient("VYKEA plank", 5),
                    ConcoctionIngredient("VYKEA plank", 5),
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

    private fun setupMuseCraftQueue(
        resultName: String = "pottery yo-yo",
        quantity: Int = 1,
    ) {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = resultName,
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
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
                result = resultName,
                resultQuantity = 1,
                methods = setOf("MUSE"),
                ingredients = listOf(ConcoctionIngredient("smoked potsherd", 5)),
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

    private fun setupPhineasCraftQueue(
        resultName: String = "sealhide hood",
        quantity: Int = 1,
    ) {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = resultName,
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
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
                result = resultName,
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

    private fun registerPhineasIngredients() {
        listOf(
            88112 to "hellseal brain",
            88113 to "hellseal sinew",
            88114 to "hellseal hide",
            88115 to "hellseal whisker",
            88116 to "hellseal claw",
        ).forEach { (id, name) ->
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

    private fun setupStaffCraftQueue(
        resultName: String = "Staff of the Teapot Tempest",
        quantity: Int = 1,
    ) {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = resultName,
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
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
                result = resultName,
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

    private fun registerStaffIngredients() {
        listOf(
            88201 to "big stirring stick",
            88202 to "menudo",
            88203 to "sangria",
            88204 to "hippy herbal tea",
            88205 to "concentrated magicalness pill",
            88206 to "magical mystery juice (3)",
        ).forEach { (id, name) ->
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

    private fun setupSushiCraftQueue(
        resultName: String = "beefy nigiri",
        quantity: Int = 1,
    ) {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = resultName,
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
                advMin = 4,
                advMax = 8,
                muscMin = 8,
                muscMax = 16,
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
                methods = setOf("SUSHI"),
                ingredients = listOf(
                    ConcoctionIngredient("beefy fish meat", 1),
                    ConcoctionIngredient("white rice", 1),
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

    private fun registerSushiIngredients() {
        listOf(
            89101 to "beefy fish meat",
            89102 to "white rice",
        ).forEach { (id, name) ->
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

    private fun setupTinkerCraftQueue(
        resultName: String = "clockwork widget",
        quantity: Int = 1,
    ) {
        ConsumableDatabase.injectForTest(
            ConsumableData(
                name = resultName,
                type = ConsumableType.FOOD,
                amount = 1,
                levelReq = 1,
                quality = ConsumableQuality.DECENT,
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
                result = resultName,
                resultQuantity = 1,
                methods = setOf("TINKER"),
                ingredients = listOf(
                    ConcoctionIngredient("flange", 1),
                    ConcoctionIngredient("cog", 1),
                    ConcoctionIngredient("sprocket", 1),
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

    private fun registerTinkerIngredients() {
        listOf(
            88301 to "flange",
            88302 to "cog",
            88303 to "sprocket",
        ).forEach { (id, name) ->
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

    private fun setupInventoryPotionQueue(
        itemId: Int,
        name: String,
        quantity: Int,
    ) {
        ItemDatabase.registerForTest(
            ItemData(
                id = itemId,
                name = name,
                descId = "d$itemId",
                image = "img",
                primaryUse = ItemPrimaryUse.POTION,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
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

    private fun buildCafeRunner(client: HttpClient): ConcoctionQueueRunner {
        val cafeRequest = CafeRequest(client)
        val hellKitchen = HellKitchenRequest(cafeRequest)
        val chezSnootee = ChezSnooteeRequest(hellKitchen)
        val microBrewery = MicroBreweryRequest(hellKitchen)
        val crimboCafe = CrimboCafeRequest(cafeRequest)
        val cafePurchase = CafePurchaseRequest(hellKitchen, chezSnootee, microBrewery, crimboCafe)
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

    private fun registerItem(id: Int, name: String, primaryUse: ItemPrimaryUse = ItemPrimaryUse.NONE) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = primaryUse,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
            ),
        )
    }

    private fun setupInventoryFoodQueueWithPrimaryUse(
        itemId: Int,
        name: String,
        primaryUse: ItemPrimaryUse,
        quantity: Int,
    ) {
        registerItem(itemId, name, primaryUse)
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
        val refreshContext = ConcoctionRefreshContext(
            characterState = CharacterState(),
            preferences = Preferences(MapSettings()),
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        val context = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        assertTrue(ConcoctionCraftQueue.push(name, quantity, context))
    }

    private fun setupInventoryBoozeQueueWithPrimaryUse(
        itemId: Int,
        name: String,
        primaryUse: ItemPrimaryUse,
        quantity: Int,
    ) {
        registerItem(itemId, name, primaryUse)
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

    private fun setupFloundryQueue(name: String, quantity: Int, prefs: Preferences) {
        FloundryAvailability.addForTest(name, 100)
        registerItem(9001, name)
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
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = name,
                resultQuantity = 1,
                methods = setOf("FLOUNDRY"),
                ingredients = emptyList(),
            ),
        )
        ConcoctionDatabase.setRuntimeForTest(
            name.lowercase(),
            ConcoctionRuntimeState(initial = 0, creatable = 1),
        )
        val refreshContext = ConcoctionRefreshContext(
            characterState = CharacterState(),
            preferences = prefs,
        )
        ConcoctionDatabase.refreshConcoctionsNow(refreshContext)
        val context = ConcoctionQueueContext.fromRefreshContext(refreshContext)
        assertTrue(ConcoctionCraftQueue.push(name, quantity, context))
    }

    private fun ghostFamiliarManager(client: HttpClient): FamiliarManager {
        val fm = FamiliarManager(client, GameEventBus())
        fm.testSetState(
            FamiliarState(
                activeFamiliar = FamiliarData(
                    id = 74,
                    name = "Gluttonous Green Ghost",
                    race = "ghost",
                    weight = 1,
                    experience = 0,
                    kills = 0,
                ),
            ),
        )
        return fm
    }

    private fun hoboFamiliarManager(client: HttpClient): FamiliarManager {
        val fm = FamiliarManager(client, GameEventBus())
        fm.testSetState(
            FamiliarState(
                activeFamiliar = FamiliarData(
                    id = 52,
                    name = "Spirit Hobo",
                    race = "hobo",
                    weight = 1,
                    experience = 0,
                    kills = 0,
                ),
            ),
        )
        return fm
    }

    private fun slimelingFamiliarManager(client: HttpClient): FamiliarManager {
        val fm = FamiliarManager(client, GameEventBus())
        fm.testSetState(
            FamiliarState(
                activeFamiliar = FamiliarData(
                    id = 112,
                    name = "Slimeling",
                    race = "slimeling",
                    weight = 1,
                    experience = 0,
                    kills = 0,
                ),
            ),
        )
        return fm
    }

    private fun robortenderFamiliarManager(client: HttpClient): FamiliarManager {
        val fm = FamiliarManager(client, GameEventBus())
        fm.testSetState(
            FamiliarState(
                activeFamiliar = FamiliarData(
                    id = 211,
                    name = "Robortender",
                    race = "robortender",
                    weight = 1,
                    experience = 0,
                    kills = 0,
                ),
            ),
        )
        return fm
    }

    private fun stockingMimicFamiliarManager(client: HttpClient): FamiliarManager {
        val fm = FamiliarManager(client, GameEventBus())
        fm.testSetState(
            FamiliarState(
                activeFamiliar = FamiliarData(
                    id = 120,
                    name = "Stocking Mimic",
                    race = "stocking mimic",
                    weight = 1,
                    experience = 0,
                    kills = 0,
                ),
            ),
        )
        return fm
    }

    private fun setupInventoryCandyQueue(itemId: Int, name: String, quantity: Int) {
        ItemDatabase.registerForTest(
            ItemData(
                id = itemId,
                name = name,
                descId = "d$itemId",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = setOf("candy1"),
                access = setOf('t', 'd'),
                autosellPrice = 100,
                plural = null,
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

    private fun registerItem(id: Int, name: String) {
        registerItem(id, name, ItemPrimaryUse.NONE)
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
