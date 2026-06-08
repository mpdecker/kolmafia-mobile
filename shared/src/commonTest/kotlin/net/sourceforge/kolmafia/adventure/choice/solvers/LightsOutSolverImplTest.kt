package net.sourceforge.kolmafia.adventure.choice.solvers

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LightsOutSolverImplTest {

    private fun solver(automation: Int): LightsOutSolverImpl {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lightsOutAutomation", automation)
        return LightsOutSolverImpl(prefs)
    }

    @Test fun disabled_returnsNull() =
        assertNull(solver(0).autoLightsOut(890, "Look Out the Window"))

    @Test fun mode2_room890_noGhostPresent_returns1() =
        assertEquals(1, solver(2).autoLightsOut(890, "no ghost here"))

    @Test fun mode1_room890_ghostOptionPresent_returns3() =
        assertEquals(3, solver(1).autoLightsOut(890, "Look Out the Window"))

    @Test fun mode1_room890_noGhostOption_returns1() =
        assertEquals(1, solver(1).autoLightsOut(890, "no matching text"))

    @Test fun mode1_room893_ghostOptionPresent_returns4() =
        assertEquals(4, solver(1).autoLightsOut(893, "Make a Snack"))

    @Test fun mode1_room893_noGhostOption_returns1() =
        assertEquals(1, solver(1).autoLightsOut(893, "no matching text"))

    @Test fun mode1_room894_ghostOptionPresent_returns2() =
        assertEquals(2, solver(1).autoLightsOut(894, "Go to the Children's Section"))

    @Test fun mode1_room897_searchForLight_returns1() =
        assertEquals(1, solver(1).autoLightsOut(897, "Search for a light"))
    @Test fun mode2_room897_searchForLight_returns2() =
        assertEquals(2, solver(2).autoLightsOut(897, "Search for a light"))
    @Test fun mode1_room897_searchNightstand_returns3() =
        assertEquals(3, solver(1).autoLightsOut(897, "Search a nearby nightstand"))
    @Test fun mode1_room897_checkNightstandLeft_returns1() =
        assertEquals(1, solver(1).autoLightsOut(897, "Check a nightstand on your left"))

    @Test fun mode1_room901_tryToFindLight_returns1() =
        assertEquals(1, solver(1).autoLightsOut(901, "Try to find a light"))
    @Test fun mode1_room901_keepCool_returns2() =
        assertEquals(2, solver(1).autoLightsOut(901, "Keep your cool"))
    @Test fun mode1_room901_pinot_returns3() =
        assertEquals(3, solver(1).autoLightsOut(901, "Examine the Pinot Noir rack"))

    @Test fun mode1_room903_searchForLight_returns1() =
        assertEquals(1, solver(1).autoLightsOut(903, "Search for a light"))
    @Test fun mode1_room903_checkItOut_returns1() =
        assertEquals(1, solver(1).autoLightsOut(903, "Check it out"))
    @Test fun mode1_room903_weirdMachines_returns3() =
        assertEquals(3, solver(1).autoLightsOut(903, "Examine the weird machines"))
    @Test fun mode1_room903_ohGod_returns1() =
        assertEquals(1, solver(1).autoLightsOut(903, "Oh god"))

    @Test fun unknownRoom_returns2() =
        assertEquals(2, solver(1).autoLightsOut(999, "anything"))
}
