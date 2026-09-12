package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.LegacyCoinmasterResponseParse

class GameRuntimeLibraryPhase6310Test {

    @Test
    fun revision_phase6310() {
        assertEquals("phase6910", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun mrStore_parsesWordCountAccessories() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            LegacyCoinmasterResponseParse.parseResponse(
                "mrstore.php",
                "You have three Mr. Accessories to trade.",
                prefs,
            ),
        )
        assertEquals(3, prefs.getInt("availableMrAccessories", 0))
    }

    @Test
    fun bigBrother_parsesSandDollars() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            LegacyCoinmasterResponseParse.parseResponse(
                "monkeycastle.php?who=2",
                "You have <b>42</b> sand dollars.",
                prefs,
            ),
        )
        assertEquals(42, prefs.getInt("availableSandDollars", 0))
        assertTrue(prefs.getBoolean("bigBrotherRescued", false))
    }

    @Test
    fun fudgeWand_parsesFudgecules() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            LegacyCoinmasterResponseParse.parseResponse(
                "choice.php?whichchoice=562",
                "You've got <b>1,200</b> fudgecules.",
                prefs,
            ),
        )
        assertEquals(1200, prefs.getInt("availableFudgecules", 0))
    }

    @Test
    fun isotope_parsesLunarBalance() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            LegacyCoinmasterResponseParse.parseResponse(
                "shop.php?whichshop=isotope",
                "You have 500 lunar isotopes.",
                prefs,
            ),
        )
        assertEquals(500, prefs.getInt("availableLunarIsotopes", 0))
    }

    @Test
    fun eight_bit_points_locColor_overload() {
        val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()).also {
            it.setString("8BitColor", "red")
        })
        // Non-eight-bit location → 0; color overload must resolve.
        assertEquals(
            "0",
            outputLib(lib, """print(eight_bit_points(to_location("The Shore"), "red"));""").trim(),
        )
    }
}
