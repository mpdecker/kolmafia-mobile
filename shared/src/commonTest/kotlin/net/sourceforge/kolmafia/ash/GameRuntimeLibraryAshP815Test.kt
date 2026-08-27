package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DartPerksChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP815Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_invokesCheckDartPerks() {
        var called = false
        assertTrue(
            DartPerksChoiceSync.apply(1525) { called = true },
        )
        assertTrue(called)
    }

    @Test
    fun questChoiceRules_wires1525() {
        var called = false
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1525,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                preferences = prefs,
                checkDartPerks = { called = true },
            ),
        )
        assertTrue(called)
    }
}
