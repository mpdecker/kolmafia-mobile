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

class GameRuntimeLibraryAshP556Test {

    @Test
    fun revision_phase556() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun cardsleeve_bare_printsCurrent() {
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.CARDSLEEVE, "Alice's Army card")
        val out = outputLib(
            GameRuntimeLibrary(character = character),
            """cli_execute("cardsleeve");""",
        )
        assertTrue(out.contains("Alice's Army card", ignoreCase = true))
    }

    @Test
    fun cardsleeve_equip_skipsAlreadyEquipped() {
        val equipCalls = mutableListOf<Pair<String, String>>()
        val inv = object : InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(
                InventoryState(
                    items = mapOf(
                        9148 to InventoryItem(9148, "Alice's Army card", 1, ItemType.OTHER),
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
        character.updateEquipment(EquipmentSlot.CARDSLEEVE, "Alice's Army card")
        val out = outputLib(
            GameRuntimeLibrary(character = character, inventoryManager = inv),
            """cli_execute("cardsleeve Alice's Army card");""",
        )
        assertTrue(equipCalls.isEmpty())
        assertTrue(out.contains("already equipped", ignoreCase = true))
    }

    @Test
    fun help_listsCardsleeve() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help cardsleeve");""")
        assertTrue(out.lines().any { it.trim() == "cardsleeve" })
    }
}
