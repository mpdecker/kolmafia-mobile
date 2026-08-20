package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LightsOutChoiceSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestItemRules
import net.sourceforge.kolmafia.session.TurnCounter

class GameRuntimeLibraryAshP687Test {

    @Test
    fun revision_phase689() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_stopsCounterAndSetsLastTurn() {
        val prefs = Preferences(MapSettings())
        TurnCounter.startCounting(prefs, 10, 5, LightsOutChoiceSync.COUNTER_LABEL, "lightsout.gif")
        assertTrue(LightsOutChoiceSync.applyVisit(890, prefs, 42))
        assertEquals(42, prefs.getInt(LightsOutChoiceSync.LAST_TURN_PREF))
        assertTrue(
            TurnCounter.load(prefs).none {
                it.parsedLabel().equals(LightsOutChoiceSync.COUNTER_LABEL, ignoreCase = true)
            },
        )
    }

    @Test
    fun dollie_setsElizabethNone() {
        val prefs = Preferences(MapSettings())
        prefs.setString(LightsOutChoiceSync.ELIZABETH_PREF, "The Haunted Gallery")
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestItemRules.applyItemsGained(
                itemsGained = listOf("Elizabeth's Dollie"),
                questDatabase = db,
                preferences = prefs,
                itemIdsGained = listOf(QuestItemRules.ELIZABETH_DOLLIE),
            ),
        )
        assertEquals("none", prefs.getString(LightsOutChoiceSync.ELIZABETH_PREF))
    }

    @Test
    fun labCoat_setsStephenNone() {
        val prefs = Preferences(MapSettings())
        prefs.setString(LightsOutChoiceSync.STEPHEN_PREF, "The Haunted Laboratory")
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestItemRules.applyItemsGained(
                itemsGained = emptyList(),
                questDatabase = db,
                preferences = prefs,
                itemIdsGained = listOf(QuestItemRules.STEPHEN_LAB_COAT),
            ),
        )
        assertEquals("none", prefs.getString(LightsOutChoiceSync.STEPHEN_PREF))
    }
}
