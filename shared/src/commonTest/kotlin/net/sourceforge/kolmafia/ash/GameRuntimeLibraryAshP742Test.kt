package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BaseballChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.track.TrackManager

class GameRuntimeLibraryAshP742Test {

    @Test
    fun visit_incrementsInningsWithoutStrike() {
        val prefs = Preferences(MapSettings())
        assertTrue(BaseballChoiceSync.applyVisit(1598, "Play Ball!", prefs))
        assertEquals(1, prefs.getInt(BaseballChoiceSync.INNINGS_PREF, 0))
        assertFalse(BaseballChoiceSync.applyVisit(1598, "Already threw at <s>the Foo</s>", prefs))
    }

    @Test
    fun post_skullball() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            BaseballChoiceSync.apply(
                choiceId = 1598,
                decision = 1,
                html = """<div id="output">You draw a skull on the ball. <s>the Foo</s></div>""",
                preferences = prefs,
            ),
        )
        assertEquals("Foo", prefs.getString("_skullballMonster", ""))
    }

    @Test
    fun post_banishIce() {
        val prefs = Preferences(MapSettings())
        var banished: String? = null
        assertTrue(
            BaseballChoiceSync.apply(
                choiceId = 1598,
                decision = 2,
                html = """<div id="output">Instead of a baseball, you throw a big handful of ice. <s>the Bar</s></div>""",
                preferences = prefs,
                banishMonster = { name, banisher, _ ->
                    banished = name
                    assertEquals(Banisher.BASEBALL_DIAMOND, banisher)
                },
            ),
        )
        assertEquals("Bar", banished)
    }

    @Test
    fun post_trackCheese() {
        val prefs = Preferences(MapSettings())
        var tracked: String? = null
        assertTrue(
            BaseballChoiceSync.apply(
                choiceId = 1598,
                decision = 3,
                html = """<div id="output">You coat the ball in some melted cheddar cheese. <s>the Baz</s></div>""",
                preferences = prefs,
                trackMonster = { name, tracker, _ ->
                    tracked = name
                    assertEquals(TrackManager.Tracker.BASEBALL_DIAMOND, tracker)
                },
            ),
        )
        assertEquals("Baz", tracked)
    }

    @Test
    fun questChoiceRules_wires1598() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1598,
                responseText = """<div id="output">You throw a screwball. <s>the Qux</s></div>""",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals("Qux", prefs.getString("_screwballMonster", ""))
    }
}
