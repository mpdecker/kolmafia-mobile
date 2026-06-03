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

class ResponseTextHandlersTest {

    private val prefs = Preferences(MapSettings())
    private val noOpSolvers = ChoiceSolvers(
        SafetyShelterSolver.NoOp, VampOutSolver.NoOp, ArcadeGameSolver.NoOp,
        LostKeySolver.NoOp, GameproSolver.NoOp, LightsOutSolver.NoOp,
    )

    private fun ctx(
        choiceId: Int,
        preference: Int = 0,
        response: String = "",
    ) = ChoiceContext(
        choiceId = choiceId, options = (1..6).associateWith { "O$it" },
        responseText = response, characterState = CharacterState(),
        inventoryState = InventoryState(), effectState = EffectState(),
        skillState = SkillState(), preferences = prefs,
        goalManager = GoalManager(), questDatabase = QuestDatabase(prefs),
        solvers = noOpSolvers, preference = preference,
    )

    private fun decide(choiceId: Int, preference: Int = 0, response: String = "") =
        ResponseTextHandlers.handlers[choiceId]?.decide(ctx(choiceId, preference, response))

    // Case 155
    @Test fun case155_pref4_shinyAbsent_returns5() = assertEquals(5, decide(155, 4, "nothing"))
    @Test fun case155_pref4_shinyPresent_returnsPref() = assertEquals(4, decide(155, 4, "Check the shiny object"))
    @Test fun case155_pref2_returnsPreference() = assertEquals(2, decide(155, 2, "nothing"))

    // Case 575
    @Test fun case575_pref2_digAbsent_returns3() = assertEquals(3, decide(575, 2, "nothing"))
    @Test fun case575_pref2_digPresent_returnsPref() = assertEquals(2, decide(575, 2, "Dig deeper"))
    @Test fun case575_pref0_returnsNull() = assertNull(decide(575, 0))

    // Case 678
    @Test fun case678_pref3_trashAbsent_returnsNull() = assertNull(decide(678, 3, "nothing"))
    @Test fun case678_pref3_trashPresent_returnsPref() = assertEquals(3, decide(678, 3, "Check behind the trash can"))
    @Test fun case678_pref1_returnsPreference() = assertEquals(1, decide(678, 1))

    // Case 705
    @Test fun case705_pref2_janitorAbsent_returnsNull() = assertNull(decide(705, 2, "nothing"))
    @Test fun case705_pref2_janitorPresent_returnsPref() = assertEquals(2, decide(705, 2, "Go to the janitor's closet"))
    @Test fun case705_pref3_bathroomAbsent_returnsNull() = assertNull(decide(705, 3, "nothing"))
    @Test fun case705_pref4_loungeAbsent_returnsNull() = assertNull(decide(705, 4, "nothing"))
    @Test fun case705_pref1_returnsPreference() = assertEquals(1, decide(705, 1))

    // Case 808
    @Test fun case808_pref2_nightstandAbsent_returnsNull() = assertNull(decide(808, 2, "nothing"))
    @Test fun case808_pref2_nightstandPresent_returnsPref() =
        assertEquals(2, decide(808, 2, "nightstand wasn't here before"))
    @Test fun case808_pref1_returnsPreference() = assertEquals(1, decide(808, 1))

    // Case 919
    @Test fun case919_pref1_alreadyThoroughly_returns6() =
        assertEquals(6, decide(919, 1, "You've already thoroughly"))
    @Test fun case919_pref1_normal_returnsPref() = assertEquals(1, decide(919, 1, "nothing"))

    // Case 923
    @Test fun case923_pref2_blacksmithAbsent_returnsNull() = assertNull(decide(923, 2, "nothing"))
    @Test fun case923_pref2_blacksmithPresent_returnsPref() =
        assertEquals(2, decide(923, 2, "Visit the blacksmith's cottage"))
    @Test fun case923_pref3_mineAbsent_returnsNull() = assertNull(decide(923, 3, "nothing"))
    @Test fun case923_pref4_churchAbsent_returnsNull() = assertNull(decide(923, 4, "nothing"))

    // Case 973
    @Test fun case973_pref2_hoochAbsent_returns6() = assertEquals(6, decide(973, 2, "nothing"))
    @Test fun case973_pref2_hoochPresent_returnsPref() = assertEquals(2, decide(973, 2, "Turn in Hooch"))

    // Case 975
    @Test fun case975_onionsAbsent_returns2() = assertEquals(2, decide(975, 1, "nothing"))
    @Test fun case975_onionsPresent_returnsPref() = assertEquals(1, decide(975, 1, "Stick in the onions"))

    // Case 1026
    @Test fun case1026_pref2_drawerAbsent_returns3() = assertEquals(3, decide(1026, 2, "nothing"))
    @Test fun case1026_pref2_drawerPresent_returnsPref() =
        assertEquals(2, decide(1026, 2, "Investigate the noisy drawer"))

    // Case 1222
    @Test fun case1222_alreadyGone_returns2() =
        assertEquals(2, decide(1222, 1, "You've already gone through the Tunnel once today"))
    @Test fun case1222_normal_returnsPref() = assertEquals(1, decide(1222, 1, "nothing"))

    // Case 1461
    @Test fun case1461_grabCheerCore_returns5() = assertEquals(5, decide(1461, 1, "Grab the Cheer Core!"))
    @Test fun case1461_normal_returnsPref() = assertEquals(1, decide(1461, 1, "nothing"))
}
