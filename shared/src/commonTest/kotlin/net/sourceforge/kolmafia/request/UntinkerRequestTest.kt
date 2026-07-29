package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.AdventureRequest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.adventure.FightRequest
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.AdventureSpentTracker
import net.sourceforge.kolmafia.session.DreadKissesTracker
import net.sourceforge.kolmafia.session.GoalManager

class UntinkerRequestTest {

    @AfterTest
    fun tearDown() {
        UntinkerRequest.resetForTest()
        ConcoctionDatabase.resetForTest()
        ItemDatabase.resetForTest()
        AdventureDatabase.resetForTest()
    }

    @Test
    fun completeQuest_knollPath_postsScrewquestAndInnabox() = runTest {
        val urls = mutableListOf<String>()
        val engine = MockEngine { request ->
            val payload = requestPayload(request)
            urls += payload
            when {
                payload.contains("preaction=screwquest") ->
                    respond(
                        "I'm just lost without my screwdriver",
                        HttpStatusCode.OK,
                    )
                payload.contains("dk_innabox") ->
                    respond("ok", HttpStatusCode.OK)
                payload.contains("action=fv_untinker") ->
                    respond("<select name=whichitem></select>", HttpStatusCode.OK)
                else -> respond("ok", HttpStatusCode.OK)
            }
        }

        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(sign = "Mongoose"))

        val prefs = Preferences(MapSettings())
        val questDb = QuestDatabase(prefs)

        val request = UntinkerRequest(
            client = HttpClient(engine),
            character = char,
            questDatabase = questDb,
        )

        assertTrue(request.completeQuest())
        assertTrue(urls.any { it.contains("preaction=screwquest") || it.contains("screwquest") })
        assertTrue(urls.any { it.contains("dk_innabox") })
        assertEquals(QuestDatabase.STARTED, questDb.getProgress(Quest.UNTINKER))
    }

    @Test
    fun completeQuest_plainsPath_returnsFalseWithoutSideTripManagers() = runTest {
        val engine = MockEngine { respond("ok", HttpStatusCode.OK) }
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(sign = "Seal", adventures = "5"))

        val request = UntinkerRequest(
            client = HttpClient(engine),
            character = char,
        )

        assertEquals(false, request.completeQuest())
    }

    @Test
    fun runSideTripForItem_obtainsItemAndRestoresGoals() = runTest {
        AdventureDatabase.injectForTest(
            AdventureZone(
                zoneName = "Degrassi Knoll",
                urlParams = "adventure=354",
                locationName = "The Degrassi Knoll Garage",
                environment = "indoor",
                diffLevel = "low",
                statRequirement = 10,
                goals = emptyList(),
                isOverdrunk = false,
                noWander = false,
            ),
        )

        lateinit var inventory: MutableInventoryManager
        val adventureEngine = MockEngine {
            inventory.addItem(
                UntinkerRequest.RUSTY_SCREWDRIVER,
                "rusty screwdriver",
                1,
            )
            respond("You acquire an item: <b>rusty screwdriver</b>", HttpStatusCode.OK)
        }

        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(sign = "Seal", adventures = "5", buffedmus = "15"),
        )

        inventory = MutableInventoryManager()
        val prefs = Preferences(MapSettings())
        val goalManager = GoalManager()
        goalManager.addItemGoal(999)
        goalManager.setMeatGoal(500)

        val adventureManager = AdventureManager(
            adventureRequest = AdventureRequest(HttpClient(adventureEngine)),
            fightRequest = FightRequest(HttpClient(adventureEngine)),
            choiceRequest = ChoiceRequest(HttpClient(adventureEngine)),
            characterRequest = net.sourceforge.kolmafia.request.CharacterRequest(
                HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }),
            ),
            character = char,
            preferences = prefs,
            eventBus = GameEventBus(),
            goalManager = goalManager,
            inventory = inventory,
            adventureSpentTracker = AdventureSpentTracker(prefs),
            dreadKissesTracker = DreadKissesTracker(prefs),
        )

        val location = AdventureLocation("354", "The Degrassi Knoll Garage", "Degrassi Knoll")
        val obtained = goalManager.runSideTripForItem(
            adventureManager = adventureManager,
            location = location,
            itemId = UntinkerRequest.RUSTY_SCREWDRIVER,
            maxTurns = 5,
            scope = this,
            itemCount = { inventory.state.value.items[UntinkerRequest.RUSTY_SCREWDRIVER]?.quantity ?: 0 },
        )

        assertTrue(obtained)
        assertEquals(1, inventory.state.value.items[UntinkerRequest.RUSTY_SCREWDRIVER]?.quantity)
        assertTrue(goalManager.hasItemGoal(999))
        assertTrue(goalManager.hasMeatGoalSet())
    }

    @Test
    fun untinkerViaLegionScrewdriver_postsScrewActionWithUntinkerAll() = runTest {
        registerUntinkerItem(MEAT_PASTE_ITEM, "meat paste item")
        registerItem(MEAT_PASTE_ITEM, "meat paste item")
        registerItem(UntinkerRequest.LOATHING_LEGION_SCREWDRIVER, "Loathing Legion universal screwdriver")

        val urls = mutableListOf<String>()
        val engine = MockEngine { request ->
            urls += request.url.toString()
            respond("You acquire an item: meat paste", HttpStatusCode.OK)
        }

        val inventory = MutableInventoryManager(
            mapOf(
                MEAT_PASTE_ITEM to InventoryItem(MEAT_PASTE_ITEM, "meat paste item", 6, ItemType.OTHER),
                UntinkerRequest.LOATHING_LEGION_SCREWDRIVER to InventoryItem(
                    UntinkerRequest.LOATHING_LEGION_SCREWDRIVER,
                    "Loathing Legion universal screwdriver",
                    1,
                    ItemType.OTHER,
                ),
            ),
        )

        val request = UntinkerRequest(
            client = HttpClient(engine),
            inventoryManager = inventory,
            gameDatabase = GameDatabase(),
        )

        val result = request.untinkerViaLegionScrewdriver(MEAT_PASTE_ITEM, 6)
        assertTrue(result.isSuccess)
        assertEquals(6, result.getOrNull())
        assertEquals(1, urls.size)
        assertTrue(urls[0].contains("action=screw"))
        assertTrue(urls[0].contains("whichitem=${UntinkerRequest.LOATHING_LEGION_SCREWDRIVER}"))
        assertTrue(urls[0].contains("dowhichitem=$MEAT_PASTE_ITEM"))
        assertTrue(urls[0].contains("untinkerall=on"))
    }

    @Test
    fun untinker_retriesAfterCompleteQuestWhenProbeLacksSelect() = runTest {
        registerUntinkerItem(MEAT_PASTE_ITEM, "meat paste item")
        registerItem(MEAT_PASTE_ITEM, "meat paste item")

        var untinkerAttempts = 0
        var probeCount = 0
        val engine = MockEngine { request ->
            val payload = requestPayload(request)
            when {
                payload.contains("preaction=screwquest") ->
                    respond("I'm just lost without my screwdriver", HttpStatusCode.OK)
                payload.contains("preaction=untinker") -> {
                    untinkerAttempts++
                    if (untinkerAttempts == 1) {
                        respond("You need a screwdriver first.", HttpStatusCode.OK)
                    } else {
                        respond("You acquire an item: meat paste", HttpStatusCode.OK)
                    }
                }
                payload.contains("action=fv_untinker") && request.method == HttpMethod.Get -> {
                    probeCount++
                    respond(
                        if (probeCount >= 2) {
                            "<select name=whichitem></select>"
                        } else {
                            "Visit Degrassi Knoll to find my screwdriver."
                        },
                        HttpStatusCode.OK,
                    )
                }
                payload.contains("dk_innabox") ->
                    respond("ok", HttpStatusCode.OK)
                else -> respond("ok", HttpStatusCode.OK)
            }
        }

        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(sign = "Mongoose"))

        val inventory = MutableInventoryManager(
            mapOf(
                MEAT_PASTE_ITEM to InventoryItem(MEAT_PASTE_ITEM, "meat paste item", 1, ItemType.OTHER),
            ),
        )

        val request = UntinkerRequest(
            client = HttpClient(engine),
            inventoryManager = inventory,
            gameDatabase = GameDatabase(),
            character = char,
            questDatabase = QuestDatabase(Preferences(MapSettings())),
        )

        val result = request.untinker(MEAT_PASTE_ITEM, 1)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        assertTrue(untinkerAttempts >= 2)
    }

    @Test
    fun syncQuestFromResponse_marksUntinkerStartedAndFinished() {
        val prefs = Preferences(MapSettings())
        val questDb = QuestDatabase(prefs)

        UntinkerRequest.syncQuestFromResponse(
            urlString = "place.php?preaction=screwquest",
            responseText = "I'm just lost without my screwdriver",
            inventoryHasScrewdriver = false,
            questDatabase = questDb,
        )
        assertEquals(QuestDatabase.STARTED, questDb.getProgress(Quest.UNTINKER))

        var removed = false
        UntinkerRequest.syncQuestFromResponse(
            urlString = "place.php?action=fv_untinker",
            responseText = "ok",
            inventoryHasScrewdriver = true,
            questDatabase = questDb,
            onScrewdriverRemoved = { removed = true },
        )
        assertEquals(QuestDatabase.FINISHED, questDb.getProgress(Quest.UNTINKER))
        assertTrue(removed)
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

    private class MutableInventoryManager(
        initial: Map<Int, InventoryItem> = emptyMap(),
    ) : InventoryManager(HttpClient(MockEngine { respond("ok") }), GameEventBus()) {
        private val flow = MutableStateFlow(InventoryState(items = initial.toMutableMap()))
        override val state = flow.asStateFlow()

        override suspend fun fetchInventory() {
        }

        override fun consumeItemLocally(itemId: Int, quantity: Int) {
            val items = flow.value.items.toMutableMap()
            val current = items[itemId]?.quantity ?: 0
            val next = (current - quantity).coerceAtLeast(0)
            if (next == 0) {
                items.remove(itemId)
            } else {
                items[itemId] = items[itemId]!!.copy(quantity = next)
            }
            flow.value = flow.value.copy(items = items)
        }

        fun addItem(itemId: Int, name: String, quantity: Int) {
            val items = flow.value.items.toMutableMap()
            items[itemId] = InventoryItem(itemId, name, quantity, ItemType.OTHER)
            flow.value = flow.value.copy(items = items)
        }
    }

    companion object {
        private const val MEAT_PASTE_ITEM = 1001
    }
}
