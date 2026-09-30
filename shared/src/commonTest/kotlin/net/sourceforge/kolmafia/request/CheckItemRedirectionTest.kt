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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckItemRedirectionTest {

    private fun inventory(): InventoryManager =
        InventoryManager(
            HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            GameEventBus(),
        )

    @Test
    fun photocopiedMonster_consumesAndClearsPref() {
        val prefs = Preferences(MapSettings())
        prefs.setString("photocopyMonster", "Knob Goblin Embezzler")
        val inv = inventory()
        inv.gainItemLocally(CheckItemRedirection.PHOTOCOPIED_MONSTER, 1)
        val result = CheckItemRedirection.apply(
            CheckItemRedirection.PHOTOCOPIED_MONSTER,
            prefs,
            inv,
        )
        assertTrue(result.handled)
        assertTrue(result.consumed)
        assertEquals("", prefs.getString("photocopyMonster", "x"))
        assertTrue(prefs.getBoolean("_photocopyUsed", false))
        assertEquals(0, inv.getCount(CheckItemRedirection.PHOTOCOPIED_MONSTER))
        assertEquals("photocopied monster", CheckItemRedirection.itemMonster)
    }

    @Test
    fun spookyPutty_returnsSheet() {
        val prefs = Preferences(MapSettings())
        prefs.setString("spookyPuttyMonster", "snakefly")
        val inv = inventory()
        inv.gainItemLocally(CheckItemRedirection.SPOOKY_PUTTY_MONSTER, 1)
        val result = CheckItemRedirection.apply(
            CheckItemRedirection.SPOOKY_PUTTY_MONSTER,
            prefs,
            inv,
        )
        assertTrue(result.handled)
        assertEquals("", prefs.getString("spookyPuttyMonster", "x"))
        assertEquals(0, inv.getCount(CheckItemRedirection.SPOOKY_PUTTY_MONSTER))
        assertEquals(1, inv.getCount(CheckItemRedirection.SPOOKY_PUTTY_SHEET))
    }

    @Test
    fun lynyrdSnare_setsAdventureAndIncrements() {
        val prefs = Preferences(MapSettings())
        val inv = inventory()
        inv.gainItemLocally(CheckItemRedirection.LYNYRD_SNARE, 2)
        val result = CheckItemRedirection.apply(
            CheckItemRedirection.LYNYRD_SNARE,
            prefs,
            inv,
            count = 1,
        )
        assertTrue(result.handled)
        assertEquals(1, prefs.getInt("_lynyrdSnareUses", 0))
        assertEquals(1, inv.getCount(CheckItemRedirection.LYNYRD_SNARE))
    }

    @Test
    fun unknownItem_notHandled() {
        val result = CheckItemRedirection.apply(1, Preferences(MapSettings()), inventory())
        assertFalse(result.handled)
        assertFalse(result.consumed)
    }
}
