package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.GrimChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP809Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_setsGrimBuff() {
        val prefs = Preferences(MapSettings())
        assertTrue(GrimChoiceSync.apply(835, 1, prefs))
        assertEquals(true, prefs.getBoolean("_grimBuff", false))
    }

    @Test
    fun post_decision0_noop() {
        val prefs = Preferences(MapSettings())
        assertFalse(GrimChoiceSync.apply(835, 0, prefs))
        assertEquals(false, prefs.getBoolean("_grimBuff", false))
    }

    @Test
    fun questChoiceRules_wires835() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 835,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_grimBuff", false))
    }
}
