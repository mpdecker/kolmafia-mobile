package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class SummoningChamberRequestTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun parseResponse_ignoresUnrelatedLocation() {
        val p = prefs()
        val result = SummoningChamberRequest.parseResponse(
            "adventure.php?snarfblat=1",
            "You light three black candles",
            p,
        )
        assertFalse(result.setDemonSummoned)
        assertFalse(result.consumeSummoningItems)
        assertFalse(p.getBoolean(Preferences.DEMON_SUMMONED, false))
    }

    @Test
    fun parseResponse_rechargeSetsDemonSummoned() {
        val p = prefs()
        val location = SummoningChamberRequest.buildLocation("Ak'gyxoth")
        val result = SummoningChamberRequest.parseResponse(
            location,
            "greasy static-electricity feel",
            p,
        )
        assertTrue(result.setDemonSummoned)
        assertTrue(p.getBoolean(Preferences.DEMON_SUMMONED, false))
    }

    @Test
    fun parseResponse_candleSuccessConsumesAndSetsFlag() {
        val p = prefs()
        val location = SummoningChamberRequest.buildLocation("Ak'gyxoth")
        val result = SummoningChamberRequest.parseResponse(
            location,
            "You light three black candles and read from the scroll.",
            p,
        )
        assertTrue(result.setDemonSummoned)
        assertTrue(result.consumeSummoningItems)
        assertTrue(p.getBoolean(Preferences.DEMON_SUMMONED, false))
    }

    @Test
    fun parseResponse_candleCrossedSignalDoesNotSetFlag() {
        val p = prefs()
        val location = SummoningChamberRequest.buildLocation("Ak'gyxoth")
        val result = SummoningChamberRequest.parseResponse(
            location,
            "You light three black candles. some sort of crossed signal happens.",
            p,
        )
        assertFalse(result.setDemonSummoned)
        assertTrue(result.consumeSummoningItems)
        assertFalse(p.getBoolean(Preferences.DEMON_SUMMONED, false))
    }

    @Test
    fun parseResponse_shubInternetPath() {
        val p = prefs()
        val location = SummoningChamberRequest.buildLocation("Neil Ak'gyxoth roa")
        val result = SummoningChamberRequest.parseResponse(
            location,
            "Great Old One Shub-Internet appears.",
            p,
        )
        assertTrue(result.setDemonSummoned)
        assertTrue(result.consumeSummoningItems)
    }

    @Test
    fun parseResponse_brownWordExtracted() {
        val p = prefs()
        val location = SummoningChamberRequest.buildLocation("Gary")
        val html =
            """If you see -hic- Gary, tell him that the passhword is <font color=brown><b>oPeNs3saMe</b></font>."""
        val result = SummoningChamberRequest.parseResponse(
            location,
            "You light three black candles. $html",
            p,
        )
        assertEquals("oPeNs3saMe", result.brownWord)
    }
}
