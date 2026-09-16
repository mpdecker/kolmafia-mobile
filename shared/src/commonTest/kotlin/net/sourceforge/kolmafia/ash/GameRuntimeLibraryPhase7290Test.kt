package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CafeRequest
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import net.sourceforge.kolmafia.session.ResponseTextParser

/**
 * Focused HTTP parse-depth leftovers Track A coverage (phases 7271–7290).
 * Parent wrap bumps REVISION to phase7330.
 */
class GameRuntimeLibraryPhase7290Test {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    private fun inventory(): InventoryManager =
        InventoryManager(HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) }), GameEventBus())

    @Test
    fun cafeVisit_wiresDailySpecialThroughCafeRequestParseResponse() {
        val p = prefs()
        val html = """
            Today's Special:
            <input type=hidden name=whichitem value=806>
            <a href='javascript:void()' onclick='descitem("12345")'></a>
            <td>special lager (150 Meat)</td>
        """.trimIndent()
        CafeRequest.parseResponse("cafe.php?cafeid=2", html, p)
        assertEquals(806, p.getInt("_dailySpecialItemId", 0))
        assertEquals(150, p.getInt("_dailySpecialPrice", 0))
    }

    @Test
    fun microBreweryConsume_setsMimeArmyShotglass() {
        val p = prefs()
        CafeRequest.parseResponse(
            "cafe.php?cafeid=2&action=CONSUME!&whichitem=806",
            "You pour your drink into your mime army shotglass and drink.",
            p,
        )
        assertTrue(p.getBoolean("_mimeArmyShotglassUsed", false))
    }

    @Test
    fun drinkHelpers_decrementFlagellateFlagon() {
        val p = prefs { putInt("flagellateFlagonsActive", 2) }
        DrinkBoozeRequest.parseDrinkHelpers(
            "You pour your drink into your flagellate flagon.",
            p,
        )
        assertEquals(1, p.getInt("flagellateFlagonsActive", 0))
    }

    @Test
    fun handleFoodHelper_consumesSaltAndCapsAtThree() {
        val p = prefs { putInt("_saltGrainsConsumed", 1) }
        val inv = inventory()
        inv.gainItemLocally(EatFoodRequest.GRAINS_OF_SALT, 5)
        EatFoodRequest.handleFoodHelper(
            itemName = "toast",
            count = 2,
            responseText = "You chase it with that salt you made in the chemistry lab.",
            preferences = p,
            inventory = inv,
            adjustFullness = false,
        )
        assertEquals(3, p.getInt("_saltGrainsConsumed", 0))
        assertEquals(3, inv.getCount(EatFoodRequest.GRAINS_OF_SALT))
    }

    @Test
    fun chezConsume_clearsMilkOfMagnesium() {
        val p = prefs { putBoolean("milkOfMagnesiumActive", true) }
        CafeRequest.parseResponse(
            "cafe.php?cafeid=1&action=CONSUME!&whichitem=100",
            "You gain 5 Adventures. Satisfied, you let loose a nasty magnesium-flavored belch.",
            p,
        )
        assertEquals(false, p.getBoolean("milkOfMagnesiumActive", true))
    }

    @Test
    fun responseTextParser_classifiesCafe() {
        assertEquals("cafe", ResponseTextParser.classify("cafe.php?cafeid=2"))
    }
}
