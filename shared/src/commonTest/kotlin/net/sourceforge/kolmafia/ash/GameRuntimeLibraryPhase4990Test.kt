package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.request.AutoSellRequestHub
import net.sourceforge.kolmafia.request.BurningNewspaperRequest
import net.sourceforge.kolmafia.request.Crimbo05Request
import net.sourceforge.kolmafia.request.Crimbo06Request
import net.sourceforge.kolmafia.request.Crimbo07Request
import net.sourceforge.kolmafia.request.GrubbyWoolRequest
import net.sourceforge.kolmafia.request.MeteoroidRequest

class GameRuntimeLibraryPhase4990Test {

    @Test
    fun revision_phase4990() {
        assertEquals("phase5590", GameRuntimeLibrary.REVISION)
        assertEquals("phase5590", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun random_int_and_min_max_arity() {
        val out = outputLib(
            GameRuntimeLibrary(),
            """
            print(min(5, 2, 9));
            print(max(5, 2, 9, 1));
            int r = random(10);
            print(r >= 0 && r < 10);
            """.trimIndent(),
        )
        val lines = out.lines().filter { it.isNotBlank() }
        assertEquals("2", lines[0])
        assertEquals("9", lines[1])
        assertEquals("true", lines[2])
    }

    @Test
    fun burningNewspaper_meteoroid_wool_crimbo_autosell_hubs() {
        assertTrue(BurningNewspaperRequest.registerRequest("choice.php?whichchoice=1277&option=2"))
        assertFalse(BurningNewspaperRequest.registerRequest("choice.php?whichchoice=1"))
        assertTrue(MeteoroidRequest.registerRequest("choice.php?whichchoice=1264&option=3"))
        assertFalse(MeteoroidRequest.registerRequest("choice.php?whichchoice=1277"))
        assertTrue(GrubbyWoolRequest.registerRequest("choice.php?whichchoice=1490&option=1"))
        assertFalse(GrubbyWoolRequest.registerRequest("choice.php?whichchoice=1264"))
        assertTrue(Crimbo05Request.registerRequest("crimbo_uncle.php?whichitem=100&quantity=2"))
        assertFalse(Crimbo05Request.registerRequest("shop.php?whichshop=x"))
        assertTrue(Crimbo06Request.registerRequest("crimbo06.php?whichitem=55"))
        assertTrue(Crimbo07Request.registerRequest("crimbo07.php?whichitem=66"))
        assertTrue(AutoSellRequestHub.registerRequest("sellstuff.php?action=sellall"))
        assertTrue(AutoSellRequestHub.registerRequest("sellstuff_ugly.php?action=sell"))
        assertFalse(AutoSellRequestHub.registerRequest("adventure.php"))
    }
}
