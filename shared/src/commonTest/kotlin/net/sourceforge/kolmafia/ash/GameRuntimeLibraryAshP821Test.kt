package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LanguageFluencyChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP821Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun landHo_parsesFluency() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            LanguageFluencyChoiceSync.apply(
                choiceId = 1246,
                decision = 1,
                html = "Fluency is now 40%",
                preferences = prefs,
            ),
        )
        assertEquals(40, prefs.getInt("spacePirateLanguageFluency", 0))
    }

    @Test
    fun halfShip_acquireResetsFluency() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("spacePirateLanguageFluency", 80)
        assertTrue(
            LanguageFluencyChoiceSync.apply(
                choiceId = 1247,
                decision = 1,
                html = "You acquire an item: treasure",
                preferences = prefs,
            ),
        )
        assertEquals(0, prefs.getInt("spacePirateLanguageFluency", -1))
    }

    @Test
    fun paradise_consumesMap() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            LanguageFluencyChoiceSync.apply(
                choiceId = 1248,
                decision = 1,
                html = "You acquire an item: loot",
                preferences = Preferences(MapSettings()),
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(LanguageFluencyChoiceSync.SPACE_PIRATE_TREASURE_MAP to 1), consumed)
    }

    @Test
    fun questChoiceRules_wires1246() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1246,
                responseText = "Fluency is now 10%",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(10, prefs.getInt("spacePirateLanguageFluency", 0))
    }

    @Test
    fun landHo_decision0_noop() {
        assertFalse(
            LanguageFluencyChoiceSync.apply(
                choiceId = 1246,
                decision = 0,
                html = "Fluency is now 10%",
                preferences = Preferences(MapSettings()),
            ),
        )
    }
}
