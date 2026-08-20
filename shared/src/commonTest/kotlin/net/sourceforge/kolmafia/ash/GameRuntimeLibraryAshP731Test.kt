package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BatfellowUpgradeChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP731Test {

    @Test
    fun revision_phase743() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun suitUpgrade_addsAndDebitsFunds() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(BatfellowUpgradeChoiceSync.FUNDS_PREF, 3)
        assertTrue(
            BatfellowUpgradeChoiceSync.apply(
                choiceId = 1137,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals("Hardened Knuckles", prefs.getString(BatfellowUpgradeChoiceSync.UPGRADES_PREF, ""))
        assertEquals(2, prefs.getInt(BatfellowUpgradeChoiceSync.FUNDS_PREF, 0))
    }

    @Test
    fun sedanUpgrade_skipsDuplicate() {
        val prefs = Preferences(MapSettings())
        prefs.setString(BatfellowUpgradeChoiceSync.UPGRADES_PREF, "Spotlight")
        prefs.setInt(BatfellowUpgradeChoiceSync.FUNDS_PREF, 5)
        assertFalse(
            BatfellowUpgradeChoiceSync.apply(
                choiceId = 1138,
                decision = 6,
                preferences = prefs,
            ),
        )
        assertEquals(5, prefs.getInt(BatfellowUpgradeChoiceSync.FUNDS_PREF, 0))
    }

    @Test
    fun cavernUpgrade_appends() {
        val prefs = Preferences(MapSettings())
        prefs.setString(BatfellowUpgradeChoiceSync.UPGRADES_PREF, "Hardened Knuckles")
        assertTrue(
            BatfellowUpgradeChoiceSync.apply(
                choiceId = 1139,
                decision = 7,
                preferences = prefs,
            ),
        )
        assertEquals(
            "Hardened Knuckles;Snugglybear Nightlight",
            prefs.getString(BatfellowUpgradeChoiceSync.UPGRADES_PREF, ""),
        )
    }

    @Test
    fun questChoiceRules_wires1137() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1137,
                responseText = "",
                questDatabase = db,
                decision = 3,
                preferences = prefs,
            ),
        )
        assertEquals(
            "Extra-Swishy Cloak",
            prefs.getString(BatfellowUpgradeChoiceSync.UPGRADES_PREF, ""),
        )
    }

    @Test
    fun ignoresOtherChoices() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            BatfellowUpgradeChoiceSync.apply(
                choiceId = 1133,
                decision = 1,
                preferences = prefs,
            ),
        )
    }
}
