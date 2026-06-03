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
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.skill.SkillState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuestHandlersTest {

    private val prefs = Preferences(MapSettings())
    private val questDb = QuestDatabase(prefs)
    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    private fun ctx(choiceId: Int, preference: Int = 0) = ChoiceContext(
        choiceId = choiceId, options = (1..6).associateWith { "O$it" },
        responseText = "", characterState = CharacterState(),
        inventoryState = InventoryState(), effectState = EffectState(),
        skillState = SkillState(), preferences = prefs,
        goalManager = GoalManager(), questDatabase = questDb,
        solvers = noOpSolvers, preference = preference,
    )

    private fun decide(choiceId: Int, preference: Int = 0) =
        QuestHandlers.handlers[choiceId]?.decide(ctx(choiceId, preference))

    // Case 1060
    @Test fun case1060_pref4_meatsmithUnstarted_returnsPref() {
        questDb.setProgress(Quest.MEATSMITH, QuestDatabase.UNSTARTED)
        assertEquals(4, decide(1060, 4))
    }
    @Test fun case1060_pref4_meatsmithStarted_returnsPref() {
        // isQuestLaterThan(MEATSMITH, STARTED) = false when progress == STARTED
        questDb.setProgress(Quest.MEATSMITH, QuestDatabase.STARTED)
        assertEquals(4, decide(1060, 4))
    }
    @Test fun case1060_pref4_meatsmithPastStarted_returnsNull() {
        questDb.setProgress(Quest.MEATSMITH, "step1")
        assertNull(decide(1060, 4))
    }
    @Test fun case1060_pref1_returnsPreference() {
        questDb.setProgress(Quest.MEATSMITH, "step1")
        assertEquals(1, decide(1060, 1))
    }

    // Case 1061
    @Test fun case1061_pref1_armorerAtStep4_returnsPref() {
        // isQuestLaterThan(ARMORER, "step4") = false when at step4
        questDb.setProgress(Quest.ARMORER, "step4")
        assertEquals(1, decide(1061, 1))
    }
    @Test fun case1061_pref1_armorerPastStep4_returnsNull() {
        questDb.setProgress(Quest.ARMORER, "step5")
        assertNull(decide(1061, 1))
    }
    @Test fun case1061_pref3_armorerNotFinished_returnsNull() {
        questDb.setProgress(Quest.ARMORER, "step3")
        assertNull(decide(1061, 3))
    }
    @Test fun case1061_pref3_armorerFinished_returnsPref() {
        questDb.setProgress(Quest.ARMORER, QuestDatabase.FINISHED)
        assertEquals(3, decide(1061, 3))
    }
    @Test fun case1061_pref2_returnsPreference() {
        questDb.setProgress(Quest.ARMORER, "step1")
        assertEquals(2, decide(1061, 2))
    }
}
