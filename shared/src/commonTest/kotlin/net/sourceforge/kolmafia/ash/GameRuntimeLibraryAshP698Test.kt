package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LostKeyChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP698Test {

    @Test
    fun revision_phase701() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun lostRoom_consumesKey() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            LostKeyChoiceSync.apply(
                choiceId = 594,
                html = "You acquire an item: <b>something</b>",
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.contains(LostKeyChoiceSync.LOST_KEY to 1))
    }

    @Test
    fun lostRoom_requiresAcquire() {
        assertFalse(LostKeyChoiceSync.apply(594, "the door stays shut"))
    }

    @Test
    fun questChoiceRules_wires594() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 594,
                responseText = "You acquire an item",
                questDatabase = db,
                preferences = prefs,
            ),
        )
    }
}
