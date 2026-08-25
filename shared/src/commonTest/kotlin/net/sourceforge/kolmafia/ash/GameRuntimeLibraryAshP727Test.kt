package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MonorailChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP727Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_setsMuffinOnOrder() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            MonorailChoiceSync.applyVisit(
                1308,
                "Looks like your order for a blueberry muffin is not yet ready.",
                prefs,
            ),
        )
        assertEquals("blueberry muffin", prefs.getString("muffinOnOrder", ""))
    }

    @Test
    fun visit_clearsWhenOrderButtonsPresent() {
        val prefs = Preferences(MapSettings())
        prefs.setString("muffinOnOrder", "blueberry muffin")
        assertTrue(
            MonorailChoiceSync.applyVisit(
                1308,
                "Order a blueberry muffin",
                prefs,
            ),
        )
        assertEquals("none", prefs.getString("muffinOnOrder", ""))
    }

    @Test
    fun post_setsOrderedTodayAndConsumesTin() {
        val prefs = Preferences(MapSettings())
        var consumedId = -1
        assertTrue(
            MonorailChoiceSync.apply(
                choiceId = 1308,
                html = "Your muffin is not yet ready.",
                preferences = prefs,
                visitHtml = "Order a blueberry muffin",
                consumeItem = { id, _ -> consumedId = id },
            ),
        )
        assertTrue(prefs.getBoolean("_muffinOrderedToday"))
        assertEquals(MonorailChoiceSync.EARTHENWARE_MUFFIN_TIN, consumedId)
    }

    @Test
    fun questChoiceRules_wires1308() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1308,
                responseText = "Looks like your order for a bran muffin is not yet ready.",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals("bran muffin", prefs.getString("muffinOnOrder", ""))
        assertTrue(prefs.getBoolean("_muffinOrderedToday"))
    }

    @Test
    fun ignoresOtherChoices() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            MonorailChoiceSync.apply(
                choiceId = 1219,
                html = "muffin is not yet ready",
                preferences = prefs,
            ),
        )
    }
}
