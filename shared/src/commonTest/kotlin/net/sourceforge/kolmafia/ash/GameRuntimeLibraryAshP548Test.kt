package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType

class GameRuntimeLibraryAshP548Test {

    @Test
    fun revision_phase550() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun equip_alreadyEquipped_skipsHttpAndPrints() {
        val equipCalls = mutableListOf<Pair<String, String>>()
        val inv = object : InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        1 to InventoryItem(1, "helmet turtle", 1, ItemType.HAT),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
            override suspend fun equipItem(item: InventoryItem, slot: String): Result<Unit> {
                equipCalls += item.name to slot
                return Result.success(Unit)
            }
        }
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.HAT, "helmet turtle")
        val out = outputLib(
            GameRuntimeLibrary(inventoryManager = inv, character = character),
            """cli_execute("equip helmet turtle");""",
        )
        assertTrue(equipCalls.isEmpty())
        assertTrue(out.contains("already equipped", ignoreCase = true))
        assertTrue(out.contains("helmet turtle", ignoreCase = true))
    }

    @Test
    fun equip_slotAlreadyEquipped_skipsHttp() {
        val equipCalls = mutableListOf<Pair<String, String>>()
        val inv = object : InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        1 to InventoryItem(1, "helmet turtle", 1, ItemType.HAT),
                    ),
                ),
            )
            override val state = flow.asStateFlow()
            override suspend fun equipItem(item: InventoryItem, slot: String): Result<Unit> {
                equipCalls += item.name to slot
                return Result.success(Unit)
            }
        }
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.HAT, "helmet turtle")
        val out = outputLib(
            GameRuntimeLibrary(inventoryManager = inv, character = character),
            """cli_execute("equip hat helmet turtle");""",
        )
        assertTrue(equipCalls.isEmpty())
        assertTrue(out.contains("already equipped", ignoreCase = true))
    }
}
