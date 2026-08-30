package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.BastilleDatabase.Castle
import net.sourceforge.kolmafia.data.BastilleDatabase.Stat
import net.sourceforge.kolmafia.data.BastilleDatabase.Stats
import net.sourceforge.kolmafia.preferences.Preferences

class BastilleBattalionAdvisorTest {
    @Test
    fun resultsExposeWinCountAndOutcome() {
        val results = BastilleBattleResults(true, military = true, castle = false, psychological = true)
        assertEquals(2, results.winCount())
        assertTrue(results.won())
    }

    @Test
    fun stanceSimulationAccountsForAggressorOdds() {
        val stats = Stats(ma = 9, md = 1, ca = 9, cd = 1, pa = 9, pd = 1)
        val offense = BastilleBattleSimulation.winProbability(
            stats, BastilleBoosts(""), Castle.BORING, battleNumber = 1, BastilleStance.OFFENSE,
        )
        val defense = BastilleBattleSimulation.winProbability(
            stats, BastilleBoosts(""), Castle.BORING, battleNumber = 1, BastilleStance.DEFENSE,
        )
        assertTrue(offense > defense)
        assertEquals(BastilleStance.OFFENSE, BastilleBattleSimulation.bestStance(stats, BastilleBoosts(""), Castle.BORING, 1))
    }

    @Test
    fun allTwelveScalingCheeseEncountersHaveFormulae() {
        val scaling = BastilleCheeseEncounter.scalingEncounters()
        assertEquals(12, scaling.size)
        assertTrue(scaling.all { it.expectedCheese(5) > 0 })
        assertTrue(BastilleCheeseEncounter.forName("Raid the cave").expectedCheese(8) >
            BastilleCheeseEncounter.forName("Raid the cave").expectedCheese(2))
        assertTrue(BastilleCheeseEncounter.forName("Enter the Weakest Army competition").expectedCheese(8) <
            BastilleCheeseEncounter.forName("Enter the Weakest Army competition").expectedCheese(2))
    }

    @Test
    fun cheeseAdvisorChoosesHighestExpectedOffer() {
        val prefs = Preferences(MapSettings()).apply {
            setString("_bastilleStats", "MA=8,MD=1,CA=2,CD=3,PA=4,PD=5")
            setString("_bastilleChoice1", "Raid the cave")
            setString("_bastilleChoice2", "Enter the Weakest Army competition")
            setString("_bastilleChoice3", "Grab the boulder")
        }
        assertEquals(1, BastilleBattalionAdvisor.recommend(BastilleBattalionSync.CHOICE_CHEESE_SEEKING, prefs))
    }

    @Test
    fun resetHubClearsDailyAndTransientProperties() {
        val prefs = Preferences(MapSettings()).apply {
            setInt("_bastilleGames", 4)
            setString("_bastilleStats", "MA=9")
            setString("_bastilleChoice1", "old")
        }
        BastilleBattalionSync.reset(prefs)
        assertEquals(0, prefs.getInt("_bastilleGames"))
        assertEquals("", prefs.getString("_bastilleStats"))
        assertEquals("", prefs.getString("_bastilleChoice1"))
        assertEquals(Stat.MA, BastilleCheeseEncounter.forName("Raid the cave").stat)
    }
}
