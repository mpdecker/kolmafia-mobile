package net.sourceforge.kolmafia.adventure.choice.handlers

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.adventure.choice.ChoiceContext
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.adventure.choice.ChoiceSolvers
import net.sourceforge.kolmafia.adventure.choice.solvers.ArcadeGameSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.GameproSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.LightsOutSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.LostKeySolver
import net.sourceforge.kolmafia.adventure.choice.solvers.SafetyShelterSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.VampOutSolver
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.skill.SkillState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VillainLairHandlersTest {

    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    /** Fresh isolated preferences per call — no cross-test state leakage. */
    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

    private fun buildHtml(vararg options: Pair<Int, String>): String =
        options.joinToString(" ") { (num, text) ->
            "name=whichchoice value=$num $text"
        }

    private fun ctx(
        choiceId: Int,
        responseText: String = "",
        prefs: Preferences = prefs(),
    ) = ChoiceContext(
        choiceId = choiceId, options = (1..6).associateWith { "O$it" },
        responseText = responseText, characterState = CharacterState(),
        inventoryState = InventoryState(), effectState = EffectState(),
        skillState = SkillState(), preferences = prefs,
        goalManager = GoalManager(), questDatabase = QuestDatabase(prefs),
        solvers = noOpSolvers, preference = 0,
    )

    private fun decide(
        choiceId: Int,
        responseText: String = "",
        prefs: Preferences = prefs(),
    ) = VillainLairHandlers.handlers[choiceId]?.decide(ctx(choiceId, responseText, prefs))

    // Test 1: Returns option 1 when color matches option 1's HTML fragment
    @Test fun colorMatchesOption1_returns1() {
        val html = buildHtml(
            1 to "red door",
            2 to "blue door",
            3 to "green door",
        )
        val result = decide(
            choiceId = 1260,
            responseText = html,
            prefs = prefs { setString("_villainLairColor", "red") },
        )
        assertEquals(1, result)
    }

    // Test 2: Returns option 2 when color matches option 2's HTML fragment
    @Test fun colorMatchesOption2_returns2() {
        val html = buildHtml(
            1 to "red door",
            2 to "blue door",
            3 to "green door",
        )
        val result = decide(
            choiceId = 1260,
            responseText = html,
            prefs = prefs { setString("_villainLairColor", "blue") },
        )
        assertEquals(2, result)
    }

    // Test 3: Returns null when _villainLairColor is empty
    @Test fun emptyColor_returnsNull() {
        val html = buildHtml(
            1 to "red door",
            2 to "blue door",
        )
        val result = decide(
            choiceId = 1260,
            responseText = html,
            prefs = prefs { setString("_villainLairColor", "") },
        )
        assertNull(result)
    }

    // Test 4: Returns null when color not found in any option's HTML
    @Test fun colorNotFound_returnsNull() {
        val html = buildHtml(
            1 to "red door",
            2 to "blue door",
        )
        val result = decide(
            choiceId = 1260,
            responseText = html,
            prefs = prefs { setString("_villainLairColor", "purple") },
        )
        assertNull(result)
    }

    // Test 5: Handler 1262 uses same logic (returns correct option)
    @Test fun choice1262_colorMatchesOption3_returns3() {
        val html = buildHtml(
            1 to "red door",
            2 to "blue door",
            3 to "green door",
        )
        val result = decide(
            choiceId = 1262,
            responseText = html,
            prefs = prefs { setString("_villainLairColor", "green") },
        )
        assertEquals(3, result)
    }

    // Test 6: registerAll registers handlers for both 1260 and 1262
    @Test fun registerAll_registersBothChoices() {
        val registry = ChoiceHandlerRegistry()
        VillainLairHandlers.registerAll(registry)

        val html = buildHtml(1 to "red door", 2 to "blue door")
        val p = prefs { setString("_villainLairColor", "red") }

        val result1260 = registry.dispatch(ctx(1260, html, p))
        val result1262 = registry.dispatch(ctx(1262, html, p))

        assertEquals(1, result1260)
        assertEquals(1, result1262)
    }
}
