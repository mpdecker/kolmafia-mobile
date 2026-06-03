package net.sourceforge.kolmafia.adventure.choice

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.skill.SkillState
import net.sourceforge.kolmafia.adventure.choice.solvers.SafetyShelterSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.VampOutSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.ArcadeGameSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.LostKeySolver
import net.sourceforge.kolmafia.adventure.choice.solvers.GameproSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.LightsOutSolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChoiceHandlerRegistryTest {

    private val prefs = Preferences(MapSettings())
    private val noOpSolvers = ChoiceSolvers(
        safetyShelter = SafetyShelterSolver.NoOp,
        vampOut = VampOutSolver.NoOp,
        arcadeGame = ArcadeGameSolver.NoOp,
        lostKey = LostKeySolver.NoOp,
        gamepro = GameproSolver.NoOp,
        lightsOut = LightsOutSolver.NoOp,
    )

    private fun registry() = ChoiceHandlerRegistry()

    private fun ctx(choiceId: Int, preference: Int = 0) = ChoiceContext(
        choiceId = choiceId, options = mapOf(1 to "A", 2 to "B"),
        responseText = "", characterState = CharacterState(),
        inventoryState = InventoryState(), effectState = EffectState(),
        skillState = SkillState(), preferences = prefs,
        goalManager = GoalManager(), questDatabase = QuestDatabase(prefs),
        solvers = noOpSolvers, preference = preference,
    )

    @Test fun knownHandler_callsHandler() {
        val r = registry()
        r.register(42) { _ -> 1 }
        assertEquals(1, r.dispatch(ctx(42)))
    }

    @Test fun unknownChoice_returnsNull() {
        assertNull(registry().dispatch(ctx(9999)))
    }

    @Test fun handlerReturnsNull_returnsManuralControlNotPreference() {
        // A registered handler returning null signals "go manual" — the preference
        // must NOT be applied, because the handler is explicitly overriding it
        // (e.g., the desired option is not on the page this step).
        val r = registry()
        r.register(42) { _ -> null }
        assertNull(r.dispatch(ctx(42, preference = 2)))
    }

    @Test fun handlerThatRespectsPreference_returnsPreference() {
        // Handlers that want to fall through to preference do so explicitly:
        val r = registry()
        r.register(42) { c -> c.preference.takeIf { it > 0 } }
        assertEquals(2, r.dispatch(ctx(42, preference = 2)))
    }

    @Test fun noHandler_nonZeroPreference_returnsPreference() {
        assertEquals(3, registry().dispatch(ctx(9998, preference = 3)))
    }

    @Test fun noHandler_zeroPreference_returnsNull() {
        assertNull(registry().dispatch(ctx(9997, preference = 0)))
    }
}
