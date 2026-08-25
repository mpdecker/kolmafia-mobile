package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences

class FightFinalRoundSyncTest {
    private lateinit var prefs: Preferences
    private lateinit var character: KoLCharacter

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
        character = KoLCharacter()
        character.updateHpMp(50, 100, 50, 100)
        EncounterManager.ignoreSpecialMonsters = true
        FightActionCostSync.jiggledChefstaff = true
    }

    @Test
    fun ghostPepperDecrementsWhileAlive() {
        prefs.setInt("ghostPepperTurnsLeft", 5)
        FightFinalRoundSync.applyGhostPepper(
            html = "The ghost pepper you ate burns on.",
            preferences = prefs,
            character = character,
            garbledCombat = false,
        )
        assertEquals(4, prefs.getInt("ghostPepperTurnsLeft", 0))
    }

    @Test
    fun ghostPepperClearsWithoutMessage() {
        prefs.setInt("ghostPepperTurnsLeft", 3)
        FightFinalRoundSync.applyGhostPepper(
            html = "plain fight",
            preferences = prefs,
            character = character,
            garbledCombat = false,
        )
        assertEquals(0, prefs.getInt("ghostPepperTurnsLeft", -1))
    }

    @Test
    fun garbageShirtCharge() {
        FightFinalRoundSync.applyGarbageCharges(
            html = "You read a useful bit of information off your shirt. Looks like there are 36 more useful scraps.",
            preferences = prefs,
            character = null,
            inventory = null,
        )
        assertEquals(36, prefs.getInt("garbageShirtCharge", 0))
    }

    @Test
    fun unicornHornInflates() {
        FightFinalRoundSync.applyUnicornHorn(
            "you hear a whirring as your unicorn horn begins to inflate. Yeah!",
            prefs,
        )
        assertEquals(5, prefs.getInt("unicornHornInflation", 0))
    }

    @Test
    fun fightEndedClearsIgnoreSpecialAndActionCost() {
        FightFinalRoundSync.apply(
            html = "You win the fight!",
            preferences = prefs,
            character = character,
            won = true,
            fightEnded = true,
        )
        assertFalse(EncounterManager.ignoreSpecialMonsters)
        assertFalse(FightActionCostSync.jiggledChefstaff)
        assertTrue(FightActionCostSync.nextAction.isEmpty())
    }

    @Test
    fun stinkyCheeseIncrements() {
        FightFinalRoundSync.apply(
            html = "fight",
            preferences = prefs,
            stinkyCheeseLevel = 2,
        )
        assertEquals(2, prefs.getInt("_stinkyCheeseCount", 0))
    }
}
