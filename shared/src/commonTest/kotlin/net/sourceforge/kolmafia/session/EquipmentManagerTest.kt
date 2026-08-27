package net.sourceforge.kolmafia.session

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
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.UseItemConsumptionSync

class EquipmentManagerTest {
    private lateinit var character: KoLCharacter
    private lateinit var inventory: InventoryManager
    private lateinit var manager: EquipmentManager

    @BeforeTest
    fun setUp() = runBlocking {
        ItemDatabase.load()
        character = KoLCharacter()
        val engine = MockEngine { respond("", HttpStatusCode.OK) }
        inventory = InventoryManager(HttpClient(engine), GameEventBus())
        manager = EquipmentManager(character, inventory)
        ResultProcessor.resetForTest()
        UseItemConsumptionSync.equipmentManagerProvider = { manager }
    }

    @Test
    fun setEquipmentSwapsInventoryAndSlot() {
        val pants = ItemDatabase.getByName("old sweatpants") ?: return
        inventory.gainItemLocally(pants.id, 1)
        manager.setEquipment(EquipmentSlot.PANTS, pants.id, swapInventory = true)
        assertTrue(pants.name.equals(character.state.value.equipment[EquipmentSlot.PANTS], ignoreCase = true))
        assertEquals(0, inventory.state.value.items[pants.id]?.quantity ?: 0)
    }

    @Test
    fun autoequipItemPutsGearInTypeSlot() {
        val pants = ItemDatabase.getByName("old sweatpants") ?: return
        inventory.gainItemLocally(pants.id, 1)
        manager.autoequipItem(pants.id)
        assertTrue(pants.name.equals(character.state.value.equipment[EquipmentSlot.PANTS], ignoreCase = true))
    }

    @Test
    fun canEquipRejectsMissingItem() {
        assertFalse(manager.canEquip(-1))
        assertFalse(manager.canEquip(0))
    }

    @Test
    fun canEquipKnownPants() {
        val pants = ItemDatabase.getByName("old sweatpants") ?: return
        assertTrue(manager.canEquip(pants.id))
        assertEquals(ItemPrimaryUse.PANTS, pants.primaryUse)
    }

    @Test
    fun discardEquipmentClearsSlot() {
        val pants = ItemDatabase.getByName("old sweatpants") ?: return
        character.updateEquipment(EquipmentSlot.PANTS, pants.name)
        manager.discardEquipment(pants.id)
        assertTrue(character.state.value.equipment[EquipmentSlot.PANTS].isNullOrBlank())
    }

    @Test
    fun parseEquipmentChangeEquipUpdatesSlot() {
        val pants = ItemDatabase.getByName("old sweatpants") ?: return
        inventory.gainItemLocally(pants.id, 1)
        val html = "You equip an item: <b>${pants.name}</b>"
        val location = "inv_equip.php?action=equip&whichitem=${pants.id}&ajax=1"
        assertTrue(EquipmentRequest.parseEquipmentChange(location, html, manager))
        assertTrue(pants.name.equals(character.state.value.equipment[EquipmentSlot.PANTS], ignoreCase = true))
    }

    @Test
    fun resultProcessorAutoequip() {
        val pants = ItemDatabase.getByName("old sweatpants") ?: return
        val html = """You acquire an item: <b>${pants.name}</b> (automatically equipped)"""
        ResultProcessor.processResults(
            adventureResults = true,
            html = html,
            inventory = inventory,
            character = character,
            preferences = Preferences(MapSettings()),
            equipmentManager = manager,
        )
        assertTrue(pants.name.equals(character.state.value.equipment[EquipmentSlot.PANTS], ignoreCase = true))
    }

    @Test
    fun useItemBootskinSetsSlot() {
        val skinId = UseItemConsumptionSync.BOOTSKINS.first()
        inventory.gainItemLocally(skinId, 1)
        assertTrue(
            UseItemConsumptionSync.parseConsumption(
                responseText = "You use the mountain skin and slap it on your boots.",
                itemId = skinId,
                count = 1,
                inventory = inventory,
                equipmentManager = manager,
            ),
        )
        assertTrue(
            "mountain lion skin".equals(
                character.state.value.equipment[EquipmentSlot.BOOTSKIN],
                ignoreCase = true,
            ),
        )
    }
}
