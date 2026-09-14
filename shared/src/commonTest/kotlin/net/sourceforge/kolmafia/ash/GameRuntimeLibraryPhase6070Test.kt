package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.request.GrandpaRequest
import net.sourceforge.kolmafia.request.PulverizeRequest
import net.sourceforge.kolmafia.request.SummoningChamberRequest
import net.sourceforge.kolmafia.request.UntinkerRequest
import net.sourceforge.kolmafia.utilities.SimpleXPath
import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryPhase6070Test {

    @Test
    fun revision_phase6070() {
        // Superseded by XXXVIII; current runtime revision is phase6310.
        assertEquals("phase7150", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun grandpaParseResponse_setsStoryFlag() {
        val prefs = Preferences(MapSettings())
        GrandpaRequest.parseResponse("eel", "", prefs, null)
        assertTrue(prefs.getBoolean("grandpaUnlockedEelSauce", false))
    }

    @Test
    fun pulverizeParseResponse_readsQty() {
        assertEquals(
            3,
            PulverizeRequest.parseResponse(
                "craft.php?action=pulverize&smashitem=123&qty=3",
                "You smash it.",
            ),
        )
    }

    @Test
    fun untinkerParseResponse_requiresAcquire() {
        assertEquals(
            0,
            UntinkerRequest.parseResponse(
                "place.php?whichplace=forestvillage&action=fv_untinker&whichitem=5",
                "Nothing happened",
                inventoryCount = 4,
            ),
        )
        assertEquals(
            1,
            UntinkerRequest.parseResponse(
                "place.php?whichplace=forestvillage&action=fv_untinker&whichitem=5",
                "You acquire an item",
                inventoryCount = 4,
            ),
        )
    }

    @Test
    fun summoningChamber_failedMarkers_doNotConsume() {
        val prefs = Preferences(MapSettings())
        val parsed = SummoningChamberRequest.parseResponse(
            "choice.php?whichchoice=922&option=1&demonname=Foo",
            "You light three black candles and get some sort of crossed signal",
            prefs,
        )
        assertTrue(parsed.consumeSummoningItems)
        assertEquals(false, parsed.setDemonSummoned)
        assertEquals(false, prefs.getBoolean(Preferences.DEMON_SUMMONED, false))
    }

    @Test
    fun xpath_contains_accountInventoryFlag() {
        val html = """<label><input type="checkbox" checked="checked"  name="flag_invimages"></label>"""
        assertEquals(
            listOf("checked"),
            SimpleXPath.evaluate(html, "//input[contains(@name,'flag_invimages')]@checked"),
        )
    }
}
