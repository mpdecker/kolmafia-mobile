package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.MayamAvailability
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MayamChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP777Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun parse_usedSymbolsIncludingYams() {
        val prefs = Preferences(MapSettings())
        val html = """
            <img data-pos="0" class="used" alt="sword something">
            <img data-pos="1" class="" alt="yam something">
            <img data-pos="2" class="used" alt="yam something">
            <img data-pos="3" class="used" alt="clock something">
        """.trimIndent()
        assertTrue(MayamChoiceSync.applyVisit(1527, html, prefs))
        assertEquals("sword,yam2,clock", prefs.getString(MayamAvailability.SYMBOLS_USED_PREF, ""))
    }

    @Test
    fun questChoiceRules_wires1527() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1527,
                responseText = """<img data-pos="0" class="used" alt="eye foo">""",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals("eye", prefs.getString(MayamAvailability.SYMBOLS_USED_PREF, ""))
    }
}
