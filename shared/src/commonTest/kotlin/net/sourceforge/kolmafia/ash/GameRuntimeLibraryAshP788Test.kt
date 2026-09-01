package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CrimboPastChoiceSync

class GameRuntimeLibraryAshP788Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_marksSoldOutAndParsesSpecial() {
        val prefs = Preferences(MapSettings())
        val html = """
            <b>Daily Special:</b> descitem(12345) (7 knucklebones)
            Buy a prize turkey
            Buy medical gruel
        """.trimIndent()
        assertTrue(
            CrimboPastChoiceSync.applyVisit(
                choiceId = 1567,
                html = html,
                preferences = prefs,
                itemIdFromDesc = { if (it == "12345") 9999 else null },
            ),
        )
        assertTrue(prefs.getBoolean("_crimboPastSmokingPope", false))
        assertFalse(prefs.getBoolean("_crimboPastPrizeTurkey", true))
        assertFalse(prefs.getBoolean("_crimboPastMedicalGruel", true))
        assertFalse(prefs.getBoolean("_crimboPastDailySpecial", true))
        assertEquals(9999, prefs.getInt("_crimboPastDailySpecialItem", -1))
        assertEquals(7, prefs.getInt("_crimboPastDailySpecialPrice", 0))
    }

    @Test
    fun visit_allAvailableWhenBuyLinksPresent() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CrimboPastChoiceSync.applyVisit(
                1567,
                "Buy a Smoking Pope Buy a prize turkey Buy medical gruel Daily Special",
                prefs,
            ),
        )
        assertFalse(prefs.getBoolean("_crimboPastSmokingPope", true))
        assertFalse(prefs.getBoolean("_crimboPastPrizeTurkey", true))
        assertFalse(prefs.getBoolean("_crimboPastMedicalGruel", true))
        assertFalse(prefs.getBoolean("_crimboPastDailySpecial", true))
    }
}
