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

class SolverHandlersTest {

    private val prefs = Preferences(MapSettings())
    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    private fun ctx(choiceId: Int, preference: Int = 0, response: String = "",
                    solvers: ChoiceSolvers = noOpSolvers, stepCount: Int = 0) = ChoiceContext(
        choiceId = choiceId, options = (1..6).associateWith { "O$it" },
        responseText = response, characterState = CharacterState(),
        inventoryState = InventoryState(), effectState = EffectState(),
        skillState = SkillState(), preferences = prefs,
        goalManager = GoalManager(), questDatabase = QuestDatabase(prefs),
        solvers = solvers, preference = preference, stepCount = stepCount,
    )

    // NoOp solvers return null for all
    @Test fun case486_noOpSolver_returnsNull() = assertNull(SolverHandlers.handlers[486]?.decide(ctx(486)))
    @Test fun case535_noOpSolver_returnsNull() = assertNull(SolverHandlers.handlers[535]?.decide(ctx(535)))
    @Test fun case536_noOpSolver_returnsNull() = assertNull(SolverHandlers.handlers[536]?.decide(ctx(536)))
    @Test fun case546_noOpSolver_returnsNull() = assertNull(SolverHandlers.handlers[546]?.decide(ctx(546)))
    @Test fun case594_noOpSolver_returnsNull() = assertNull(SolverHandlers.handlers[594]?.decide(ctx(594)))
    @Test fun case665_noOpSolver_returnsNull() = assertNull(SolverHandlers.handlers[665]?.decide(ctx(665)))

    // Case 702 — swamp navigation
    @Test fun case702_facingNorth_returns1() = assertEquals(1, SolverHandlers.handlers[702]?.decide(ctx(702, 0, "you are facing north")))
    @Test fun case702_faceNorth_returns1() = assertEquals(1, SolverHandlers.handlers[702]?.decide(ctx(702, 0, "face north now")))
    @Test fun case702_indicateNorth_returns1() = assertEquals(1, SolverHandlers.handlers[702]?.decide(ctx(702, 0, "indicate north")))
    @Test fun case702_facingEast_returns2() = assertEquals(2, SolverHandlers.handlers[702]?.decide(ctx(702, 0, "facing east")))
    @Test fun case702_facingSouth_returns3() = assertEquals(3, SolverHandlers.handlers[702]?.decide(ctx(702, 0, "facing south")))
    @Test fun case702_facingWest_returns4() = assertEquals(4, SolverHandlers.handlers[702]?.decide(ctx(702, 0, "facing west")))
    @Test fun case702_noDirection_returnsNull() = assertNull(SolverHandlers.handlers[702]?.decide(ctx(702, 0, "no direction")))

    // Lights Out cases 890–903 — NoOp returns null
    @Test fun case890_noOpSolver_returnsNull() = assertNull(SolverHandlers.handlers[890]?.decide(ctx(890)))
    @Test fun case903_noOpSolver_returnsNull() = assertNull(SolverHandlers.handlers[903]?.decide(ctx(903)))

    // All lights out cases registered
    @Test fun allLightsOutCasesRegistered() {
        for (i in 890..903) {
            assertNull(SolverHandlers.handlers[i]?.decide(ctx(i)),
                "Expected null from NoOp for lights out case $i")
        }
    }

    // Custom solver wiring — arcade game returns non-null
    @Test fun case486_customSolver_returnsValue() {
        val solvers = ChoiceSolvers(
            safetyShelter = SafetyShelterSolver.NoOp,
            vampOut = VampOutSolver.NoOp,
            arcadeGame = object : ArcadeGameSolver {
                override fun autoDungeonFist(stepCount: Int, responseText: String) = 3
            },
            lostKey = LostKeySolver.NoOp,
            gamepro = GameproSolver.NoOp,
            lightsOut = LightsOutSolver.NoOp,
        )
        assertEquals(3, SolverHandlers.handlers[486]?.decide(ctx(486, solvers = solvers)))
    }
}
