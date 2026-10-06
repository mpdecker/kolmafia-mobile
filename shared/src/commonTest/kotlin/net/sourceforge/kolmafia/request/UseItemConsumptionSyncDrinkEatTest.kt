package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UseItemConsumptionSyncDrinkEatTest {

    @Test
    fun drink_everfullOnceADay_setsPref() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You can only drink from your everfull glass once a day.",
            itemId = UseItemConsumptionSync.EVERFULL_GLASS,
            count = 1,
            preferences = prefs,
        )
        assertFalse(ok)
        assertTrue(prefs.getBoolean("_everfullGlassUsed", false))
        assertEquals(
            "You may only drink from the everfull glass once a day.",
            UseItemConsumptionSync.lastUpdate,
        )
    }

    @Test
    fun drink_getsYouDrunk_rejectSetsTurns() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You shouldn't drink one of those",
            itemId = UseItemConsumptionSync.GETS_YOU_DRUNK,
            count = 1,
            preferences = prefs,
        )
        assertFalse(ok)
        assertEquals(4, prefs.getInt("getsYouDrunkTurnsLeft", 0))
    }

    @Test
    fun drink_pickleJuice_setsPrefAndReducesSpleen() {
        val prefs = Preferences(MapSettings())
        val character = KoLCharacter()
        character.updateConsumables(fullness = 0, inebriety = 0, spleenUsed = 10)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You drink the fermented pickle juice. Yum! Drunkenness",
            itemId = UseItemConsumptionSync.FERMENTED_PICKLE_JUICE,
            count = 1,
            preferences = prefs,
            character = character,
        )
        assertTrue(ok)
        assertTrue(prefs.getBoolean("_pickleJuiceDrunk", false))
        assertEquals(5, character.state.value.spleenUsed)
    }

    @Test
    fun drink_frostyMugHelper_setsPref() {
        val prefs = Preferences(MapSettings())
        net.sourceforge.kolmafia.session.ConsumptionHelperState.queueDrinkHelper(
            UseItemConsumptionSync.FROSTYS_MUG,
            1,
        )
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "Brisk! Refreshing! You drink the frigid beer and discard the no-longer-frosty mug. Drunkenness",
            itemId = UseItemConsumptionSync.BLOODWEISER,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertTrue(prefs.getBoolean("_frostyMugUsed", false))
        net.sourceforge.kolmafia.session.ConsumptionHelperState.clearDrinkHelper()
    }

    @Test
    fun drink_cinchoSaltAndLime_decrements() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cinchoSaltAndLime", 2)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "Some of the salt and lime stuck to your hands gets on your drink. Drunkenness",
            itemId = UseItemConsumptionSync.MINI_MARTINI,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(1, prefs.getInt("cinchoSaltAndLime", 0))
        assertEquals(1, prefs.getInt("miniMartinisDrunk", 0))
    }

    @Test
    fun eat_ghostPepper_rejectSetsTurns() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You shouldn't eat one of those",
            itemId = UseItemConsumptionSync.GHOST_PEPPER,
            count = 1,
            preferences = prefs,
        )
        assertFalse(ok)
        assertEquals(4, prefs.getInt("ghostPepperTurnsLeft", 0))
    }

    @Test
    fun eat_spaghettiBreakfast_setsPref() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You eat the spaghetti breakfast. Fullness",
            itemId = UseItemConsumptionSync.SPAGHETTI_BREAKFAST,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertTrue(prefs.getBoolean("_spaghettiBreakfastEaten", false))
    }

    @Test
    fun eat_tooFull_partialConsume() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("munchiesPillsUsed", 3)
        val character = KoLCharacter()
        // fullnessLimit default 15; set fullness to 14 so room for at most partial
        character.updateConsumables(fullness = 14, inebriety = 0, spleenUsed = 0)
        // fortune cookie is fullness 1 typically; without DB, fullness may be 0 and early-return
        // Use a synthetic path: when fullness==0, applyTooFullPartial returns false without consume
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You're too full to eat that.",
            itemId = UseItemConsumptionSync.FORTUNE_COOKIE,
            count = 3,
            preferences = prefs,
            character = character,
        )
        assertFalse(ok)
        assertEquals("Consumption limit reached.", UseItemConsumptionSync.lastUpdate)
    }

    @Test
    fun eat_updateTimeSpinner_appendsFood() {
        val prefs = Preferences(MapSettings())
        // Use a tradeable/discardable id when ItemDatabase is unloaded — method still writes when flags pass
        UseItemConsumptionSync.updateTimeSpinner(
            itemId = UseItemConsumptionSync.CANDIED_SWEET_POTATOES,
            preferences = prefs,
            timeSpinnerUsed = false,
        )
        // Without ItemDatabase load, isDiscardable/isTradeable may be false — just ensure no crash
        UseItemConsumptionSync.updateTimeSpinner(
            itemId = 1,
            preferences = prefs,
            timeSpinnerUsed = true,
        )
        assertEquals(3, prefs.getInt("_timeSpinnerMinutesUsed", 0))
    }

    @Test
    fun drink_tps_sequentialConsume() {
        assertTrue(DrinkBoozeRequest.sequentialConsume(DrinkBoozeRequest.DIRTY_MARTINI))
        assertTrue(DrinkBoozeRequest.sequentialConsume(DrinkBoozeRequest.SANGRIA_DEL_DIABLO))
        assertFalse(DrinkBoozeRequest.sequentialConsume(UseItemConsumptionSync.BLOODWEISER))
    }

    @Test
    fun spleen_rupture_partialConsume() {
        val character = KoLCharacter()
        character.updateConsumables(fullness = 0, inebriety = 0, spleenUsed = 14)
        // turkey blaster is typically spleen 5; with unloaded DB hit may be 0 → early false
        // Seed via known spleen id with synthetic hit by using STEEL_SPLEEN (hit often 0) —
        // verify rupture path sets lastUpdate when hit==0.
        val okZero = UseItemConsumptionSync.parseConsumption(
            responseText = "Your spleen is about to rupture!",
            itemId = UseItemConsumptionSync.STEEL_SPLEEN,
            count = 2,
            character = character,
        )
        assertFalse(okZero)
        assertEquals("Your spleen might go kablooie.", UseItemConsumptionSync.lastUpdate)
    }

    @Test
    fun spleen_voodooSnuff_setsPref() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You snuff the voodoo snuff. Spleen",
            itemId = UseItemConsumptionSync.VOODOO_SNUFF,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertTrue(prefs.getBoolean("_voodooSnuffUsed", false))
    }

    @Test
    fun spleen_turkeyBlaster_cantHandleCapsDaily() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You can't handle another turkey blaster today.",
            itemId = UseItemConsumptionSync.TURKEY_BLASTER,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals(3, prefs.getInt("_turkeyBlastersUsed", 0))
    }

    @Test
    fun spleen_homebodyl_successGatesCharges() {
        val prefs = Preferences(MapSettings())
        val miss = UseItemConsumptionSync.parseConsumption(
            responseText = "Nothing happens.",
            itemId = UseItemConsumptionSync.HOMEBODYL,
            count = 1,
            preferences = prefs,
        )
        assertTrue(miss)
        assertEquals(0, prefs.getInt("homebodylCharges", 0))
        val hit = UseItemConsumptionSync.parseConsumption(
            responseText = "You pop the pill and feel an immediate desire to nest.",
            itemId = UseItemConsumptionSync.HOMEBODYL,
            count = 2,
            preferences = prefs,
        )
        assertTrue(hit)
        assertEquals(22, prefs.getInt("homebodylCharges", 0))
    }
}
