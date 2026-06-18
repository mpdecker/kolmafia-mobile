package net.sourceforge.kolmafia.adventure

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class RufusManagerShadowRiftNcTest {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun handleShadowRiftNC_choice1499_resetsEncounterCounter() {
        val testPrefs = prefs()
        testPrefs.setInt("encountersUntilSRChoice", 3)
        RufusManager(testPrefs).handleShadowRiftNC(1499)
        assertEquals(11, testPrefs.getInt("encountersUntilSRChoice", 0))
    }

    @Test
    fun handleShadowRiftNC_choice1500_consumesLodestone() {
        val testPrefs = prefs()
        val inv = stubInventory(ItemPool.RUFUS_SHADOW_LODESTONE, 1)
        RufusManager(testPrefs).handleShadowRiftNC(1500, inv)
        assertNull(inv.state.value.items[ItemPool.RUFUS_SHADOW_LODESTONE])
    }

    @Test
    fun questChoiceRules_choice1500_removesLodestone() {
        val testPrefs = prefs()
        val inv = stubInventory(ItemPool.RUFUS_SHADOW_LODESTONE, 1)
        val db = QuestDatabase(testPrefs)
        QuestChoiceRules.apply(1500, "You use the lodestone.", db, preferences = testPrefs, inventoryManager = inv)
        assertNull(inv.state.value.items[ItemPool.RUFUS_SHADOW_LODESTONE])
    }

    private fun stubInventory(itemId: Int, quantity: Int): InventoryManager {
        val items = mapOf(itemId to InventoryItem(itemId, "lodestone", quantity, ItemType.OTHER))
        return object : InventoryManager(
            client = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }),
            eventBus = GameEventBus(),
        ) {
            init {
                _state.value = InventoryState(items = items)
            }
        }
    }
}
