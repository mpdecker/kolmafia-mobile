package net.sourceforge.kolmafia.adventure.choice

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.adventure.choice.solvers.FightersOfFighting
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class VioletFogManagerTest {

    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        VioletFogManager.reset(prefs, ascensions = 1)
    }

    @Test
    fun fogChoiceRange() {
        assertTrue(VioletFogManager.fogChoice(48))
        assertTrue(VioletFogManager.fogChoice(70))
        assertEquals(false, VioletFogManager.fogChoice(47))
    }

    @Test
    fun escapeGoalPicksOptionFour() {
        prefs.setInt("violetFogGoal", 0)
        val decision = VioletFogManager.handleChoice(
            source = 48,
            preferences = prefs,
            goalManager = GoalManager(),
            characterState = CharacterState(),
        )
        assertEquals(4, decision)
    }

    @Test
    fun nextHopFromStartTowardGoal() {
        // From 48 toward 62 (cloche), first hop among exits
        val hop = VioletFogManager.nextHop(48, 62)
        assertTrue(hop in listOf(49, 50, 51))
    }

    @Test
    fun mapChoiceLearnsExit() {
        VioletFogManager.reset(prefs, 1)
        assertTrue(
            VioletFogManager.mapChoice(
                lastChoice = 48,
                lastDecision = 1,
                text = """<form><input type=hidden name=whichchoice value=49></form>""",
                preferences = prefs,
                ascensions = 1,
            ),
        )
        assertEquals(49, VioletFogManager.choiceAt(48, 1))
        assertTrue(prefs.getString("violetFogLayout", "").isNotBlank())
    }

    @Test
    fun atGoalLocationTakesOptionOne() {
        prefs.setInt("violetFogGoal", 1) // Cerebral Cloche → location 62
        VioletFogManager.setChoiceForTest(62, 1, -1)
        val decision = VioletFogManager.handleChoice(
            62,
            prefs,
            GoalManager(),
            CharacterState(),
        )
        assertEquals(1, decision)
    }
}

class ChoiceCostTest {

    @Test
    fun rubberAxeCostLookup() {
        val cost = ChoiceCost.getCost(2, 1)
        assertNotNull(cost)
        assertEquals(ChoiceCost.Kind.ITEM, cost.kind)
        assertEquals(-1, cost.amount)
        assertEquals(292, cost.itemId)
    }

    @Test
    fun payMeatCost() {
        val prefs = Preferences(MapSettings())
        val character = KoLCharacter()
        character.updateMeat(1000)
        assertTrue(ChoiceCost.payCost(21, 1, inventory = null, character = character))
        assertEquals(500, character.state.value.meat)
    }

    @Test
    fun payItemCost() {
        val prefs = Preferences(MapSettings())
        val inv = object : InventoryManager(
            HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        292 to InventoryItem(292, "rubber axe", 2, ItemType.OTHER),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
            override fun consumeItemLocally(itemId: Int, quantity: Int) {
                val cur = flow.value.items[itemId] ?: return
                flow.value = flow.value.copy(
                    items = flow.value.items + (itemId to cur.copy(quantity = cur.quantity - quantity)),
                )
            }
        }
        assertTrue(ChoiceCost.payCost(2, 1, inv, null))
        assertEquals(1, inv.state.value.items[292]?.quantity)
    }
}

class LouvreManagerTest {

    @Test
    fun louvreRange() {
        assertTrue(LouvreManager.louvreChoice(904))
        assertTrue(LouvreManager.louvreChoice(913))
        assertEquals(false, LouvreManager.louvreChoice(914))
    }

    @Test
    fun overrideScript() {
        val prefs = Preferences(MapSettings())
        prefs.setString("louvreOverride", "up,down,side")
        assertEquals(
            1,
            LouvreManager.handleChoice(905, 0, prefs, GoalManager(), CharacterState()),
        )
        assertEquals(
            2,
            LouvreManager.handleChoice(905, 1, prefs, GoalManager(), CharacterState()),
        )
        assertEquals(
            3,
            LouvreManager.handleChoice(905, 2, prefs, GoalManager(), CharacterState()),
        )
    }
}

class FightersOfFightingTest {

    @Test
    fun initialMatchReturnsSix() {
        val html = """&quot;You Vs. Kitty the Zmobie Basher FIGHT!&quot;"""
        assertEquals(6, FightersOfFighting.autoChoice(html))
    }
}

class ChoiceAdventuresUsedTest {

    @Test
    fun hedgeMazeUsesAdventure() {
        assertEquals(1, ChoiceAdventuresUsed.adventuresForChoice(1005))
        assertEquals(
            1,
            ChoiceAdventuresUsed.getAdventuresUsed("choice.php?whichchoice=1005&option=1"),
        )
    }

    @Test
    fun gymUsesAdventure() {
        assertEquals(1, ChoiceAdventuresUsed.adventuresForChoice(770, 1))
        assertEquals(0, ChoiceAdventuresUsed.adventuresForChoice(770, 0))
    }
}

class DeferredChoiceTest {

    @Test
    fun registersHiddenTemple() {
        val prefs = Preferences(MapSettings())
        DeferredChoice.register(123, preferences = prefs)
        assertEquals(123, prefs.getInt("_lastDeferredChoice", 0))
    }
}
