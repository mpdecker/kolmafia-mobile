package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

class GameRuntimeLibraryAshP155Test {

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun revision_phase182() {
        assertEquals("phase2450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun shopVisitHook_appliesPirateRealmFunALogSync() {
        CoinmasterDatabase.loadFromText(
            shopsText = "piraterealm\tPirateRealm Fun-a-Log\n",
            coinText = "PirateRealm Fun-a-Log\tbuy\t100\tcrabsicle\tROW1053\n",
        )
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = p, character = KoLCharacter())
        lib.processVisitResponseHooks(
            html = """
                <b>You have 250 FunPoints.</b>
                <tr rel="10227"><td>pirate fork</td></tr>
            """.trimIndent(),
            url = "https://www.kingdomofloathing.com/shop.php?whichshop=piraterealm",
        )
        assertTrue(p.getBoolean("pirateRealmUnlockedFork", false))
        assertEquals(250, p.getInt("availableFunPoints", 0))
    }
}
