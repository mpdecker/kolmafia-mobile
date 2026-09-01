package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DigGiftChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP790Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_logsNote() {
        val prefs = Preferences(MapSettings())
        val logs = mutableListOf<String>()
        val html = """
            Looks like they left a note: <div style="padding: 1em; margin: 1em; border: 1px solid black">Hello digger</div>
        """.trimIndent()
        assertTrue(
            DigGiftChoiceSync.apply(
                choiceId = 1591,
                decision = 1,
                html = html,
                preferences = prefs,
                sessionLog = { logs += it },
            ),
        )
        assertEquals(listOf("Note: Hello digger"), logs)
    }

    @Test
    fun questChoiceRules_wires1591() {
        val prefs = Preferences(MapSettings())
        val logs = mutableListOf<String>()
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1591,
                responseText = """Looks like they left a note: <div style="padding: 1em; margin: 1em; border: 1px solid black">Gift note</div>""",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
                sessionLog = { logs += it },
            ),
        )
        assertEquals(listOf("Note: Gift note"), logs)
    }
}
