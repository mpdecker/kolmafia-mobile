package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.DemonInCombatNameSync

class AlliedRadioRequestTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun parseVisitChoice_setsDropsUsedFromBattery() {
        val p = prefs()
        AlliedRadioRequest.parseVisitChoice(
            "Looks like you have enough battery left to make 2 calls today.",
            p,
        )
        assertEquals(1, p.getInt(Preferences.ALLIED_RADIO_DROPS_USED, 0))
    }

    @Test
    fun parsePostChoice_setsMaterielIntel() {
        val p = prefs()
        AlliedRadioRequest.parsePostChoice(
            html = "ok",
            handheld = false,
            request = "materiel intel",
            preferences = p,
            segmentSync = null,
        )
        assertTrue(p.getBoolean(Preferences.ALLIED_RADIO_MATERIEL_INTEL, false))
    }

    @Test
    fun parsePostChoice_setsWildsunBoon() {
        val p = prefs()
        AlliedRadioRequest.parsePostChoice(
            html = "ok",
            handheld = false,
            request = "wildsun boon",
            preferences = p,
            segmentSync = null,
        )
        assertTrue(p.getBoolean(Preferences.ALLIED_RADIO_WILDSUN_BOON, false))
    }

    @Test
    fun parsePostChoice_setsNoncombatForcerForSniperSupport() {
        val p = prefs()
        AlliedRadioRequest.parsePostChoice(
            html = "ok",
            handheld = true,
            request = "sniper support",
            preferences = p,
            segmentSync = null,
        )
        assertTrue(p.getBoolean(Preferences.NONCOMBAT_FORCER_ACTIVE, false))
    }

    @Test
    fun parsePostChoice_incrementsBackpackDropsUsed() {
        val p = prefs()
        AlliedRadioRequest.parsePostChoice(
            html = "Thanks for calling.",
            handheld = false,
            request = "fuel",
            preferences = p,
            segmentSync = null,
        )
        assertEquals(1, p.getInt(Preferences.ALLIED_RADIO_DROPS_USED, 0))
    }

    @Test
    fun parsePostChoice_skipsIncrementWhenPleaseRequestSomethingElse() {
        val p = prefs()
        AlliedRadioRequest.parsePostChoice(
            html = "Please request something else",
            handheld = false,
            request = "fuel",
            preferences = p,
            segmentSync = null,
        )
        assertEquals(0, p.getInt(Preferences.ALLIED_RADIO_DROPS_USED, 0))
    }

    @Test
    fun parsePostChoice_delegatesGreyTextToSegmentSync() {
        val p = prefs()
        val sync = DemonInCombatNameSync(p)
        val html = """static <i style='color: #999'>ulH</i> crackle"""
        val result = AlliedRadioRequest.parsePostChoice(
            html = html,
            handheld = true,
            request = "fuel",
            preferences = p,
            segmentSync = sync,
        )
        assertEquals(setOf("ulH"), sync.knownSegmentKeys())
        assertFalse(result.logMessages.any { it.contains("demons solve14") })
    }

    @Test
    fun parsePostChoice_logsNumberLetterPattern() {
        val p = prefs()
        val html = """voice saying <b>&quot;1654... S...&quot;</b>"""
        val result = AlliedRadioRequest.parsePostChoice(
            html = html,
            handheld = true,
            request = "fuel",
            preferences = p,
            segmentSync = null,
        )
        assertTrue(result.logMessages.any { it.contains("1654") && it.contains("S") })
    }
}
