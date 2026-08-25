package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class FightIotmSyncTest {
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        prefs = Preferences(MapSettings())
    }

    @Test
    fun sweatMoreAndLess() {
        prefs.setInt("sweat", 50)
        FightIotmSync.applySweat("<td>You get 10% Sweatier.</td>", prefs)
        assertEquals(60, prefs.getInt("sweat", 0))
        FightIotmSync.applySweat("<td>You get 5% less Sweaty.</td>", prefs)
        assertEquals(55, prefs.getInt("sweat", 0))
    }

    @Test
    fun nanorhinoBuffResetsCharge() {
        prefs.setInt("_nanorhinoCharge", 100)
        assertTrue(
            FightIotmSync.applyNanorhino("""title="Nanobrawny" """, prefs),
        )
        assertEquals(0, prefs.getInt("_nanorhinoCharge", -1))
    }

    @Test
    fun scrapbookChargeIncrement() {
        FightIotmSync.applyScrapbook("that is definitely going in the scrapbook", prefs)
        assertEquals(1, prefs.getInt("scrapbookCharges", 0))
    }

    @Test
    fun gooseDronesLast() {
        prefs.setInt("gooseDronesRemaining", 3)
        FightIotmSync.applyGooseDrones(
            "matter duplicating drones. That was the last drone.",
            prefs,
        )
        assertEquals(0, prefs.getInt("gooseDronesRemaining", -1))
    }

    @Test
    fun cookbookbatIncrementsCharge() {
        FightIotmSync.applyCookbookbat("Your Cookbookbat flaps.", prefs, "The Shore")
        assertEquals(1, prefs.getInt("cookbookbatIngredientsCharge", 0))
    }

    @Test
    fun melodramedarySpitFull() {
        FightIotmSync.applyMelodramedary(
            "sucking the liquid out of a keg (42% full)",
            prefs,
        )
        assertEquals(42, prefs.getInt("camelSpit", 0))
    }

    @Test
    fun bagOTricksCharge() {
        FightIotmSync.applyBagOTricks(
            "The Bag o' Tricks suddenly feels a little heavier.",
            prefs,
        )
        assertEquals(1, prefs.getInt("bagOTricksCharges", 0))
    }

    @Test
    fun noteFightStartDecrementsBowlingReturn() {
        prefs.setInt("cosmicBowlingBallReturnCombats", 3)
        FightIotmSync.noteFightStart(prefs)
        assertEquals(2, prefs.getInt("cosmicBowlingBallReturnCombats", -1))
    }
}
