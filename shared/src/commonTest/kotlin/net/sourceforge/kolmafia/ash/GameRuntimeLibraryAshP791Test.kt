package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CoolerYetiChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP791Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesBusyModes() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CoolerYetiChoiceSync.applyVisit(
                choiceId = 1560,
                html = "He's busy with a cooler. Something else.",
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_coolerYetiAdventures", false))
        assertEquals("adventures", prefs.getString("coolerYetiMode", ""))
    }

    @Test
    fun visit_coldDrinkClearsAdventuresFlag() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CoolerYetiChoiceSync.applyVisit(
                choiceId = 1560,
                html = "Make my next drink impossibly cold",
                preferences = prefs,
            ),
        )
        assertEquals(false, prefs.getBoolean("_coolerYetiAdventures", true))
        assertEquals("", prefs.getString("coolerYetiMode", "x"))
    }

    @Test
    fun post_decision2_setsAdventuresMode() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CoolerYetiChoiceSync.apply(
                choiceId = 1560,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_coolerYetiAdventures", false))
        assertEquals("adventures", prefs.getString("coolerYetiMode", ""))
    }

    @Test
    fun questChoiceRules_wires1560() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1560,
                responseText = "",
                questDatabase = QuestDatabase(prefs),
                decision = 4,
                preferences = prefs,
            ),
        )
        assertEquals("bar", prefs.getString("coolerYetiMode", ""))
    }
}
