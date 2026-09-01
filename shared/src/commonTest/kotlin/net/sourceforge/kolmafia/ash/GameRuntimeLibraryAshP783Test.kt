package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LeprecondoChoiceSync

class GameRuntimeLibraryAshP783Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesInstalledAndRearrangements() {
        val prefs = Preferences(MapSettings())
        val html = """
            <img id="i0" alt="beer cooler in top left">
            <img id="i1" alt="free mattress in top right">
            <img id="i2" alt="empty slot in bottom left">
            <img id="i3" alt="Omnipot in bottom right">
            You can rearrange the furnishings 2 more times today.
            <select id="r1" name="r1">
              <option value='5'><option selected value='6'><option value='25'>
            </select>
        """.trimIndent()
        assertTrue(LeprecondoChoiceSync.applyVisit(1556, html, prefs))
        assertEquals("5,6,0,25", prefs.getString("leprecondoInstalled", ""))
        assertEquals(1, prefs.getInt("_leprecondoRearrangements", -1))
        assertEquals("5,6,25", prefs.getString("leprecondoDiscovered", ""))
    }

    @Test
    fun visit_withoutRearrangementsStillSetsInstalled() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            LeprecondoChoiceSync.applyVisit(
                1556,
                """<img id="i0" alt="whiskeybed in top left">""",
                prefs,
            ),
        )
        assertEquals("21", prefs.getString("leprecondoInstalled", ""))
    }
}
