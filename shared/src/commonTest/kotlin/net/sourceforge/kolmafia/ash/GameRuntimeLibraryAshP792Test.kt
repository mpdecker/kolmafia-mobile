package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LockPickedChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP792Test {

    @Test
    fun revision_phase826() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_setsLockPicked() {
        val prefs = Preferences(MapSettings())
        assertTrue(LockPickedChoiceSync.apply(choiceId = 1414, preferences = prefs))
        assertEquals(true, prefs.getBoolean("lockPicked", false))
    }

    @Test
    fun questChoiceRules_wires1414() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1414,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("lockPicked", false))
    }
}
