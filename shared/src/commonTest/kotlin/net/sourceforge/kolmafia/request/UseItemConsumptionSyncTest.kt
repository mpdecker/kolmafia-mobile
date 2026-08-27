package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UseItemConsumptionSyncTest {

    @Test
    fun failureGate_missingItem() {
        assertTrue(
            UseItemConsumptionSync.isFailureGate(
                "You don't have the item you're trying to use.",
            ),
        )
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You don't have the item you're trying to use.",
            itemId = 100,
            count = 1,
        )
        assertFalse(ok)
        assertEquals("You don't have that item.", UseItemConsumptionSync.lastUpdate)
    }

    @Test
    fun photocopier_setsPrefOnSuccess() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "you drop your pants and giggle as you make a photocopy",
            itemId = UseItemConsumptionSync.PHOTOCOPIER,
            count = 1,
            preferences = prefs,
        )
        assertTrue(ok)
        assertEquals("Your butt", prefs.getString("photocopyMonster", ""))
    }

    @Test
    fun photocopier_failsWithoutGiggle() {
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You don't want your desk to get all messy",
            itemId = UseItemConsumptionSync.PHOTOCOPIER,
            count = 1,
            preferences = Preferences(MapSettings()),
        )
        assertFalse(ok)
    }

    @Test
    fun mojoFilter_incrementsAndReducesSpleen() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("currentMojoFilters", 0)
        val character = KoLCharacter()
        character.updateConsumables(fullness = 0, inebriety = 0, spleenUsed = 5)
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You strain some of the toxins out of your mojo, and discard the now-grodulated filter.",
            itemId = UseItemConsumptionSync.MOJO_FILTER,
            count = 1,
            preferences = prefs,
            character = character,
        )
        assertTrue(ok)
        assertEquals(1, prefs.getInt("currentMojoFilters", 0))
        assertEquals(4, character.state.value.spleenUsed)
    }

    @Test
    fun affirmationCookie_lifetimePrefOnReject() {
        val prefs = Preferences(MapSettings())
        val ok = UseItemConsumptionSync.parseConsumption(
            responseText = "You may only eat one of those per lifetime",
            itemId = UseItemConsumptionSync.DEEP_DISH_OF_LEGEND,
            count = 1,
            preferences = prefs,
        )
        assertFalse(ok)
        assertTrue(prefs.getBoolean("deepDishOfLegendEaten", false))
    }

    @Test
    fun maximumUses_mojoFilterCapsAtThree() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("currentMojoFilters", 2)
        val ctx = ItemUseLimitsContext(
            character = CharacterState(),
            preferences = prefs,
            expressionContext = ExpressionContext(),
        )
        assertEquals(1, maximumUses(ItemDatabase.MOJO_FILTER, "mojo filter", ctx))
    }
}
