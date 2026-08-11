package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToppingPeakCombatSyncTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun applyCombatWin_lossVsOilSlick_noChange() {
        val prefs = prefs()
        prefs.setString("oilPeakProgress", "100.00")
        assertFalse(
            ToppingPeakCombatSync.applyCombatWin(
                preferences = prefs,
                monster = "oil slick",
                responseText = "",
                won = false,
            ),
        )
        assertEquals("100.00", prefs.getString("oilPeakProgress", "0"))
    }

    @Test
    fun applyCombatWin_winVsOilSlick_decrementsProgress() {
        val prefs = prefs()
        prefs.setString("oilPeakProgress", "100.00")
        assertTrue(
            ToppingPeakCombatSync.applyCombatWin(
                preferences = prefs,
                monster = "Oil Slick",
                responseText = "",
                won = true,
            ),
        )
        assertEquals("93.66", prefs.getString("oilPeakProgress", "0"))
    }

    @Test
    fun applyCombatWin_winVsOilBaronWithDressPants_appliesPantsBonus() {
        val prefs = prefs()
        prefs.setString("oilPeakProgress", "100.00")
        assertTrue(
            ToppingPeakCombatSync.applyCombatWin(
                preferences = prefs,
                monster = "oil baron",
                responseText = "",
                won = true,
                hasItemEquipped = { it == ToppingPeakCombatSync.DRESS_PANTS_ID },
            ),
        )
        assertEquals("61.96", prefs.getString("oilPeakProgress", "0"))
    }

    @Test
    fun applyCombatWin_winVsOilTycoonWithLovebug_appliesLovebugBonus() {
        val prefs = prefs()
        prefs.setString("oilPeakProgress", "100.00")
        assertTrue(
            ToppingPeakCombatSync.applyCombatWin(
                preferences = prefs,
                monster = "oil tycoon",
                responseText = "A love oil beetle trundles up and helps.",
                won = true,
            ),
        )
        assertEquals("74.64", prefs.getString("oilPeakProgress", "0"))
    }

    @Test
    fun applyCombatWin_winVsBooGhost_decrementsByTwo() {
        val prefs = prefs()
        prefs.setInt("booPeakProgress", 50)
        assertTrue(
            ToppingPeakCombatSync.applyCombatWin(
                preferences = prefs,
                monster = "Battlie Knight Ghost",
                responseText = "",
                won = true,
            ),
        )
        assertEquals(48, prefs.getInt("booPeakProgress", 0))
    }

    @Test
    fun applyCombatWin_nonPeakMonster_noChange() {
        val prefs = prefs()
        prefs.setString("oilPeakProgress", "50.00")
        prefs.setInt("booPeakProgress", 25)
        assertFalse(
            ToppingPeakCombatSync.applyCombatWin(
                preferences = prefs,
                monster = "orcish lumberjack",
                responseText = "",
                won = true,
            ),
        )
        assertEquals("50.00", prefs.getString("oilPeakProgress", "0"))
        assertEquals(25, prefs.getInt("booPeakProgress", 0))
    }
}
