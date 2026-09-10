package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ZoneCombatCalculator
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AWOLQuartermasterRequest
import net.sourceforge.kolmafia.request.BURTRequest
import net.sourceforge.kolmafia.request.GameShoppeRequest
import net.sourceforge.kolmafia.request.MrStoreRequest
import net.sourceforge.kolmafia.request.ShadowForgeRequest

class GameRuntimeLibraryPhase5110Test {

    @Test
    fun revision_phase5110() {
        assertEquals("phase6670", GameRuntimeLibrary.REVISION)
        assertEquals("6670", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun combatDatabase_parsesRejectionAndParityFlags() {
        val r = CombatDatabase.parseMonsterEntry("foo: 1r50")
        assertEquals("foo", r.name)
        assertEquals(1, r.weight)
        assertEquals(50, r.rejectionPercent)

        val odd = CombatDatabase.parseMonsterEntry("bar: 2o")
        assertEquals(2, odd.weight)
        assertEquals(1, odd.ascensionParity)

        val even = CombatDatabase.parseMonsterEntry("baz: 3e")
        assertEquals(3, even.weight)
        assertEquals(2, even.ascensionParity)
    }

    @Test
    fun zoneCombat_banishZeroesRateWhenStateful() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(com.russhwolf.settings.MapSettings())
        val banishes = BanishManager(prefs)
        banishes.banishMonster("spooky vampire", Banisher.SNOKEBOMB, currentTurn = 0)
        val ctx = ZoneCombatCalculator.Context(
            preferences = prefs,
            banishManager = banishes,
            turnsPlayed = 1,
        )
        val rates = ZoneCombatCalculator.appearanceRates(
            locationName = "The Spooky Forest",
            includeQueue = true,
            ctx = ctx,
        )
        assertTrue((rates["spooky vampire"] ?: 0.0) <= 0.0)
        val other = rates["spooky mummy"] ?: 0.0
        assertTrue(other > 0.0)
    }

    @Test
    fun appearance_rates_spookyForest_unchangedBaseline() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "15.0",
            outputLib(
                lib,
                """print(to_string(appearance_rates(to_location("The Spooky Forest"))[to_monster("none")]));""",
            ).trim(),
        )
        val rate = outputLib(
            lib,
            """print(to_string(appearance_rates(to_location("The Spooky Forest"))[to_monster("spooky vampire")]));""",
        ).trim().toDouble()
        assertEquals(85.0 / 6.0, rate, absoluteTolerance = 0.0001)
    }

    @Test
    fun shop_amount_int_overload_registers() {
        val out = outputLib(
            GameRuntimeLibrary(),
            """print(shop_amount(1));""",
        ).trim()
        assertEquals("0", out)
    }

    @Test
    fun http_hubs_phase5110() {
        assertTrue(BURTRequest.registerRequest("inv_use.php?whichitem=5683"))
        assertFalse(BURTRequest.registerRequest("inv_use.php?whichitem=1"))
        assertTrue(GameShoppeRequest.registerRequest("gamestore.php"))
        assertTrue(ShadowForgeRequest.registerRequest("shop.php?whichshop=shadow"))
        assertTrue(AWOLQuartermasterRequest.registerRequest("shop.php?whichshop=awol"))
        assertFalse(AWOLQuartermasterRequest.registerRequest("shop.php?whichshop=other"))
        assertTrue(MrStoreRequest.registerRequest("mrstore.php"))
    }
}
