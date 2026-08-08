package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterPurchasePrefs

class GameRuntimeLibraryAshP151Test {

    @Test
    fun revision_phase179() {
        assertEquals("phase300", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun storeVisitHook_appliesHiddenTavernNpcSync() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(
            preferences = p,
            character = KoLCharacter().apply {
                updateFromApiResponse(CharacterApiResponse(ascensions = "3"))
            },
        )
        lib.processVisitResponseHooks(
            html = "<html>The Hidden Tavern</html>",
            url = "https://www.kingdomofloathing.com/store.php?whichstore=hiddentavern",
        )
        assertEquals(3, p.getInt("hiddenTavernUnlock", -1))
    }

    @Test
    fun swaggerVisitHook_appliesSeasonPrefs() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p)
        val html = """
            You've earned 600 swagger during a pirate season, yarrr.
            <tr><td><b>Black Bart's Booty</b></td>
            <td><form><input type="hidden" name="whichitem" value="7732" />
            <input type="submit" value="Buy (1000 swagger)" /></form></td></tr>
        """.trimIndent()
        lib.processVisitResponseHooks(
            html = html,
            url = "https://www.kingdomofloathing.com/peevpee.php?place=shop",
        )
        assertEquals("pirate", p.getString("currentPVPSeason", ""))
        assertTrue(p.getBoolean("blackBartsBootyAvailable", false))
    }

    @Test
    fun swaggerVisitHook_skipsBuyActionUrl() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p)
        lib.processVisitResponseHooks(
            html = "You've earned 600 swagger during a pirate season",
            url = "https://www.kingdomofloathing.com/peevpee.php?place=shop&action=buy",
        )
        assertEquals("", p.getString("currentPVPSeason", ""))
    }

    @Test
    fun jarlPurchaseHook_setsCosmicSixPackPref() {
        val p = Preferences(MapSettings())
        CoinmasterPurchasePrefs.applyPurchasedItem(
            master = net.sourceforge.kolmafia.shop.CoinmasterData(
                masterName = "Jarlsberg's Cosmic Kitchen",
                nickname = "jarl",
                token = null,
                shopId = "jarl",
                buyItems = emptyList(),
                sellItems = emptyList(),
            ),
            itemId = 6237,
            prefs = p,
        )
        assertTrue(p.getBoolean("_cosmicSixPackConjured", false))
    }
}
