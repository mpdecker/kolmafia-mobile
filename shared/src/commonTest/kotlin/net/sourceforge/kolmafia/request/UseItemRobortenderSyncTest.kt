package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class UseItemRobortenderSyncTest {

    private fun inventory(): InventoryManager =
        InventoryManager(
            HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            GameEventBus(),
        )

    @Test
    fun cocktail_appendsTheDrinkAndConsumesIt() {
        val prefs = Preferences(MapSettings())
        val inventory = inventory()
        inventory.gainItemLocally(9396, 2)
        val ok = UseItemRobortenderSync.parse(
            "https://www.kingdomofloathing.com/inventory.php?action=robooze&whichitem=9396",
            "Your Robortender mixes the cocktail and knocks it back.",
            inventory,
            prefs,
            itemName = { "single entendre" },
        )
        assertEquals(true, ok)
        assertEquals("single entendre", prefs.getString("_roboDrinks", ""))
        assertEquals(1, inventory.getCount(9396))
    }

    @Test
    fun secondDrink_isCommaAppended() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_roboDrinks", "single entendre")
        UseItemRobortenderSync.parse(
            "inventory.php?pwd&action=robooze&whichitem=9400",
            "He enjoys the drink.",
            preferences = prefs,
            itemName = { "dirt julep" },
        )
        assertEquals("single entendre,dirt julep", prefs.getString("_roboDrinks", ""))
    }

    @Test
    fun refusedDrink_keepsTheItemAndThePref() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_roboDrinks", "piscatini")
        val inventory = inventory()
        inventory.gainItemLocally(12, 1)
        val ok = UseItemRobortenderSync.parse(
            "inventory.php?action=robooze&whichitem=12",
            "Your Robortender can't drink that.",
            inventory,
            prefs,
            itemName = { "milk" },
        )
        assertEquals(false, ok)
        assertEquals("piscatini", prefs.getString("_roboDrinks", ""))
        assertEquals(1, inventory.getCount(12))
    }

    @Test
    fun otherPages_areNotRobooze() {
        val inventory = inventory()
        inventory.gainItemLocally(12, 1)
        val ok = UseItemRobortenderSync.parse(
            "inv_use.php?whichitem=12",
            "He enjoys the drink.",
            inventory,
            itemName = { "milk" },
        )
        assertEquals(false, ok)
        assertEquals(1, inventory.getCount(12))
    }
}
