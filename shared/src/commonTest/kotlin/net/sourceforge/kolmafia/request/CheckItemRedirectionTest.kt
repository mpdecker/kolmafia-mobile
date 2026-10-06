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

    @Test
    fun sealFigurine_consumesBlubberCandles() {
        val prefs = Preferences(MapSettings())
        val inv = inventory()
        inv.gainItemLocally(CheckItemRedirection.WRETCHED_SEAL, 1)
        inv.gainItemLocally(CheckItemRedirection.SEAL_BLUBBER_CANDLE, 3)
        val result = CheckItemRedirection.apply(
            CheckItemRedirection.WRETCHED_SEAL,
            prefs,
            inv,
        )
        assertTrue(result.handled)
        assertTrue(result.consumed)
        assertEquals(0, inv.getCount(CheckItemRedirection.WRETCHED_SEAL))
        assertEquals(2, inv.getCount(CheckItemRedirection.SEAL_BLUBBER_CANDLE))
        assertEquals(1, prefs.getInt("_sealsSummoned", 0))
    }

    @Test
    fun fossilizedBatSkull_consumesWings() {
        val prefs = Preferences(MapSettings())
        val inv = inventory()
        inv.gainItemLocally(CheckItemRedirection.FOSSILIZED_BAT_SKULL, 1)
        inv.gainItemLocally(CheckItemRedirection.FOSSILIZED_WING, 2)
        val result = CheckItemRedirection.apply(
            CheckItemRedirection.FOSSILIZED_BAT_SKULL,
            prefs,
            inv,
        )
        assertTrue(result.handled)
        assertTrue(result.consumed)
        assertEquals(0, inv.getCount(CheckItemRedirection.FOSSILIZED_WING))
        assertEquals("Fossilized Bat Skull", CheckItemRedirection.itemMonster)
    }

    @Test
    fun genieBottle_earlyReturn_noConsume() {
        val prefs = Preferences(MapSettings())
        val inv = inventory()
        inv.gainItemLocally(CheckItemRedirection.GENIE_BOTTLE, 1)
        val result = CheckItemRedirection.apply(
            CheckItemRedirection.GENIE_BOTTLE,
            prefs,
            inv,
        )
        assertTrue(result.handled)
        assertFalse(result.consumed)
        assertEquals(1, inv.getCount(CheckItemRedirection.GENIE_BOTTLE))
        assertEquals(null, CheckItemRedirection.itemMonster)
    }

    @Test
    fun shakingCrappyCamera_usesDesktopId7176() {
        assertEquals(7176, CheckItemRedirection.SHAKING_CRAPPY_CAMERA)
        val prefs = Preferences(MapSettings())
        prefs.setString("crappyCameraMonster", "Knob Goblin Embezzler")
        val inv = inventory()
        inv.gainItemLocally(7176, 1)
        val result = CheckItemRedirection.apply(7176, prefs, inv)
        assertTrue(result.handled)
        assertTrue(result.consumed)
        assertEquals("", prefs.getString("crappyCameraMonster", "x"))
        assertTrue(prefs.getBoolean("_crappyCameraUsed", false))
        assertEquals(0, inv.getCount(7176))
        assertEquals("Shaking crappy camera", CheckItemRedirection.itemMonster)
    }

    @Test
    fun mapsAndPlans_cluster() {
        val prefs = Preferences(MapSettings())
        val inv = inventory()
        inv.gainItemLocally(CheckItemRedirection.BARREL_MAP, 1)
        inv.gainItemLocally(CheckItemRedirection.RONALD_SHELTER_MAP, 1)

        val blueprints = CheckItemRedirection.apply(
            CheckItemRedirection.FRATHOUSE_BLUEPRINTS, prefs, inv,
        )
        assertTrue(blueprints.handled)
        assertFalse(blueprints.consumed)
        assertEquals("Orcish Frat House blueprints", CheckItemRedirection.itemMonster)

        val barrel = CheckItemRedirection.apply(CheckItemRedirection.BARREL_MAP, prefs, inv)
        assertTrue(barrel.handled)
        assertTrue(barrel.consumed)
        assertEquals(0, inv.getCount(CheckItemRedirection.BARREL_MAP))

        val ronald = CheckItemRedirection.apply(CheckItemRedirection.RONALD_SHELTER_MAP, prefs, inv)
        assertTrue(ronald.handled)
        assertTrue(ronald.consumed)

        val abyssal = CheckItemRedirection.apply(
            CheckItemRedirection.ABYSSAL_BATTLE_PLANS, prefs, inv,
        )
        assertTrue(abyssal.handled)
        assertEquals("abyssal battle plans", CheckItemRedirection.itemMonster)

        val address = CheckItemRedirection.apply(
            CheckItemRedirection.SUSPICIOUS_ADDRESS, prefs, inv,
        )
        assertTrue(address.handled)
        assertEquals("a suspicious address", CheckItemRedirection.itemMonster)
    }

    @Test
    fun iotmToys_cluster() {
        val prefs = Preferences(MapSettings())
        val inv = inventory()
        inv.gainItemLocally(CheckItemRedirection.GIFT_CARD, 1)
        inv.gainItemLocally(CheckItemRedirection.TIME_RESIDUE, 1)
        inv.gainItemLocally(CheckItemRedirection.BASTILLE_LOANER_VOUCHER, 1)

        val chateau = CheckItemRedirection.apply(
            CheckItemRedirection.CHATEAU_WATERCOLOR, prefs, inv,
        )
        assertTrue(chateau.handled)
        assertFalse(chateau.consumed)
        assertTrue(prefs.getBoolean("_chateauMonsterFought", false))
        assertEquals("Chateau Painting", CheckItemRedirection.itemMonster)

        val gift = CheckItemRedirection.apply(CheckItemRedirection.GIFT_CARD, prefs, inv)
        assertTrue(gift.handled)
        assertTrue(gift.consumed)

        val spinner = CheckItemRedirection.apply(CheckItemRedirection.TIME_SPINNER, prefs, inv)
        assertTrue(spinner.handled)
        assertEquals("Time-Spinner", CheckItemRedirection.itemMonster)

        val residue = CheckItemRedirection.apply(CheckItemRedirection.TIME_RESIDUE, prefs, inv)
        assertTrue(residue.handled)
        assertTrue(residue.consumed)

        val bastille = CheckItemRedirection.apply(
            CheckItemRedirection.BASTILLE_LOANER_VOUCHER, prefs, inv,
        )
        assertTrue(bastille.handled)
        assertTrue(bastille.consumed)

        val d10Ok = CheckItemRedirection.apply(CheckItemRedirection.D10, prefs, inv, count = 1)
        assertTrue(d10Ok.handled)
        assertFalse(d10Ok.consumed)
        assertEquals("d10", CheckItemRedirection.itemMonster)

        val d10Skip = CheckItemRedirection.apply(CheckItemRedirection.D10, prefs, inv, count = 2)
        assertFalse(d10Skip.handled)
    }

    @Test
    fun glitchItem_requiresInvEat() {
        val prefs = Preferences(MapSettings())
        val inv = inventory()
        val skipped = CheckItemRedirection.apply(
            CheckItemRedirection.GLITCH_ITEM, prefs, inv, urlString = "inv_use.php",
        )
        assertFalse(skipped.handled)

        val eaten = CheckItemRedirection.apply(
            CheckItemRedirection.GLITCH_ITEM, prefs, inv, urlString = "inv_eat.php",
        )
        assertTrue(eaten.handled)
        assertEquals("[glitch season reward name]", CheckItemRedirection.itemMonster)
    }

    @Test
    fun xiblaxianAndDeskBells_consume() {
        val prefs = Preferences(MapSettings())
        val inv = inventory()
        inv.gainItemLocally(CheckItemRedirection.XIBLAXIAN_HOLOTRAINING_SIMCODE, 1)
        inv.gainItemLocally(CheckItemRedirection.SIZZLING_DESK_BELL, 1)
        inv.gainItemLocally(CheckItemRedirection.GREASY_DESK_BELL, 1)

        val holo = CheckItemRedirection.apply(
            CheckItemRedirection.XIBLAXIAN_HOLOTRAINING_SIMCODE, prefs, inv,
        )
        assertTrue(holo.handled)
        assertTrue(holo.consumed)
        assertEquals(0, inv.getCount(CheckItemRedirection.XIBLAXIAN_HOLOTRAINING_SIMCODE))

        val prisoner = CheckItemRedirection.apply(
            CheckItemRedirection.XIBLAXIAN_POLITICAL_PRISONER, prefs, inv,
        )
        assertTrue(prisoner.handled)
        assertEquals("Xiblaxian encrypted political prisoner", CheckItemRedirection.itemMonster)

        val sizzling = CheckItemRedirection.apply(
            CheckItemRedirection.SIZZLING_DESK_BELL, prefs, inv,
        )
        assertTrue(sizzling.handled)
        assertTrue(sizzling.consumed)
        assertEquals(0, inv.getCount(CheckItemRedirection.SIZZLING_DESK_BELL))

        val greasy = CheckItemRedirection.apply(
            CheckItemRedirection.GREASY_DESK_BELL, prefs, inv,
        )
        assertTrue(greasy.handled)
        assertTrue(greasy.consumed)
    }
}
