package net.sourceforge.kolmafia.equipment

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.OutfitData
import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.CustomOutfitRequest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OutfitManagerTest {

    private val helmet = ItemData(1, "miner's helmet", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 100, null)
    private val mattock = ItemData(2, "7-Foot Dwarven mattock", "", "", ItemPrimaryUse.WEAPON, emptySet(), setOf('t'), 100, null)

    @BeforeTest
    fun setup() {
        OutfitDatabase.clearCustom()
        OutfitDatabase.registerCustom(
            OutfitData(-3, "Test Mining", "", listOf("miner's helmet", "7-Foot Dwarven mattock"), emptyList())
        )
    }

    @AfterTest
    fun teardown() {
        OutfitDatabase.clearCustom()
    }

    private fun manager(
        equipment: Map<EquipmentSlot, String> = emptyMap(),
        inventory: Map<Int, Int> = emptyMap(),
        wearResult: Boolean = true,
    ): OutfitManager {
        val character = KoLCharacter()
        equipment.forEach { (slot, name) -> character.updateEquipment(slot, name) }
        val db = object : GameDatabase() {
            override fun item(id: Int) = when (id) {
                1 -> helmet
                2 -> mattock
                else -> null
            }
            override fun item(name: String) = when (name.lowercase()) {
                "miner's helmet" -> helmet
                "7-foot dwarven mattock" -> mattock
                else -> null
            }
        }
        val invItems = inventory.mapValues { (id, qty) ->
            val item = db.item(id)!!
            InventoryItem(id, item.name, qty, ItemType.OTHER)
        }
        val inventoryManager = object : InventoryManager(HttpClient(MockEngine { respond("") }), GameEventBus()) {
            private val _s = MutableStateFlow(InventoryState(items = invItems))
            override val state: StateFlow<InventoryState> = _s
        }
        val equipmentRequest = object : EquipmentRequest(HttpClient(MockEngine { respond("You put on") })) {
            override suspend fun wearOutfit(outfitId: Int) =
                if (wearResult) Result.success(Unit) else Result.failure(Exception("fail"))
        }
        return OutfitManager(
            retrieveItemService = RetrieveItemService(inventoryManager, null, null, null, null, null, null, null, null, null, db),
            equipmentRequest = equipmentRequest,
            customOutfitRequest = CustomOutfitRequest(HttpClient(MockEngine { respond("") })),
            character = character,
            gameDatabase = db,
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            inventoryManager = inventoryManager,
        )
    }

    @Test
    fun isWearingOutfit_requiresAllPieceCounts() {
        val outfit = ResolvedOutfit(-3, "Test Mining", listOf("miner's helmet", "7-Foot Dwarven mattock"))
        val wearing = mapOf(
            EquipmentSlot.HAT to "miner's helmet",
            EquipmentSlot.WEAPON to "7-Foot Dwarven mattock",
        )
        assertTrue(manager(equipment = wearing).isWearingOutfit(outfit))
        assertFalse(manager(equipment = mapOf(EquipmentSlot.HAT to "miner's helmet")).isWearingOutfit(outfit))
    }

    @Test
    fun hasAllPieces_checksInventory() = runTest {
        val outfit = ResolvedOutfit(-3, "Test Mining", listOf("miner's helmet", "7-Foot Dwarven mattock"))
        assertFalse(manager(inventory = mapOf(1 to 1)).hasAllPieces(outfit))
        assertTrue(manager(inventory = mapOf(1 to 1, 2 to 1)).hasAllPieces(outfit))
    }

    @Test
    fun wearOutfit_resolvesCustomByName() = runTest {
        assertTrue(manager(inventory = mapOf(1 to 1, 2 to 1)).wearOutfit("Test Mining"))
    }

    @Test
    fun getMatchingOutfit_birthdaySuitAlias() {
        val outfit = manager().getMatchingOutfit("birthday suit")
        assertTrue(outfit?.isBirthdaySuit == true)
    }
}
