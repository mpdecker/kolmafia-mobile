package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UseItemBingeSyncTest {

    private fun inventory(): InventoryManager =
        InventoryManager(
            HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            GameEventBus(),
        )

    private fun familiars(active: FamiliarData): FamiliarManager {
        val manager = FamiliarManager(
            HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            GameEventBus(),
        )
        manager.testSetState(FamiliarState(activeFamiliar = active, ownedFamiliars = listOf(active)))
        return manager
    }

    @Test
    fun ghost_growsAndConsumes() {
        val char = KoLCharacter().also { it.updateFamiliar(74, "Boo", 1, 4) }
        val inventory = inventory()
        inventory.gainItemLocally(12, 3)
        val manager = familiars(
            FamiliarData(74, "Boo", "Gluttonous Green Ghost", 1, 4, 0),
        )
        val ok = UseItemBingeSync.parse(
            "https://www.kingdomofloathing.com/familiarbinger.php?whichitem=12&action=binge&qty=3",
            "Boo takes the milk and quickly consumes them. He grows a bit.",
            character = char,
            familiarManager = manager,
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals(7, char.state.value.familiarExp)
        assertEquals(7, manager.state.value.activeFamiliar?.experience)
        assertEquals(0, inventory.getCount(12))
    }

    @Test
    fun wrongFamiliar_returnsFalseAndKeepsTheItem() {
        val inventory = inventory()
        inventory.gainItemLocally(12, 1)
        val ok = UseItemBingeSync.parse(
            "familiarbinger.php?whichitem=12&qty=1",
            "Looks like you don't currently have a familiar capable of binging.",
            inventory = inventory,
            familiarId = UseItemBingeLog.GHOST,
        )
        assertEquals(false, ok)
        assertEquals(1, inventory.getCount(12))
    }

    @Test
    fun shortCount_doesNotConsume() {
        val inventory = inventory()
        inventory.gainItemLocally(12, 1)
        val ok = UseItemBingeSync.parse(
            "inventory.php?action=hobo&whichitem=12&qty=4",
            "You don't have that many of those.",
            inventory = inventory,
        )
        assertTrue(ok)
        assertEquals(1, inventory.getCount(12))
    }

    @Test
    fun uninterested_doesNotConsume() {
        val inventory = inventory()
        inventory.gainItemLocally(12, 1)
        UseItemBingeSync.parse(
            "familiarbinger.php?action=candy&whichitem=12&qty=1",
            "The mimic approaches the milk but doesn't seem interested.",
            inventory = inventory,
        )
        assertEquals(1, inventory.getCount(12))
    }

    @Test
    fun slimeling_recordsStacksForTheAutoplunger() {
        val prefs = Preferences(MapSettings())
        val inventory = inventory()
        inventory.gainItemLocally(127, 4)
        UseItemBingeSync.parse(
            "inventory.php?action=slime&whichitem=127&qty=4",
            "Your Slimeling gulps it down.",
            inventory = inventory,
            preferences = prefs,
            familiarId = UseItemBingeLog.SLIMELING,
        )
        assertEquals(4, prefs.getInt("slimelingStacksDue", 0))
        assertEquals(0f, prefs.getFloat("slimelingFullness", 0f))
        assertEquals(0, inventory.getCount(127))
    }

    @Test
    fun slimeling_estimatesFullnessFromItemPower() {
        val prefs = Preferences(MapSettings())
        prefs.setFloat("slimelingFullness", 1.5f)
        val inventory = inventory()
        inventory.gainItemLocally(12, 2)
        UseItemBingeSync.parse(
            "inventory.php?which=2&action=slime&whichitem=12&qty=2",
            "Your Slimeling gulps it down.",
            inventory = inventory,
            preferences = prefs,
            familiarId = UseItemBingeLog.SLIMELING,
            itemPower = { 15 },
        )
        assertEquals(4.5f, prefs.getFloat("slimelingFullness", 0f))
        assertEquals(0, inventory.getCount(12))
    }

    @Test
    fun slimeling_recordsStacksWhenTheItemUsesAMeatStack() {
        val prefs = Preferences(MapSettings())
        UseItemBingeSync.parse(
            "familiarbinger.php?whichitem=258&qty=1",
            "Your Slimeling gulps it down.",
            preferences = prefs,
            familiarId = UseItemBingeLog.SLIMELING,
            usesMeatStack = { it == 258 },
        )
        assertEquals(1, prefs.getInt("slimelingStacksDue", 0))
    }

    @Test
    fun otherPages_areNotFeeds() {
        assertNull(UseItemBingeSync.bingedItem("inventory.php?whichitem=12"))
        assertNull(UseItemBingeSync.bingedItem("inv_use.php?whichitem=12&qty=1"))
        val inventory = inventory()
        inventory.gainItemLocally(12, 1)
        UseItemBingeSync.parse(
            "inventory.php?whichitem=12&qty=1",
            "He grows a bit.",
            inventory = inventory,
            familiarId = UseItemBingeLog.GHOST,
        )
        assertEquals(1, inventory.getCount(12))
    }
}
