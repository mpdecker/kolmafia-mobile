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

class SkillUsesHandlersTest {

    private val prefs = Preferences(MapSettings())
    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    private fun ctx(choiceId: Int, skillUses: Int = 0) = ChoiceContext(
        choiceId = choiceId, options = (1..2).associateWith { "O$it" },
        responseText = "", characterState = CharacterState(),
        inventoryState = InventoryState(), effectState = EffectState(),
        skillState = SkillState(), preferences = prefs,
        goalManager = GoalManager(), questDatabase = QuestDatabase(prefs),
        solvers = noOpSolvers, preference = 0, skillUses = skillUses,
    )

    @Test fun case600_noSkillUses_returns2() =
        assertEquals(2, SkillUsesHandlers.handlers[600]?.decide(ctx(600, 0)))
    @Test fun case600_hasSkillUses_returns1() =
        assertEquals(1, SkillUsesHandlers.handlers[600]?.decide(ctx(600, 1)))
    @Test fun case600_multipleSkillUses_returns1() =
        assertEquals(1, SkillUsesHandlers.handlers[600]?.decide(ctx(600, 3)))

    @Test fun case601_noSkillUses_returns2() =
        assertEquals(2, SkillUsesHandlers.handlers[601]?.decide(ctx(601, 0)))
    @Test fun case601_hasSkillUses_returns1() =
        assertEquals(1, SkillUsesHandlers.handlers[601]?.decide(ctx(601, 2)))
}
