package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.EntauntaunedChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP793Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_decision1_setsToday() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            EntauntaunedChoiceSync.apply(
                choiceId = 1418,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_entauntaunedToday", false))
    }

    @Test
    fun post_otherDecision_noop() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            EntauntaunedChoiceSync.apply(
                choiceId = 1418,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals(false, prefs.getBoolean("_entauntaunedToday", false))
    }

    @Test
    fun questChoiceRules_wires1418() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1418,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_entauntaunedToday", false))
    }
}
