package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.RedSnapperChoiceSync
import net.sourceforge.kolmafia.track.TrackManager

class GameRuntimeLibraryAshP805Test {

    @Test
    fun revision_phase826() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesProgress() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            RedSnapperChoiceSync.applyVisit(
                choiceId = 1396,
                html = "guiding you towards: <b>fish</b>.  You've found <b>4</b> of them",
                preferences = prefs,
                currentTurn = 10,
            ),
        )
        assertEquals(4, prefs.getInt("redSnapperProgress", 0))
        assertEquals("fish", prefs.getString("redSnapperPhylum", ""))
        assertTrue(
            TrackManager.loadEntries(prefs, TrackManager.PREF_TRACKED_PHYLA)
                .any { it.tracked == "fish" && it.tracker == TrackManager.Tracker.RED_SNAPPER },
        )
    }

    @Test
    fun post_catUrl_resetsProgress() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("redSnapperProgress", 5)
        assertTrue(
            RedSnapperChoiceSync.apply(
                choiceId = 1396,
                preferences = prefs,
                choiceUrl = "choice.php?whichchoice=1396&option=1&cat=merkin",
            ),
        )
        assertEquals("mer-kin", prefs.getString("redSnapperPhylum", ""))
        assertEquals(0, prefs.getInt("redSnapperProgress", -1))
    }

    @Test
    fun questChoiceRules_wires1396() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1396,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
                choiceUrl = "choice.php?option=1&cat=bug",
            ),
        )
        assertEquals("bug", prefs.getString("redSnapperPhylum", ""))
    }
}
