package net.sourceforge.kolmafia.adventure.choice.handlers

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.adventure.RufusManager
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

class RufusHandlersTest {

    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    /** Fresh isolated preferences per call — no cross-test state leakage. */
    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

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

    // Test 1: choice 1498 delegates to chooseQuestOption
    // inject artifact quest type, HTML with "artifact", expect option 2
    @Test fun choice1498_artifactQuestType_withArtifactHtml_returns2() {
        val rufusManager = RufusManager(prefs { setString(Preferences.RUFUS_QUEST_TYPE, "artifact") })
        val registry = ChoiceHandlerRegistry()
        RufusHandlers.registerAll(registry, rufusManager)

        val html = "Choose your quest type: entity, artifact, or monument"
        val result = registry.dispatch(ctx(1498, html))
        assertEquals(2, result)
    }

    // Test 2: choice 1498 returns null when quest type not in HTML
    // inject monument quest type, HTML without "monument", expect null
    @Test fun choice1498_monumentQuestType_withoutMonumentHtml_returnsNull() {
        val rufusManager = RufusManager(prefs { setString(Preferences.RUFUS_QUEST_TYPE, "monument") })
        val registry = ChoiceHandlerRegistry()
        RufusHandlers.registerAll(registry, rufusManager)

        val html = "Choose your quest type: entity or artifact"
        val result = registry.dispatch(ctx(1498, html))
        assertNull(result)
    }

    // Test 3: choice 1499 always returns 1
    @Test fun choice1499_alwaysReturns1() {
        val rufusManager = RufusManager(prefs { setString(Preferences.RUFUS_QUEST_TYPE, "entity") })
        val registry = ChoiceHandlerRegistry()
        RufusHandlers.registerAll(registry, rufusManager)

        val result = registry.dispatch(ctx(1499, "any response text"))
        assertEquals(1, result)
    }
}
