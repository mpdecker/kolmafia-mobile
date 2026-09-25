package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UseItemAbsorbSyncTest {

    private fun noob(absorbs: Int = 0, level: Int = 1): KoLCharacter =
        KoLCharacter().also { char ->
            char.setChallengePath("Gelatinous Noob")
            char.setLevel(level)
            char.updateClassResource(absorbs = absorbs)
        }

    private fun inventory(): InventoryManager =
        InventoryManager(
            HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            GameEventBus(),
        )

    @Test
    fun success_countsLogsAndConsumes() {
        val char = noob()
        val prefs = Preferences(MapSettings())
        val logger = SessionLogger(prefs, GameEventBus())
        val inventory = inventory()
        inventory.gainItemLocally(12, 2)
        UseItemAbsorbSync.apply(
            url = "https://www.kingdomofloathing.com/inventory.php?absorb=12",
            responseText = "You absorb some new knowledge of humanity!",
            character = char,
            inventory = inventory,
            preferences = prefs,
            sessionLogger = logger,
            itemName = { "milk" },
        )
        assertEquals(1, char.state.value.absorbs)
        assertEquals(1, prefs.getInt("_noobSkillCount", 0))
        assertEquals(1, inventory.getCount(12))
        assertTrue(logger.recentLines().any { it == "Absorbing milk" })
    }

    @Test
    fun youAbsorbThe_andEnergyLine_alsoSucceed() {
        val char = noob()
        val inventory = inventory()
        inventory.gainItemLocally(4, 1)
        UseItemAbsorbSync.apply(
            "inventory.php?absorb=4",
            "You absorb the potion.",
            char,
            inventory,
            itemName = { "potion" },
        )
        assertEquals(1, char.state.value.absorbs)
        assertEquals(0, inventory.getCount(4))

        val energy = noob()
        UseItemAbsorbSync.apply(
            "inventory.php?absorb=4",
            "You don't gain any new knowledge from absorbing that item, but you're able to extract a lot of energy from it!",
            energy,
            itemName = { "potion" },
        )
        assertEquals(1, energy.state.value.absorbs)
    }

    @Test
    fun tooImportant_doesNotCountOrConsume() {
        val char = noob(absorbs = 2)
        val inventory = inventory()
        inventory.gainItemLocally(9, 1)
        UseItemAbsorbSync.apply(
            "inventory.php?absorb=9",
            "That's too important to absorb.",
            char,
            inventory,
            itemName = { "quest item" },
        )
        assertEquals(2, char.state.value.absorbs)
        assertEquals(1, inventory.getCount(9))
    }

    @Test
    fun missingItem_doesNotCount() {
        val char = noob()
        UseItemAbsorbSync.apply(
            "inventory.php?absorb=9",
            "You can't absorb something you don't have.",
            char,
        )
        assertEquals(0, char.state.value.absorbs)
    }

    @Test
    fun outsideNoobcore_isIgnored() {
        val char = KoLCharacter()
        val inventory = inventory()
        inventory.gainItemLocally(12, 1)
        UseItemAbsorbSync.apply(
            "inventory.php?absorb=12",
            "You absorb some new knowledge of humanity!",
            char,
            inventory,
        )
        assertEquals(0, char.state.value.absorbs)
        assertEquals(1, inventory.getCount(12))
    }

    @Test
    fun urlWithoutAbsorb_isIgnored() {
        assertNull(UseItemAbsorbSync.absorbedItemId("inventory.php?which=3"))
        assertNull(UseItemAbsorbSync.absorbedItemId("inv_use.php?absorb=12"))
        val char = noob()
        UseItemAbsorbSync.apply(
            "inv_use.php?whichitem=12",
            "You absorb the residual paste into your soul",
            char,
        )
        assertEquals(0, char.state.value.absorbs)
    }

    @Test
    fun count_clampsAtTheAbsorbLimit() {
        val char = noob(absorbs = 15, level = 13)
        UseItemAbsorbSync.apply(
            "inventory.php?absorb=12",
            "absorb some new knowledge",
            char,
            itemName = { "milk" },
        )
        assertEquals(15, char.state.value.absorbs)
    }
}
