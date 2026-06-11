package net.sourceforge.kolmafia.adventure.choice.solvers

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VampOutSolverImplTest {

    private fun solver(): VampOutSolverImpl = VampOutSolverImpl(Preferences(MapSettings()))

    private fun prefsAndSolver(): Pair<Preferences, VampOutSolverImpl> {
        val p = Preferences(MapSettings())
        return Pair(p, VampOutSolverImpl(p))
    }

    // Step 0 — starting location selection
    private val allLocationsHtml = "Finally, the sun has set." +
        """<a href="choice.php?option=1">Visit Vlad's Boutique</a>""" +
        """<a href="choice.php?option=2">Visit Isabella's</a>""" +
        """<a href="choice.php?option=3">Visit The Masquerade</a>"""

    @Test fun step0_goal1_allAvailable_returnsVlad() =
        assertEquals(1, solver().autoVampOut(1, 0, allLocationsHtml))

    @Test fun step0_goal4_allAvailable_returnsIsabella() =
        assertEquals(2, solver().autoVampOut(4, 0, allLocationsHtml))

    @Test fun step0_goal8_allAvailable_returnsMasquerade() =
        assertEquals(3, solver().autoVampOut(8, 0, allLocationsHtml))

    // Only Isabella and Masquerade available (Vlad visited)
    private val noVladHtml = "Finally, the sun has set." +
        """<a href="choice.php?option=1">Visit Isabella's</a>""" +
        """<a href="choice.php?option=2">Visit The Masquerade</a>"""

    @Test fun step0_goal4_noVlad_returnsIsabellaAt1() =
        assertEquals(1, solver().autoVampOut(4, 0, noVladHtml))

    @Test fun step0_goal1_noVlad_fallsBackToFirstAvailable() =
        assertEquals(1, solver().autoVampOut(1, 0, noVladHtml))

    @Test fun step0_goal8_noVlad_returnsMasqueradeAt2() =
        assertEquals(2, solver().autoVampOut(8, 0, noVladHtml))

    @Test fun step0_allVisited_returnsNull() {
        val html = "Finally, the sun has set." +
            """<a href="choice.php?option=1">skip ahead</a>"""
        assertNull(solver().autoVampOut(1, 0, html))
    }

    @Test fun step0_setsInterviewPrefs() {
        val (prefs, s) = prefsAndSolver()
        s.autoVampOut(1, 0, allLocationsHtml)
        assertEquals(false, prefs.getBoolean(Preferences.INTERVIEW_VLAD))
        assertEquals(false, prefs.getBoolean(Preferences.INTERVIEW_ISABELLA))
        assertEquals(false, prefs.getBoolean(Preferences.INTERVIEW_MASQUERADE))
    }

    @Test fun step0_notStartingPage_usesScript() {
        assertNull(solver().autoVampOut(4, 0, "some other page without the sun text"))
    }

    // No option links at all — all visited
    private val noLocationsHtml = "Finally, the sun has set."

    @Test fun step0_allLocationsVisited_returnsNull() =
        assertNull(solver().autoVampOut(8, 0, noLocationsHtml))

    @Test fun goalZero_returnsNull() = assertNull(solver().autoVampOut(0, 0, ""))
    @Test fun goal14_returnsNull() = assertNull(solver().autoVampOut(14, 0, ""))

    @Test fun negativeStep_returnsNull() =
        assertNull(solver().autoVampOut(4, -1, ""))

    // Step > 0 — script lookup
    @Test fun step1_goal4_script011_returns1() =
        assertEquals(1, solver().autoVampOut(4, 1, "anything"))

    @Test fun step2_goal4_script011_returns1() =
        assertEquals(1, solver().autoVampOut(4, 2, "anything"))

    @Test fun step2_goal5_script0131_returns3() =
        assertEquals(3, solver().autoVampOut(5, 2, "anything"))

    // goal 8 script = "031241mtbv11"
    // index:           0123456789...
    // step4 → '4', step5 → '1', step6 → 'm' (Malkovich), step7 → 't' (Torremolinos)
    @Test fun step4_goal8_returns4() =
        assertEquals(4, solver().autoVampOut(8, 4, "irrelevant text"))

    @Test fun step5_goal8_returns1() =
        assertEquals(1, solver().autoVampOut(8, 5, "irrelevant text"))

    @Test fun step6_goal8_malkovich_findsOption() {
        val html = """<input name="option" value="3">Do the Malkovich thing"""
        assertEquals(3, solver().autoVampOut(8, 6, html))
    }

    @Test fun step7_goal8_torremolinos_findsOption() {
        val html = """<input name="option" value="1">Visit Torremolinos"""
        assertEquals(1, solver().autoVampOut(8, 7, html))
    }

    @Test fun step6_goal8_keywordMissing_returnsNull() =
        assertNull(solver().autoVampOut(8, 6, "no malkovich here"))

    @Test fun stepPastScript_returnsNull() =
        assertNull(solver().autoVampOut(4, 99, ""))

    @Test fun step0_scriptZero_fallsBackToFirstParsedChoice() {
        val html = """<input name="option" value="2">Pick me""" +
            """<input name="option" value="5">Or me"""
        assertEquals(2, solver().autoVampOut(4, 0, html))
    }
}
