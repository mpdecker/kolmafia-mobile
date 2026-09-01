package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.WildfireCaptainChoiceSync

class GameRuntimeLibraryAshP759Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun refill_setsCharge() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            WildfireCaptainChoiceSync.apply(
                1451,
                3,
                "Hagnk takes your fire extinguisher and fills it",
                prefs,
            ),
        )
        assertEquals(100, prefs.getInt(WildfireCaptainChoiceSync.CHARGE_PREF, 0))
        assertTrue(prefs.getBoolean(WildfireCaptainChoiceSync.REFILLED_PREF))
    }

    @Test
    fun reduceFire_updatesPrefMap() {
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.WILDFIRE_FIRE_LEVELS, "240=5")
        assertTrue(
            WildfireCaptainChoiceSync.apply(
                1451,
                1,
                """<option value="240">Somewhere (Fire: 4)</option>""",
                prefs,
                choiceUrl = "choice.php?whichchoice=1451&option=1&zid=240",
            ),
        )
        assertTrue(prefs.getString(Preferences.WILDFIRE_FIRE_LEVELS, "").contains("240=4"))
    }

    @Test
    fun questChoiceRules_wires1451() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1451,
                responseText = "Hagnk takes your fire extinguisher",
                questDatabase = db,
                decision = 3,
                preferences = prefs,
            ),
        )
        assertEquals(100, prefs.getInt(WildfireCaptainChoiceSync.CHARGE_PREF, 0))
    }

    @Test
    fun ignoresOtherChoices() {
        assertFalse(
            WildfireCaptainChoiceSync.apply(1452, 3, "Hagnk takes your fire extinguisher", Preferences(MapSettings())),
        )
    }
}
