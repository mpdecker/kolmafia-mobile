package net.sourceforge.kolmafia.adventure.choice

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.adventure.choice.handlers.ComplexHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.DreadsylvaniaHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.GoalHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.HiddenCityHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.InventoryHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.MiscHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.QuestHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.ResponseTextHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.SkillUsesHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.SolverHandlers
import net.sourceforge.kolmafia.adventure.choice.handlers.StatHandlers
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

class ChoiceHandlerIntegrationTest {

    private val prefs = Preferences(MapSettings())
    private val registry = ChoiceHandlerRegistry().also { r ->
        InventoryHandlers.registerAll(r)
        ResponseTextHandlers.registerAll(r)
        StatHandlers.registerAll(r)
        ComplexHandlers.registerAll(r)
        DreadsylvaniaHandlers.registerAll(r)
        HiddenCityHandlers.registerAll(r)
        MiscHandlers.registerAll(r)
        GoalHandlers.registerAll(r)
        QuestHandlers.registerAll(r)
        SkillUsesHandlers.registerAll(r)
        SolverHandlers.registerAll(r)
    }

    private fun ctx(choiceId: Int, preference: Int = 0, response: String = "",
                    skillUses: Int = 0) = ChoiceContext(
        choiceId = choiceId, options = (1..6).associateWith { "O$it" },
        responseText = response, characterState = CharacterState(),
        inventoryState = InventoryState(), effectState = EffectState(),
        skillState = SkillState(), preferences = prefs,
        goalManager = GoalManager(), questDatabase = QuestDatabase(prefs),
        solvers = ChoiceSolvers(
            SafetyShelterSolver.NoOp, VampOutSolver.NoOp,
            ArcadeGameSolver.NoOp, LostKeySolver.NoOp,
            GameproSolver.NoOp, LightsOutSolver.NoOp,
        ),
        preference = preference, skillUses = skillUses,
    )

    // Registry dispatch — unknown choices
    @Test fun unknownChoice_pref0_returnsNull() = assertNull(registry.dispatch(ctx(9999)))
    @Test fun unknownChoice_pref2_returnsPreference() = assertEquals(2, registry.dispatch(ctx(9999, 2)))

    // Spot-check one case from each handler group

    // InventoryHandlers: case 5
    @Test fun case5_noRock_returns2() = assertEquals(2, registry.dispatch(ctx(5)))

    // ResponseTextHandlers: case 155
    @Test fun case155_pref4_shinyAbsent_returns5() = assertEquals(5, registry.dispatch(ctx(155, 4, "nothing")))

    // ResponseTextHandlers: case 1222
    @Test fun case1222_alreadyGone_returns2() =
        assertEquals(2, registry.dispatch(ctx(1222, 1, "You've already gone through the Tunnel once today")))

    // DreadsylvaniaHandlers: case 721
    @Test fun case721_pref3_returnsPreference() = assertEquals(3, registry.dispatch(ctx(721, 3)))

    // MiscHandlers: case 989
    @Test fun case989_constellation_returns1() =
        assertEquals(1, registry.dispatch(ctx(989, 0, "ever-changing constellation")))

    // SolverHandlers: case 702
    @Test fun case702_facingNorth_returns1() =
        assertEquals(1, registry.dispatch(ctx(702, 0, "you are facing north")))

    // SkillUsesHandlers: case 600
    @Test fun case600_noSkillUses_returns2() = assertEquals(2, registry.dispatch(ctx(600)))
    @Test fun case600_hasSkillUses_returns1() =
        assertEquals(1, registry.dispatch(ctx(600, skillUses = 1)))

    // SolverHandlers: case 486 (NoOp → null → falls to preference=0 → null)
    @Test fun case486_noOpSolver_noPreference_returnsNull() =
        assertNull(registry.dispatch(ctx(486)))
}
