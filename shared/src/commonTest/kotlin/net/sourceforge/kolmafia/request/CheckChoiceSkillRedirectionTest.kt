package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ChoiceCombatAshState

class CheckChoiceSkillRedirectionTest {

    @AfterTest
    fun tearDown() {
        CheckChoiceRedirection.resetForTest()
        CheckItemRedirection.clearItemMonster()
        ChoiceCombatAshState.reset()
    }

    @Test
    fun choice_scienceTent_setsPrefAndLabel() {
        val prefs = Preferences(MapSettings())
        ChoiceCombatAshState.lastChoice = 1201
        val result = CheckChoiceRedirection.apply("choice.php?whichchoice=1201", prefs, 1201)
        assertTrue(result.handled)
        assertEquals("Dr. Gordon Stuart's Science Tent", result.label)
        assertTrue(prefs.getBoolean("_eldritchTentacleFought", false))
        assertEquals("Dr. Gordon Stuart's Science Tent", CheckItemRedirection.itemMonster)
    }

    @Test
    fun skill_evokeEldritch_setsPref() {
        val prefs = Preferences(MapSettings())
        val result = CheckSkillRedirection.apply(
            "runskillz.php?whichskill=${CheckSkillRedirection.EVOKE_ELDRITCH_HORROR}",
            prefs,
        )
        assertTrue(result.handled)
        assertEquals("Evoke Eldritch Horror", result.label)
        assertTrue(prefs.getBoolean("_eldritchHorrorEvoked", false))
    }

    @Test
    fun skill_unknown_notHandled() {
        val result = CheckSkillRedirection.apply("runskillz.php?whichskill=1", Preferences(MapSettings()))
        assertFalse(result.handled)
    }
}
