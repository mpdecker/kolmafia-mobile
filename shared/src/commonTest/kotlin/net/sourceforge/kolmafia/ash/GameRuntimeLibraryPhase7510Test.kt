package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.CharpaneValhallaSync
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AfterLifeRequest
import net.sourceforge.kolmafia.session.ChoiceCombatAshState
import net.sourceforge.kolmafia.session.ValhallaManager

/** Ascension mechanics residual mega wrap (phases 7451–7510). */
class GameRuntimeLibraryPhase7510Test {

    @AfterTest
    fun tearDown() {
        CharpaneValhallaSync.reset()
        ChoiceCombatAshState.reset()
    }

    @Test
    fun revision_isPhase7510() {
        assertEquals("phase7510", GameRuntimeLibrary.REVISION)
        assertEquals("7510", outputLib(GameRuntimeLibrary(), "print(get_revision());").trim())
    }

    @Test
    fun clanDungeonResidualsStayLive() {
        assertEquals("Hodgman", net.sourceforge.kolmafia.session.HobopolisManager.hobopolisBossName(200))
    }

    @Test
    fun ascendPhp_runsPreAscensionAndSetsLastBreakfast() {
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(preferences = prefs)
        lib.processVisitResponseHooks("<html/>", "ascend.php?action=ascend&confirm=1")
        assertEquals(0, prefs.getInt("lastBreakfast", -1))
        assertEquals(0, prefs.getInt("knownAscensions", 0))
    }

    @Test
    fun afterlifeVisit_onAscensionThenReincarnatePosts() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastBreakfast", 0)
        val lib = GameRuntimeLibrary(preferences = prefs)
        lib.processVisitResponseHooks("<html>spirit</html>", "afterlife.php")
        assertEquals(-1, prefs.getInt("lastBreakfast", 0))
        assertEquals(1, prefs.getInt("knownAscensions", 0))
        lib.processVisitResponseHooks("<html>ok</html>", "afterlife.php?action=ascend&confirmascend=1")
        assertEquals("apathetic", prefs.getString("mood", ""))
    }

    @Test
    fun ascendMessageAndBearArmIdsAreDesktopShaped() {
        assertEquals(5792, ItemPool.LEFT_BEAR_ARM)
        assertEquals(5791, ItemPool.RIGHT_BEAR_ARM)
        assertEquals(5790, ItemPool.BOX_OF_BEAR_ARM)
        assertTrue(
            AfterLifeRequest.ascendMessage(
                "afterlife.php?action=ascend&confirmascend=1&asctype=1&gender=1&whichclass=1&whichpath=0&whichsign=1",
                karma = 12,
            ).contains("Casual Male Seal Clubber under the Mongoose sign"),
        )
        assertEquals(3, ValhallaManager.USABLE_ITEM_IDS.size)
    }
}
