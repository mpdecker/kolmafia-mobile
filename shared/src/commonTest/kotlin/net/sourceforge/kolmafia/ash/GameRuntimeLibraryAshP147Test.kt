package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterShopSync

class GameRuntimeLibraryAshP147Test {

    @Test
    fun revision_phase176() {
        assertEquals("phase200", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun shopVisitHook_appliesCoinmasterSync() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p)
        lib.processVisitResponseHooks(
            html = """<tr rel="5906"><b>pixel pill</b></tr>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=mystic",
        )
        assertTrue(p.getBoolean(CoinmasterShopSync.MYSTIC_PSYCHOSIS_ITEMS_UNLOCKED, false))
    }

    @Test
    fun shopVisitHook_appliesWildfireNpcSync() {
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p)
        lib.processVisitResponseHooks(
            html = "<html></html>",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=wildfire",
        )
        assertTrue(p.getBoolean("itemBoughtPerAscension10790", false))
    }
}
