package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SpoopyChoiceSync

class GameRuntimeLibraryAshP710Test {

    @Test
    fun revision_phase713() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun spoopy_boardsOnDecision5() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SpoopyChoiceSync.apply(
                1110,
                5,
                "You board up the doghouse and leave",
                prefs,
            ),
        )
        assertTrue(prefs.getBoolean("doghouseBoarded"))
    }

    @Test
    fun spoopy_unboardsOnPhrase() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("doghouseBoarded", true)
        assertTrue(
            SpoopyChoiceSync.apply(
                1110,
                5,
                "You unboard-up the doghouse",
                prefs,
            ),
        )
        assertFalse(prefs.getBoolean("doghouseBoarded"))
    }

    @Test
    fun spoopy_ignoresOtherDecision() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            SpoopyChoiceSync.apply(1110, 1, "You board up the doghouse", prefs),
        )
        assertFalse(prefs.getBoolean("doghouseBoarded"))
    }

    @Test
    fun questChoiceRules_wires1110() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1110,
                responseText = "You board up the doghouse",
                questDatabase = db,
                decision = 5,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("doghouseBoarded"))
    }
}
