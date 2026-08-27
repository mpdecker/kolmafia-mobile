package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.isCreateSupported
import net.sourceforge.kolmafia.data.isMeteoroidCraftable
import net.sourceforge.kolmafia.data.isWaxCraftable
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

class CreateItemCraftSyncTest {
    private lateinit var inventory: InventoryManager
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() = runBlocking {
        ItemDatabase.load()
        ConcoctionDatabase.load()
        CreateAbortGate.resetForTest()
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        inventory = InventoryManager(HttpClient(engine), GameEventBus())
        prefs = Preferences(MapSettings())
    }

    @Test
    fun parseCraftingConsumesIngredients() {
        val paste = 25
        inventory.gainItemLocally(1, 5) // seal-clubbing club
        inventory.gainItemLocally(2, 5)
        inventory.gainItemLocally(paste, 5)
        val html = "<!-- cr:2x1,2=3 -->You acquire an item"
        val result = CreateItemCraftSync.parseCrafting(
            location = "craft.php?action=craft&mode=combine&a=1&b=2&qty=2",
            responseText = html,
            inventory = inventory,
            preferences = prefs,
            characterState = KoLCharacter().state.value,
        )
        assertEquals(2, result.created)
        assertEquals(3, inventory.state.value.items[1]?.quantity ?: 0)
        assertEquals(3, inventory.state.value.items[2]?.quantity ?: 0)
        // Non-knoll combine consumes meat paste
        assertEquals(3, inventory.state.value.items[paste]?.quantity ?: 0)
    }

    @Test
    fun parseCraftingFailureReturnsZero() {
        val result = CreateItemCraftSync.parseCrafting(
            location = "craft.php?action=craft&mode=cook",
            responseText = "You can't craft that.",
            inventory = inventory,
            preferences = prefs,
        )
        assertEquals(0, result.created)
    }

    @Test
    fun parseCraftingClearsUnknownRecipe() {
        prefs.setBoolean("unknownRecipe123", true)
        val html = "<!-- cr:1x10,11=123 -->"
        CreateItemCraftSync.parseCrafting(
            location = "craft.php?mode=combine",
            responseText = html,
            inventory = inventory,
            preferences = prefs,
            characterState = KoLCharacter().state.value,
        )
        assertFalse(prefs.getBoolean("unknownRecipe123", true))
    }

    @Test
    fun craftRequestGetAdventuresUsed() {
        assertEquals(3, CraftRequest.getAdventuresUsed("craft.php?mode=cook&qty=3"))
        assertEquals(0, CraftRequest.getAdventuresUsed("inventory.php"))
    }

    @Test
    fun createAbortGateBlocksWhenForced() {
        CreateAbortGate.forceAbort = true
        assertTrue(CreateAbortGate.shouldAbort())
        CreateAbortGate.resetForTest()
        assertFalse(CreateAbortGate.shouldAbort())
    }

    @Test
    fun waxAndMeteoroidAreCreateSupported() {
        val wax = ConcoctionDatabase.getByResult("wax hand")
        val met = ConcoctionDatabase.getByResult("meteortarboard")
        requireNotNull(wax)
        requireNotNull(met)
        assertTrue(wax.isWaxCraftable())
        assertTrue(met.isMeteoroidCraftable())
        assertTrue(wax.isCreateSupported())
        assertTrue(met.isCreateSupported())
    }

    @Test
    fun boxServantRepairPassesWhenAlreadyHasChef() = runBlocking {
        prefs.setBoolean("hasRange", true)
        prefs.setBoolean("hasChef", true)
        val ok = CreateBoxServantRepair.autoRepair(
            method = "COOK_FANCY",
            state = KoLCharacter().state.value,
            preferences = prefs,
            retrieveItemService = null,
            useItemRequest = null,
            inventoryManager = inventory,
        )
        assertTrue(ok)
    }
}
