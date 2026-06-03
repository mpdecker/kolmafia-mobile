package net.sourceforge.kolmafia.adventure.choice.handlers

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.adventure.choice.ChoiceContext
import net.sourceforge.kolmafia.adventure.choice.ChoiceSolvers
import net.sourceforge.kolmafia.adventure.choice.EffectPool
import net.sourceforge.kolmafia.adventure.choice.solvers.ArcadeGameSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.GameproSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.LightsOutSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.LostKeySolver
import net.sourceforge.kolmafia.adventure.choice.solvers.SafetyShelterSolver
import net.sourceforge.kolmafia.adventure.choice.solvers.VampOutSolver
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.skill.SkillState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HiddenCityHandlersTest {

    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    /** Fresh isolated preferences per call — no cross-test state leakage. */
    private fun prefs(configure: Preferences.() -> Unit = {}) =
        Preferences(MapSettings()).also(configure)

    private fun ctx(
        choiceId: Int,
        preference: Int = 0,
        charState: CharacterState = CharacterState(),
        effects: List<EffectData> = emptyList(),
        prefs: Preferences = prefs(),
    ) = ChoiceContext(
        choiceId = choiceId, options = (1..6).associateWith { "O$it" },
        responseText = "", characterState = charState,
        inventoryState = InventoryState(),
        effectState = EffectState(effects = effects),
        skillState = SkillState(), preferences = prefs,
        goalManager = GoalManager(), questDatabase = QuestDatabase(prefs),
        solvers = noOpSolvers, preference = preference,
    )

    private fun decide(
        choiceId: Int,
        preference: Int = 0,
        charState: CharacterState = CharacterState(),
        effects: List<EffectData> = emptyList(),
        prefs: Preferences = prefs(),
    ) = HiddenCityHandlers.handlers[choiceId]?.decide(ctx(choiceId, preference, charState, effects, prefs))

    // Case 780
    @Test fun case780_pref1_apartmentDone_returns6() =
        assertEquals(6, decide(780, 1, prefs = prefs { setInt("hiddenApartmentProgress", 7) }))

    @Test fun case780_pref1_cursed_returns1() =
        assertEquals(1, decide(780, 1,
            effects = listOf(EffectData(0, EffectPool.CURSE3_EFFECT, 5)),
            prefs = prefs { setInt("hiddenApartmentProgress", 3) }))

    @Test fun case780_pref1_noCurse_returns2() =
        assertEquals(2, decide(780, 1, prefs = prefs { setInt("hiddenApartmentProgress", 3) }))

    @Test fun case780_pref3_lawyerRelocated_returns6() =
        assertEquals(6, decide(780, 3,
            charState = CharacterState(ascensionNumber = 5),
            prefs = prefs { setInt("relocatePygmyLawyer", 5) }))

    @Test fun case780_pref3_lawyerNotRelocated_returns3() =
        assertEquals(3, decide(780, 3,
            charState = CharacterState(ascensionNumber = 5),
            prefs = prefs { setInt("relocatePygmyLawyer", 4) }))

    @Test fun case780_pref2_returnsPreference() = assertEquals(2, decide(780, 2))

    // Case 781
    @Test fun case781_pref1_progress7_returns2() =
        assertEquals(2, decide(781, 1, prefs = prefs { setInt("hiddenApartmentProgress", 7) }))

    @Test fun case781_pref1_progress0_returns1() =
        assertEquals(1, decide(781, 1, prefs = prefs { setInt("hiddenApartmentProgress", 0) }))

    @Test fun case781_pref1_progressMid_returns6() =
        assertEquals(6, decide(781, 1, prefs = prefs { setInt("hiddenApartmentProgress", 3) }))

    @Test fun case781_pref2_returnsPreference() = assertEquals(2, decide(781, 2))

    // Case 783
    @Test fun case783_pref1_progress7_returns2() =
        assertEquals(2, decide(783, 1, prefs = prefs { setInt("hiddenHospitalProgress", 7) }))

    @Test fun case783_pref1_progress0_returns1() =
        assertEquals(1, decide(783, 1, prefs = prefs { setInt("hiddenHospitalProgress", 0) }))

    @Test fun case783_pref1_progressMid_returns6() =
        assertEquals(6, decide(783, 1, prefs = prefs { setInt("hiddenHospitalProgress", 4) }))

    // Case 785
    @Test fun case785_pref1_progress7_returns2() =
        assertEquals(2, decide(785, 1, prefs = prefs { setInt("hiddenOfficeProgress", 7) }))

    @Test fun case785_pref1_progress0_returns1() =
        assertEquals(1, decide(785, 1, prefs = prefs { setInt("hiddenOfficeProgress", 0) }))

    // Case 787
    @Test fun case787_pref1_progress7_returns2() =
        assertEquals(2, decide(787, 1, prefs = prefs { setInt("hiddenBowlingAlleyProgress", 7) }))

    @Test fun case787_pref1_progress0_returns1() =
        assertEquals(1, decide(787, 1, prefs = prefs { setInt("hiddenBowlingAlleyProgress", 0) }))

    // Case 789
    @Test fun case789_pref2_janitorRelocated_returns1() =
        assertEquals(1, decide(789, 2,
            charState = CharacterState(ascensionNumber = 3),
            prefs = prefs { setInt("relocatePygmyJanitor", 3) }))

    @Test fun case789_pref2_janitorNotRelocated_returnsPref() =
        assertEquals(2, decide(789, 2,
            charState = CharacterState(ascensionNumber = 3),
            prefs = prefs { setInt("relocatePygmyJanitor", 2) }))

    @Test fun case789_pref1_returnsPreference() = assertEquals(1, decide(789, 1))
    @Test fun case789_pref0_returnsNull() = assertNull(decide(789, 0))
}
