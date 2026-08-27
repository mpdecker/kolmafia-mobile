package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LanguageFluencyChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP823Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun timeEnough_decision1_acknowledged() {
        assertTrue(
            LanguageFluencyChoiceSync.apply(
                choiceId = 1252,
                decision = 1,
                html = "",
                preferences = Preferences(MapSettings()),
            ),
        )
    }

    @Test
    fun motherMayI_resetsBabyFluency() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("spaceBabyLanguageFluency", 90)
        assertTrue(
            LanguageFluencyChoiceSync.apply(
                choiceId = 1253,
                decision = 1,
                html = "You acquire an item: book",
                preferences = prefs,
            ),
        )
        assertEquals(0, prefs.getInt("spaceBabyLanguageFluency", -1))
    }

    @Test
    fun pleaseBaby_consumesBawbaw() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            LanguageFluencyChoiceSync.apply(
                choiceId = 1254,
                decision = 1,
                html = "You acquire an item: toy",
                preferences = Preferences(MapSettings()),
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(LanguageFluencyChoiceSync.SPACE_BABY_BAWBAW to 1), consumed)
    }

    @Test
    fun questChoiceRules_wires1253() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("spaceBabyLanguageFluency", 10)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1253,
                responseText = "You acquire an item: x",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(0, prefs.getInt("spaceBabyLanguageFluency", -1))
    }
}
