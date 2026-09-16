package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.RaffleSync
import net.sourceforge.kolmafia.request.SendGiftSync
import net.sourceforge.kolmafia.request.XliiHttpResidualParse
import net.sourceforge.kolmafia.shop.InterestingCoinShopSync
import net.sourceforge.kolmafia.shop.TimeTowerSync
import net.sourceforge.kolmafia.utilities.PHPLCG

class GameRuntimeLibraryPhase6430Test {

    @Test
    fun revision_phase6430() {
        assertEquals("phase7510", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun preferences_globalScopeRoundTrip() {
        val prefs = Preferences(MapSettings())
        prefs.setString("xlIiGlobalTest", "g", global = true)
        prefs.setString("xlIiUserTest", "u", global = false)
        assertEquals("g", prefs.getString("xlIiGlobalTest", global = true))
        assertEquals("", prefs.getString("xlIiGlobalTest", global = false))
        assertEquals("u", prefs.getString("xlIiUserTest", global = false))
        assertEquals("u", prefs.removeProperty("xlIiUserTest", global = false))
        assertTrue(!prefs.propertyExists("xlIiUserTest", global = false))
    }

    @Test
    fun phpLcg_randRangeInclusive() {
        val rng = PHPLCG(42L)
        repeat(40) {
            val v = rng.rand(1, 6)
            assertTrue(v in 1..6, "got $v")
        }
    }

    @Test
    fun gameShoppe_parsesStoreCredit() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            XliiHttpResidualParse.parseResponse(
                "gamestore.php",
                "You currently have 1,250 store credits. You have 9 Game Grid tickets.",
                prefs,
            ),
        )
        assertEquals(1250, prefs.getInt("availableStoreCredits", 0))
        assertEquals(9, prefs.getInt("availableGameGridTickets", 0))
    }

    @Test
    fun interestingCoin_marksMissingDailyStock() {
        val prefs = Preferences(MapSettings())
        InterestingCoinShopSync.syncFromShopHtml("<html>empty shop</html>", prefs)
        assertEquals(3, prefs.getInt(InterestingCoinShopSync.dailyProperty(12312), 0))
        assertTrue(prefs.getBoolean(InterestingCoinShopSync.ascensionProperty(12293), false))
    }

    @Test
    fun twitchJousting_syncsTimeTower() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean(TimeTowerSync.PREF, false)
        TimeTowerSync.syncFromChronerShopHtml("<html>Renaissance Gift Shop open</html>", prefs)
        assertTrue(prefs.getBoolean(TimeTowerSync.PREF, false))
        assertTrue("twitch_jousting" in TimeTowerSync.CHRONER_SHOP_IDS)
    }

    @Test
    fun sendGift_parsesPackageSent() {
        val ok = SendGiftSync.parseTransfer(
            "town_sendgift.php?action=Yep.&whichpackage=1&fromwhere=0&whichitem1=100&howmany1=1",
            "<td>Package sent.</td>",
            inventory = null,
            character = null,
        )
        assertTrue(ok)
    }

    @Test
    fun raffle_requiresHereYouGo() {
        assertTrue(
            !RaffleSync.parseResponse(
                "raffle.php?action=buy&where=0&quantity=2",
                "You cannot afford that many tickets.",
                null,
                null,
            ),
        )
    }

    @Test
    fun meatDrop_appliesBrokeModifier() {
        MonsterStatusTracker.resetLastMonster()
        MonsterStatusTracker.setNextMonster(
            MonsterDefinition(
                name = "phase6430 broke slug",
                id = 64301,
                image = "x.gif",
                attack = 10,
                defense = 10,
                hp = 20,
                initiative = 0,
                meatDrop = 100,
                phylum = "beast",
                isBoss = false,
                isGhost = false,
                isLucky = false,
                isScaling = false,
                scale = 0,
                cap = 0,
                floor = 0,
                drops = emptyList(),
                randomModifiers = listOf("broke"),
            ),
            listOf("broke"),
        )
        try {
            val lib = GameRuntimeLibrary(preferences = Preferences(MapSettings()))
            val out = outputLib(lib, "print(meat_drop());").trim()
            assertEquals("5", out)
        } finally {
            MonsterStatusTracker.resetLastMonster()
        }
    }
}
