package net.sourceforge.kolmafia.adventure.choice.handlers

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.adventure.choice.ChoiceContext
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

class DreadsylvaniaHandlersTest {

    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    /** Fresh isolated preferences per call — no cross-test state leakage. */
    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

    private fun ctx(choiceId: Int, preference: Int = 0, response: String = "",
                    prefs: Preferences = prefs()) = ChoiceContext(
        choiceId = choiceId, options = (1..6).associateWith { "O$it" },
        responseText = response, characterState = CharacterState(),
        inventoryState = InventoryState(), effectState = EffectState(),
        skillState = SkillState(), preferences = prefs,
        goalManager = GoalManager(), questDatabase = QuestDatabase(prefs),
        solvers = noOpSolvers, preference = preference,
    )

    private fun decide(choiceId: Int, preference: Int = 0, response: String = "",
                       prefs: Preferences = prefs()) =
        DreadsylvaniaHandlers.handlers[choiceId]?.decide(ctx(choiceId, preference, response, prefs))

    // Case 721 — pref 5, pencil option absent → returns 6
    @Test fun case721_pref5_pencilAbsent_returns6() = assertEquals(6, decide(721, 5, "nothing"))

    // Case 721 — pref 5, pencil option present, not yet used → returns 5 (pref)
    @Test fun case721_pref5_pencilPresent_notUsed_returnsPref() =
        assertEquals(5, decide(721, 5, "Use a ghost pencil",
            prefs = prefs { setBoolean("ghostPencil1", false) }))

    // Case 721 — pref 5, pencil option present, but already used → returns 6
    @Test fun case721_pref5_pencilPresent_alreadyUsed_returns6() =
        assertEquals(6, decide(721, 5, "Use a ghost pencil",
            prefs = prefs { setBoolean("ghostPencil1", true) }))

    // Case 721 — pref 3 (not 5) → pass through preference
    @Test fun case721_pref3_returnsPreference() = assertEquals(3, decide(721, 3))

    // Case 721 — pref 0 → null
    @Test fun case721_pref0_returnsNull() = assertNull(decide(721, 0))

    // Case 753 — last zone, verify each zone uses its own prefKey (ghostPencil9 not ghostPencil1)
    @Test fun case753_pref5_pencilAbsent_returns6() = assertEquals(6, decide(753, 5, "nothing"))

    @Test fun case753_pref5_pencilPresent_notUsed_returnsPref() =
        assertEquals(5, decide(753, 5, "Use a ghost pencil",
            prefs = prefs { setBoolean("ghostPencil9", false) }))

    // Verify prefKey isolation: ghostPencil1=true should not affect case 753 (ghostPencil9)
    @Test fun case753_pencilNotAffectedByCase721PrefKey() =
        assertEquals(5, decide(753, 5, "Use a ghost pencil",
            prefs = prefs { setBoolean("ghostPencil1", true) /* ghostPencil9 not set */ }))

    // All 9 choices registered with correct "no pencil" behaviour
    @Test fun allNineChoicesRegistered() {
        val ids = listOf(721, 725, 729, 733, 737, 741, 745, 749, 753)
        for (id in ids) {
            assertEquals(6, decide(id, 5, "nothing"),
                "Expected 6 for choice $id pref=5 pencil absent")
        }
    }
}
