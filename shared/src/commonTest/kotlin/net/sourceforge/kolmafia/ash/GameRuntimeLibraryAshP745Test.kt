package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.HorseryChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP745Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesHorseNamesAndCrazyStats() {
        val prefs = Preferences(MapSettings())
        val html = """
            <td valign=top class=small><b>Drab Teddy</b> the Normal Horse<P>
            <td valign=top class=small><b>Wacky Biggles</b> the Crazy Horse<P>
            Gives you +10% Muscle, -5% Mysticality, and +15% Moxie
            name=option value=1
            name=option value=2
            name=option value=3
            name=option value=4
        """.trimIndent()
        assertTrue(HorseryChoiceSync.applyVisit(1266, html, prefs))
        assertTrue(prefs.getBoolean("horseryAvailable"))
        assertEquals("Drab Teddy", prefs.getString("_horseryNormalName", ""))
        assertEquals("Wacky Biggles", prefs.getString("_horseryCrazyName", ""))
        assertEquals("+10", prefs.getString("_horseryCrazyMus", ""))
        assertEquals("-5", prefs.getString("_horseryCrazyMys", ""))
        assertEquals("+15", prefs.getString("_horseryCrazyMox", ""))
        assertEquals("", prefs.getString("_horsery", "x"))
    }

    @Test
    fun visit_infersCurrentHorseFromMissingOption() {
        val prefs = Preferences(MapSettings())
        val html = """
            name=option value=1
            name=option value=3
            name=option value=4
        """.trimIndent()
        assertTrue(HorseryChoiceSync.applyVisit(1266, html, prefs))
        assertEquals("dark horse", prefs.getString("_horsery", ""))
    }

    @Test
    fun post_rentSetsHorseAndName() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_horseryCrazyName", "Wacky Biggles")
        assertTrue(
            HorseryChoiceSync.apply(
                1266,
                3,
                "You rented the crazy horse!",
                prefs,
            ),
        )
        assertEquals("crazy horse", prefs.getString("_horsery", ""))
        assertEquals("Wacky Biggles", prefs.getString("_horseryCurrentName", ""))
    }

    @Test
    fun post_returnClearsHorse() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_horsery", "pale horse")
        prefs.setString("_horseryCurrentName", "Frightful")
        assertTrue(HorseryChoiceSync.apply(1266, 5, "", prefs))
        assertEquals("", prefs.getString("_horsery", "x"))
        assertEquals("", prefs.getString("_horseryCurrentName", "x"))
    }

    @Test
    fun questChoiceRules_wires1266() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_horseryPaleName", "Twiggy")
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1266,
                responseText = "You rent the pale horse!",
                questDatabase = db,
                decision = 4,
                preferences = prefs,
            ),
        )
        assertEquals("pale horse", prefs.getString("_horsery", ""))
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(HorseryChoiceSync.applyVisit(1410, "horsery", Preferences(MapSettings())))
    }
}
