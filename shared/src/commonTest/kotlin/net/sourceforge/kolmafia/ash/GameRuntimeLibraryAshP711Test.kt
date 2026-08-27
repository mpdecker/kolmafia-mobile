package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.SpoopyChoiceSync

class GameRuntimeLibraryAshP711Test {

    @Test
    fun revision_phase713() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_boardsWhenOptionMissing() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SpoopyChoiceSync.applyVisit(
                1110,
                "You already boarded it. Unboard the doghouse",
                prefs,
            ),
        )
        assertTrue(prefs.getBoolean("doghouseBoarded"))
    }

    @Test
    fun visit_unboardsWhenBoardOptionPresent() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("doghouseBoarded", true)
        assertTrue(
            SpoopyChoiceSync.applyVisit(
                1110,
                "Board up the doghouse",
                prefs,
            ),
        )
        assertFalse(prefs.getBoolean("doghouseBoarded"))
    }

    @Test
    fun visit_rejectsOtherChoice() {
        val prefs = Preferences(MapSettings())
        assertFalse(SpoopyChoiceSync.applyVisit(1118, "Board up the doghouse", prefs))
    }
}
