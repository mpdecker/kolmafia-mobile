package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

class GameRuntimeLibraryAshP159Test {

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun revision_phase184() {
        assertEquals("phase240", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun shopVisitHook_appliesAlliedHqTimeTowerSync() {
        CoinmasterDatabase.loadFromText(
            shopsText = "twitch_alliedhq\tAllied HQ\n",
            coinText = "Allied HQ\tROW1599\tflak shield\tChroner (20)\n",
        )
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        lib.processVisitResponseHooks(
            html = """<b>flak shield</b> Chroner (20)""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=twitch_alliedhq",
        )
        assertTrue(p.getBoolean("timeTowerAvailable", false))
    }

    @Test
    fun placeTwitchHook_appliesTimeTowerSync() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        lib.processVisitResponseHooks(
            html = """<b>Time-Twitching Tower</b> town_tower""",
            url = "https://www.kingdomofloathing.com/place.php?place=twitch",
        )
        assertTrue(p.getBoolean("timeTowerAvailable", false))
    }
}
