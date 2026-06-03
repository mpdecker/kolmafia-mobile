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

class DreadsylvaniaHandlersTest {

    private val prefs = Preferences(MapSettings())
    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    private fun ctx(choiceId: Int, preference: Int = 0, response: String = "") = ChoiceContext(
        choiceId = choiceId, options = (1..6).associateWith { "O$it" },
        responseText = response, characterState = CharacterState(),
        inventoryState = InventoryState(), effectState = EffectState(),
        skillState = SkillState(), preferences = prefs,
        goalManager = GoalManager(), questDatabase = QuestDatabase(prefs),
        solvers = noOpSolvers, preference = preference,
    )

    private fun decide(choiceId: Int, preference: Int = 0, response: String = "") =
        DreadsylvaniaHandlers.handlers[choiceId]?.decide(ctx(choiceId, preference, response))

    // All 9 cases share the same ghostPencil logic; test representative cases

    // Case 721 — pref 5, pencil option absent → returns 6
    @Test fun case721_pref5_pencilAbsent_returns6() = assertEquals(6, decide(721, 5, "nothing"))

    // Case 721 — pref 5, pencil option present, prefBool false → returns 5 (pref)
    @Test fun case721_pref5_pencilPresent_notUsed_returnsPref() {
        prefs.setBoolean("ghostPencil1", false)
        assertEquals(5, decide(721, 5, "Use a ghost pencil"))
    }

    // Case 721 — pref 5, pencil option present, but prefBool true → returns 6
    @Test fun case721_pref5_pencilPresent_alreadyUsed_returns6() {
        prefs.setBoolean("ghostPencil1", true)
        assertEquals(6, decide(721, 5, "Use a ghost pencil"))
    }

    // Case 721 — pref 3 (not 5) → returns preference
    @Test fun case721_pref3_returnsPreference() = assertEquals(3, decide(721, 3))

    // Case 721 — pref 0 → returns null
    @Test fun case721_pref0_returnsNull() {
        val result = decide(721, 0)
        assertEquals(null, result)
    }

    // Case 753 — last zone, verify prefKey is wired correctly
    @Test fun case753_pref5_pencilAbsent_returns6() = assertEquals(6, decide(753, 5, "nothing"))
    @Test fun case753_pref5_pencilPresent_notUsed_returnsPref() {
        prefs.setBoolean("ghostPencil9", false)
        assertEquals(5, decide(753, 5, "Use a ghost pencil"))
    }

    // All 9 choices are registered
    @Test fun allNineChoicesRegistered() {
        val ids = listOf(721, 725, 729, 733, 737, 741, 745, 749, 753)
        for (id in ids) {
            val result = decide(id, 5, "nothing")
            assertEquals(6, result, "Expected 6 for choice $id with pref 5 and no pencil option")
        }
    }
}
