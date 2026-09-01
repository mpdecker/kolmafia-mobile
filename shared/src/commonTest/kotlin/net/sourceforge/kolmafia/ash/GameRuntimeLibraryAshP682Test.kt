package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DinseyKioskChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP682Test {

    @Test
    fun revision_phase683() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun maint_disposesGarbageBag() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            DinseyKioskChoiceSync.apply(
                choiceId = DinseyKioskChoiceSync.MAINT_MISBEHAVIN,
                decision = 1,
                html = "You throw a bag of garbage into it",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(prefs.getBoolean("_dinseyGarbageDisposed"))
        assertTrue(consumed.contains(DinseyKioskChoiceSync.GARBAGE_BAG to 1))
    }

    @Test
    fun questChoiceRules_wires1067() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = DinseyKioskChoiceSync.MAINT_MISBEHAVIN,
                responseText = "You throw a bag of garbage into it and run.",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_dinseyGarbageDisposed"))
    }
}
