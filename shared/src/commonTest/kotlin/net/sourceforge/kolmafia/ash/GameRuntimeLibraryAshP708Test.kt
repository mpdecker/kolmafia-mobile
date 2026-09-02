package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.GnomePartChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP708Test {

    @Test
    fun revision_phase713() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun gnomePart_setsPrefAndRefreshes() {
        val prefs = Preferences(MapSettings())
        var refreshed = 0
        assertTrue(
            GnomePartChoiceSync.apply(
                choiceId = 597,
                preferences = prefs,
                refreshConcoctions = { refreshed++ },
            ),
        )
        assertTrue(prefs.getBoolean("_gnomePart"))
        assertEquals(1, refreshed)
    }

    @Test
    fun gnomePart_rejectsOtherChoice() {
        val prefs = Preferences(MapSettings())
        assertFalse(GnomePartChoiceSync.apply(596, prefs) { })
        assertFalse(prefs.getBoolean("_gnomePart"))
    }

    @Test
    fun questChoiceRules_wires597() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 597,
                responseText = "",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_gnomePart"))
    }
}
