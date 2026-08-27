package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DeckChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest

class GameRuntimeLibraryAshP732Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun random_incrementsOne() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(DeckChoiceSync.DRAWS_PREF, 2)
        assertTrue(DeckChoiceSync.apply(1085, 1, "", prefs))
        assertEquals(3, prefs.getInt(DeckChoiceSync.DRAWS_PREF, 0))
    }

    @Test
    fun cheat_incrementsFourCapped() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(DeckChoiceSync.DRAWS_PREF, 13)
        assertTrue(DeckChoiceSync.apply(1086, 1, "", prefs))
        assertEquals(15, prefs.getInt(DeckChoiceSync.DRAWS_PREF, 0))
    }

    @Test
    fun questChoiceRules_wires1085() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1085,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt(DeckChoiceSync.DRAWS_PREF, 0))
    }

    @Test
    fun ignoresCancelDecision() {
        val prefs = Preferences(MapSettings())
        assertFalse(DeckChoiceSync.apply(1085, 2, "", prefs))
    }
}

class GameRuntimeLibraryAshP738Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun parseAvailableCards_setsSeenFromMissingDropdown() {
        val prefs = Preferences(MapSettings())
        // Only XI - Strength available → all others are "seen"
        val html = """
            <select name="which">
              <option value="51">XI - Strength</option>
            </select>
        """.trimIndent()
        assertTrue(DeckOfEveryCardRequest.parseAvailableCards(html, prefs))
        val seen = prefs.getString(DeckChoiceSync.SEEN_PREF, "")
        assertTrue(seen.contains("0 - The Fool"))
        assertFalse(seen.contains("XI - Strength"))
    }

    @Test
    fun parseCardEncounter_appendsMunged() {
        val prefs = Preferences(MapSettings())
        val html = """<div id="blurb">You draw a card: <b>3 of Clubs</b><p>clubs</div>"""
        assertEquals("3 of Clubs", DeckOfEveryCardRequest.parseCardEncounter(html, prefs))
        assertEquals("X of Clubs", prefs.getString(DeckChoiceSync.SEEN_PREF, ""))
    }

    @Test
    fun visit_cheatWiresSeen() {
        val prefs = Preferences(MapSettings())
        val html = """
            <select name="which"><option value="51">XI - Strength</option></select>
        """.trimIndent()
        assertTrue(DeckChoiceSync.applyVisit(1086, html, prefs))
        assertTrue(prefs.getString(DeckChoiceSync.SEEN_PREF, "").isNotEmpty())
    }

    @Test
    fun questChoiceRules_wiresEncounterAppend() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val html = """<div id="blurb">You draw a card: <b>Dark Ritual</b><p>spell</div>"""
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1085,
                responseText = html,
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals("Dark Ritual", prefs.getString(DeckChoiceSync.SEEN_PREF, ""))
        assertEquals(1, prefs.getInt(DeckChoiceSync.DRAWS_PREF, 0))
    }
}
