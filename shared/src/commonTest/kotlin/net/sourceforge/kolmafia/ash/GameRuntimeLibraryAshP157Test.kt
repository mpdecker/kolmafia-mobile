package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

class GameRuntimeLibraryAshP157Test {

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun revision_phase182() {
        assertEquals("phase470", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun shopVisitHook_appliesDripArmorySync() {
        CoinmasterDatabase.loadFromText(
            shopsText = "driparmory\tDrip Institute Armory\n",
            coinText = "Drip Institute Armory\tbuy\t50\tdrippy shield\tROW1132\n",
        )
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        lib.processVisitResponseHooks(
            html = """<b>drippy shield</b><td>Driplet (50)</td>""",
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=driparmory",
        )
        assertTrue(p.getBoolean("drippyShieldUnlocked", false))
    }
}
