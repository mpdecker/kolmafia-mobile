package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.BarrelShrineRequest
import net.sourceforge.kolmafia.request.CoinMasterPurchaseRequest
import net.sourceforge.kolmafia.request.CreateItemRequest
import net.sourceforge.kolmafia.request.CurseRequest
import net.sourceforge.kolmafia.request.InternalChatRequest
import net.sourceforge.kolmafia.request.NPCPurchaseRequest
import net.sourceforge.kolmafia.request.PalmFrondRequest
import net.sourceforge.kolmafia.request.RichardRequest
import net.sourceforge.kolmafia.request.ShowClanRequest
import net.sourceforge.kolmafia.request.SuburbanDisRequest
import net.sourceforge.kolmafia.request.TrophyRequest
import net.sourceforge.kolmafia.request.UmbrellaRequest
import net.sourceforge.kolmafia.session.TurnCounter

class GameRuntimeLibraryPhase4630Test {

    @Test
    fun revision_phase4870() {
        assertEquals("phase6670", GameRuntimeLibrary.REVISION)
        assertEquals("6670", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun get_counters_skipsExemptWhenLabelEmpty() {
        val p = Preferences(MapSettings())
        TurnCounter.startCounting(p, currentRun = 0, turns = 3, "Hidden loc=*", "x.gif")
        TurnCounter.startCounting(p, currentRun = 0, turns = 3, "Shown", "y.gif")
        val lib = GameRuntimeLibrary(preferences = p, character = null)
        val empty = outputLib(lib, """print(get_counters("", 0, 10));""")
        assertTrue("Shown" in empty)
        assertTrue("Hidden" !in empty)
    }

    @Test
    fun requestHubs_registerUrls() {
        assertTrue(CurseRequest.registerRequest("curse.php?action=use&whichitem=1"))
        assertTrue(TrophyRequest.registerRequest("trophies.php"))
        assertTrue(UmbrellaRequest.registerRequest("choice.php?whichchoice=1466&option=1"))
        assertTrue(RichardRequest.registerRequest("clan_hobopolis.php?place=3&preaction=spendturns&whichservice=1"))
        assertEquals(5, RichardRequest.getAdventuresUsed("clan_hobopolis.php?preaction=spendturns&numturns=5"))
        assertTrue(SuburbanDisRequest.registerRequest("suburbandis.php?action=altar"))
        assertEquals(1, SuburbanDisRequest.getAdventuresUsed("suburbandis.php?action=dothis"))
        assertTrue(ShowClanRequest.registerRequest("showclan.php?action=joinclan"))
        assertTrue(CreateItemRequest.registerRequest("craft.php?action=craft&mode=combine"))
        assertTrue(NPCPurchaseRequest.registerRequest("store.php?whichstore=b"))
        assertTrue(CoinMasterPurchaseRequest.registerRequest("shop.php?whichshop=armory"))
        assertTrue(InternalChatRequest.registerRequest("newchatmessages.php?j=1"))
        assertTrue(BarrelShrineRequest.registerRequest("da.php?barrelshrine=1"))
        assertTrue(PalmFrondRequest.registerRequest("multiuse.php?whichitem=2605"))
        assertTrue(TrophyRequest.parseTrophies("").isEmpty())
    }

    @Test
    fun run_choice_negative_withoutChoice_returnsEmpty() {
        val out = outputLib(
            GameRuntimeLibrary(preferences = Preferences(MapSettings())),
            """buffer b = run_choice(-1); print(length(b));""",
        )
        assertEquals("0", out)
    }
}
