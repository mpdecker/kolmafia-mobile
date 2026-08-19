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

class GameRuntimeLibraryAshP557Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun bootskin_bare_printsCurrent() {
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.BOOTSKIN, "cowhide")
        val out = outputLib(
            GameRuntimeLibrary(character = character),
            """cli_execute("bootskin");""",
        )
        assertTrue(out.contains("cowhide", ignoreCase = true))
    }

    @Test
    fun bootspur_equip_skipsAlreadyEquipped() {
        val equipCalls = mutableListOf<Pair<String, String>>()
        val inv = object : InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        1 to InventoryItem(1, "diamond-studded", 1, ItemType.OTHER),
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
        character.updateEquipment(EquipmentSlot.BOOTSPUR, "diamond-studded")
        val out = outputLib(
            GameRuntimeLibrary(character = character, inventoryManager = inv),
            """cli_execute("bootspur diamond-studded");""",
        )
        assertTrue(equipCalls.isEmpty())
        assertTrue(out.contains("already equipped", ignoreCase = true))
    }

    @Test
    fun help_listsBootSlots() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help boot");""")
        assertTrue(out.lines().any { it.trim() == "bootskin" })
        assertTrue(out.lines().any { it.trim() == "bootspur" })
    }
}
