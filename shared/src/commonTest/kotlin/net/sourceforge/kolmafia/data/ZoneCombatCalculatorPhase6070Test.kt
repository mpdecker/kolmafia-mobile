package net.sourceforge.kolmafia.data

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.banish.Banisher
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.preferences.Preferences

class ZoneCombatCalculatorPhase6070Test {

    @Test
    fun appearanceRates_emitsUltraRareSentinel() = runBlocking {
        CombatDatabase.load()
        val rates = ZoneCombatCalculator.appearanceRates(
            locationName = "The Spooky Forest",
            includeQueue = false,
            ctx = ZoneCombatCalculator.Context(),
        )
        assertEquals(-1.0, rates["Baiowulf"])
    }

    @Test
    fun appearanceRates_emitsBanishedSentinel() = runBlocking {
        CombatDatabase.load()
        val prefs = Preferences(MapSettings())
        val banishes = BanishManager(prefs)
        banishes.banishMonster("spooky mummy", Banisher.ICE_HOUSE, currentTurn = 0)
        val rates = ZoneCombatCalculator.appearanceRates(
            locationName = "The Spooky Forest",
            includeQueue = true,
            ctx = ZoneCombatCalculator.Context(
                preferences = prefs,
                banishManager = banishes,
                turnsPlayed = 5,
            ),
        )
        assertEquals(-3.0, rates["spooky mummy"])
    }

    @Test
    fun appearanceRates_holdHandsUsesAdjustedBase() = runBlocking {
        CombatDatabase.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("holdHandsLocation", "The Spooky Forest")
        prefs.setString("holdHandsMonster", "spooky vampire")
        prefs.setInt("holdHandsMonsterCount", 1)

        val without = ZoneCombatCalculator.appearanceRates(
            locationName = "The Spooky Forest",
            includeQueue = false,
            ctx = ZoneCombatCalculator.Context(),
        )
        val withHold = ZoneCombatCalculator.appearanceRates(
            locationName = "The Spooky Forest",
            includeQueue = true,
            ctx = ZoneCombatCalculator.Context(preferences = prefs),
        )
        val base = without["spooky vampire"] ?: 0.0
        val held = withHold["spooky vampire"] ?: 0.0
        assertTrue(held > base, "holdHands should increase rate ($held vs $base)")
    }
}
