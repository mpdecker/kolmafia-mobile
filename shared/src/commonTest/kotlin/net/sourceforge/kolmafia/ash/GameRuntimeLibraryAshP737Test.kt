package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ArchSpadeChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP737Test {

    @Test
    fun revision_phase814() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesRemainingDigs() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            ArchSpadeChoiceSync.applyVisit(
                1596,
                "You still have the wherewithal to dig <b>7</b> more times today.",
                prefs,
            ),
        )
        assertEquals(4, prefs.getInt(ArchSpadeChoiceSync.DIGS_PREF, 0))
    }

    @Test
    fun postChoice_incrementsUnlessLeave() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(ArchSpadeChoiceSync.DIGS_PREF, 2)
        assertTrue(ArchSpadeChoiceSync.apply(1596, 1, prefs))
        assertEquals(3, prefs.getInt(ArchSpadeChoiceSync.DIGS_PREF, 0))
        assertFalse(ArchSpadeChoiceSync.apply(1596, 4, prefs))
        assertEquals(3, prefs.getInt(ArchSpadeChoiceSync.DIGS_PREF, 0))
    }

    @Test
    fun questChoiceRules_wires1596() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1596,
                responseText = "",
                questDatabase = db,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt(ArchSpadeChoiceSync.DIGS_PREF, 0))
    }

    @Test
    fun ignoresOtherChoices() {
        val prefs = Preferences(MapSettings())
        assertFalse(ArchSpadeChoiceSync.apply(1219, 1, prefs))
    }
}
