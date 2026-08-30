package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FireStartingKitChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP703Test {

    @Test
    fun revision_phase707() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun fireKit_setsPrefAndConsumesOnPhrase() {
        val prefs = Preferences(MapSettings())
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            FireStartingKitChoiceSync.apply(
                choiceId = 595,
                html = "You succeed by rubbing the two stupid sticks together",
                preferences = prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(prefs.getBoolean("_fireStartingKitUsed"))
        assertTrue(consumed.contains(FireStartingKitChoiceSync.CSA_FIRE_STARTING_KIT to 1))
    }

    @Test
    fun fireKit_setsPrefWithoutConsume() {
        val prefs = Preferences(MapSettings())
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            FireStartingKitChoiceSync.apply(
                choiceId = 595,
                html = "you walk away",
                preferences = prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(prefs.getBoolean("_fireStartingKitUsed"))
        assertTrue(consumed.isEmpty())
    }

    @Test
    fun questChoiceRules_wires595() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 595,
                responseText = "pile the sticks up on top of the briefcase",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_fireStartingKitUsed"))
    }
}
