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
import net.sourceforge.kolmafia.shop.FolderHolderAccessibility

class GameRuntimeLibraryAshP546Test {

    private fun invWith(
        items: Map<Int, InventoryItem>,
        onEquip: (InventoryItem, String) -> Unit = { _, _ -> },
    ): InventoryManager =
        object : InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            private val flow = MutableStateFlow(InventoryState(items = items))
            override val state = flow.asStateFlow()
            override suspend fun equipItem(item: InventoryItem, slot: String): Result<Unit> {
                onEquip(item, slot)
                return Result.success(Unit)
            }
        }

    @Test
    fun revision_phase550() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun folders_listsEmptySlots() {
        val inv = invWith(
            mapOf(
                FolderHolderAccessibility.FOLDER_HOLDER to InventoryItem(
                    FolderHolderAccessibility.FOLDER_HOLDER,
                    "over-the-shoulder Folder Holder",
                    1,
                    ItemType.OTHER,
                ),
            ),
        )
        val character = KoLCharacter()
        val out = outputLib(
            GameRuntimeLibrary(inventoryManager = inv, character = character),
            """cli_execute("folders");""",
        )
        assertTrue(out.contains("Folder 1:"))
        assertTrue(out.contains("Folder 2:"))
        assertTrue(out.contains("Folder 3:"))
    }

    @Test
    fun folders_equipsIntoEmptySlot() {
        val equipCalls = mutableListOf<Pair<String, String>>()
        val inv = invWith(
            mapOf(
                FolderHolderAccessibility.FOLDER_HOLDER to InventoryItem(
                    FolderHolderAccessibility.FOLDER_HOLDER,
                    "over-the-shoulder Folder Holder",
                    1,
                    ItemType.OTHER,
                ),
                9001 to InventoryItem(9001, "folder (aggressive)", 1, ItemType.OTHER),
            ),
        ) { item, slot -> equipCalls += item.name to slot }
        val lib = GameRuntimeLibrary(inventoryManager = inv, character = KoLCharacter())
        outputLib(lib, """cli_execute("folders folder (aggressive)");""")
        assertEquals(listOf("folder (aggressive)" to EquipmentSlot.FOLDER1.apiKey), equipCalls)
    }

    @Test
    fun folders_missingHolder_printsError() {
        val out = outputLib(
            GameRuntimeLibrary(inventoryManager = invWith(emptyMap()), character = KoLCharacter()),
            """cli_execute("folders");""",
        )
        assertTrue(out.contains("You need a folder holder."))
    }
}
