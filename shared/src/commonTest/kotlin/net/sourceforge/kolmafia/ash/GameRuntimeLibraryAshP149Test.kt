package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterPurchasePrefs

class GameRuntimeLibraryAshP149Test {

    @Test
    fun revision_phase179() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun shopVisitHook_appliesShoreCoinmasterSync() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p)
        lib.processVisitResponseHooks(
            html = "<html></html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=shore",
        )
        assertTrue(p.getBoolean("itemBoughtPerAscension637", false))
    }

    @Test
    fun shopVisitHook_appliesHippyNpcSync() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p, character = net.sourceforge.kolmafia.character.KoLCharacter().apply {
            updateFromApiResponse(net.sourceforge.kolmafia.character.CharacterApiResponse(ascensions = "2"))
        })
        lib.processVisitResponseHooks(
            html = "peach pear plum",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=hippy",
        )
        assertEquals("hippy", p.getString("currentHippyStore", ""))
    }
}
