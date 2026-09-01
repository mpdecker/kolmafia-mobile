package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.TimeSpinnerChoiceSync

class GameRuntimeLibraryAshP802Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_prankIncrementsMinutes() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            TimeSpinnerChoiceSync.apply(
                choiceId = 1198,
                decision = 1,
                preferences = prefs,
                html = "You create a paradoxical time copy",
            ),
        )
        assertEquals(1, prefs.getInt(TimeSpinnerChoiceSync.MINUTES_PREF, 0))
    }

    @Test
    fun post_prankWithoutCopy_noop() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            TimeSpinnerChoiceSync.apply(
                choiceId = 1198,
                decision = 1,
                preferences = prefs,
                html = "nothing happened",
            ),
        )
    }

    @Test
    fun post_farFuture_replicator() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            TimeSpinnerChoiceSync.apply(
                choiceId = 1199,
                decision = 1,
                preferences = prefs,
                html = "An item appears in the replicator",
            ),
        )
        assertEquals(true, prefs.getBoolean("_timeSpinnerReplicatorUsed", false))
    }

    @Test
    fun post_farFuture_medals() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            TimeSpinnerChoiceSync.apply(
                choiceId = 1199,
                decision = 1,
                preferences = prefs,
                html = "memory of earning <b>12 medals",
            ),
        )
        assertEquals(12, prefs.getInt("timeSpinnerMedals", 0))
    }

    @Test
    fun questChoiceRules_wires1198() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1198,
                responseText = "paradoxical time copy acquired",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt(TimeSpinnerChoiceSync.MINUTES_PREF, 0))
    }
}
