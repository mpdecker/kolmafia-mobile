package net.sourceforge.kolmafia.adventure.choice.handlers

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.adventure.choice.ChoiceContext
import net.sourceforge.kolmafia.adventure.choice.ChoiceSolvers
import net.sourceforge.kolmafia.adventure.choice.ItemPool
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

class MiscHandlersTest {

    private val prefs = Preferences(MapSettings())
    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    private fun ctx(
        choiceId: Int,
        preference: Int = 0,
        response: String = "",
        goalManager: GoalManager = GoalManager(),
    ) = ChoiceContext(
        choiceId = choiceId, options = (1..6).associateWith { "O$it" },
        responseText = response, characterState = CharacterState(),
        inventoryState = InventoryState(), effectState = EffectState(),
        skillState = SkillState(), preferences = prefs,
        goalManager = goalManager, questDatabase = QuestDatabase(prefs),
        solvers = noOpSolvers, preference = preference,
    )

    private fun decide(choiceId: Int, preference: Int = 0, response: String = "",
                       goalManager: GoalManager = GoalManager()) =
        MiscHandlers.handlers[choiceId]?.decide(ctx(choiceId, preference, response, goalManager))

    // Case 182
    @Test fun case182_option4Available_airshipGoal_returns4() {
        val gm = GoalManager().also { it.addItemGoal(ItemPool.MODEL_AIRSHIP) }
        assertEquals(4, decide(182, 0, "Gallivant down to the head", gm))
    }
    @Test fun case182_option4Available_noGoal_pref4_returns4() =
        assertEquals(4, decide(182, 4, "Gallivant down to the head"))
    @Test fun case182_option4Unavailable_pref4_returnsShifted() =
        assertEquals(1, decide(182, 4, "nothing"))
    @Test fun case182_pref2_returns2() = assertEquals(2, decide(182, 2))
    @Test fun case182_pref0_returnsNull() = assertNull(decide(182, 0, "nothing"))

    // Case 690/691/693 — pass-through
    @Test fun case690_pref3_returns3() = assertEquals(3, decide(690, 3))
    @Test fun case690_pref0_returnsNull() = assertNull(decide(690, 0))
    @Test fun case691_pref2_returns2() = assertEquals(2, decide(691, 2))
    @Test fun case693_pref1_returns1() = assertEquals(1, decide(693, 1))

    // Case 879
    @Test fun case879_pref4_sausagesAvailable_returns4() =
        assertEquals(4, decide(879, 4, "Check under the nightstand"))
    @Test fun case879_pref4_sausagesUnavailable_returns1() =
        assertEquals(1, decide(879, 4, "nothing"))
    @Test fun case879_mistressGoal_returns3() {
        val gm = GoalManager().also { it.addItemGoal(ItemPool.CHINTZY_SEAL_PENDANT) }
        assertEquals(3, decide(879, 1, goalManager = gm))
    }
    @Test fun case879_noGoal_returnsPref() = assertEquals(2, decide(879, 2))
    @Test fun case879_pref0_returnsNull() = assertNull(decide(879, 0))

    // Case 914
    @Test fun case914_louvreGoalNonZero_returns1() {
        prefs.setInt("louvreGoal", 3)
        assertEquals(1, decide(914))
    }
    @Test fun case914_louvreGoalZero_returns2() {
        prefs.setInt("louvreGoal", 0)
        assertEquals(2, decide(914))
    }

    // Case 988
    @Test fun case988_directionsL_returns1() {
        prefs.setString("EVEDirections", "LRLRL0")
        assertEquals(1, decide(988))
    }
    @Test fun case988_directionsR_returns2() {
        prefs.setString("EVEDirections", "RRLLL0")
        assertEquals(2, decide(988))
    }
    @Test fun case988_wrongLength_returnsNull() {
        prefs.setString("EVEDirections", "LRL")
        assertNull(decide(988, 0))
    }
    @Test fun case988_invalidProgress_returnsNull() {
        prefs.setString("EVEDirections", "LRLRL9")
        assertNull(decide(988, 0))
    }

    // Case 989
    @Test fun case989_constellation_returns1() =
        assertEquals(1, decide(989, 0, "ever-changing constellation"))
    @Test fun case989_circleOfLight_returns2() =
        assertEquals(2, decide(989, 0, "card in the circle of light"))
    @Test fun case989_flyAway_returns3() =
        assertEquals(3, decide(989, 0, "waves a fly away"))
    @Test fun case989_squareOne_returns4() =
        assertEquals(4, decide(989, 0, "back to square one"))
    @Test fun case989_anxiety_returns5() =
        assertEquals(5, decide(989, 0, "adds to your anxiety"))
    @Test fun case989_noMatch_returnsNull() = assertNull(decide(989, 0, "nothing"))
}
