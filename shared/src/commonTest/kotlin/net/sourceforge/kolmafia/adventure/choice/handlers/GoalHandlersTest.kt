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

class GoalHandlersTest {

    private val prefs = Preferences(MapSettings())
    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    private fun ctx(choiceId: Int, preference: Int = 0, goalManager: GoalManager = GoalManager()) =
        ChoiceContext(
            choiceId = choiceId, options = (1..3).associateWith { "O$it" },
            responseText = "", characterState = CharacterState(),
            inventoryState = InventoryState(), effectState = EffectState(),
            skillState = SkillState(), preferences = prefs,
            goalManager = goalManager, questDatabase = QuestDatabase(prefs),
            solvers = noOpSolvers, preference = preference,
        )

    // WOODS_ITEM_IDS = [1..12]; for choice 26: i/4+1 → item 1 (i=0) → opt 1, item 5 (i=4) → opt 2
    // for choice 27: i%4/2+1 → item 1 (i=0) → opt 1, item 3 (i=2) → opt 2

    @Test fun case26_noGoal_returnsNull() = assertNull(GoalHandlers.handlers[26]?.decide(ctx(26)))
    @Test fun case26_noGoal_pref2_returnsPref() =
        assertEquals(2, GoalHandlers.handlers[26]?.decide(ctx(26, 2)))

    @Test fun case26_item1Goal_returnsOption1() {
        val gm = GoalManager().also { it.addItemGoal(ItemPool.WOODS_ITEM_IDS[0]) } // i=0 → 0/4+1=1
        assertEquals(1, GoalHandlers.handlers[26]?.decide(ctx(26, goalManager = gm)))
    }
    @Test fun case26_item5Goal_returnsOption2() {
        val gm = GoalManager().also { it.addItemGoal(ItemPool.WOODS_ITEM_IDS[4]) } // i=4 → 4/4+1=2
        assertEquals(2, GoalHandlers.handlers[26]?.decide(ctx(26, goalManager = gm)))
    }
    @Test fun case26_item9Goal_returnsOption3() {
        val gm = GoalManager().also { it.addItemGoal(ItemPool.WOODS_ITEM_IDS[8]) } // i=8 → 8/4+1=3
        assertEquals(3, GoalHandlers.handlers[26]?.decide(ctx(26, goalManager = gm)))
    }

    @Test fun case27_noGoal_returnsNull() = assertNull(GoalHandlers.handlers[27]?.decide(ctx(27)))
    @Test fun case27_item1Goal_returnsOption1() {
        val gm = GoalManager().also { it.addItemGoal(ItemPool.WOODS_ITEM_IDS[0]) } // i=0 → 0%4/2+1=1
        assertEquals(1, GoalHandlers.handlers[27]?.decide(ctx(27, goalManager = gm)))
    }
    @Test fun case27_item3Goal_returnsOption2() {
        val gm = GoalManager().also { it.addItemGoal(ItemPool.WOODS_ITEM_IDS[2]) } // i=2 → 2%4/2+1=2
        assertEquals(2, GoalHandlers.handlers[27]?.decide(ctx(27, goalManager = gm)))
    }
}
