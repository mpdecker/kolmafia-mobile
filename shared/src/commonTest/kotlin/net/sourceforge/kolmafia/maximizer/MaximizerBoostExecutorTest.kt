package net.sourceforge.kolmafia.maximizer

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.request.EquipmentRequest
import kotlinx.coroutines.flow.MutableStateFlow

class MaximizerBoostExecutorTest {

    private val db = object : GameDatabase() {
        override fun item(id: Int): ItemData? = when (id) {
            100 -> ItemData(100, "test item", "", "", ItemPrimaryUse.USABLE, emptySet(), setOf('t'), 0, null)
            else -> null
        }
        override fun item(name: String): ItemData? = null
    }

    private fun executor(
        cliCommands: MutableList<String>,
        cliResult: Boolean = true,
    ): MaximizerBoostExecutor {
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(InventoryState(items = emptyMap()))
        }
        val equip = EquipmentRequest(HttpClient(MockEngine { respond("ok") }), character = character)
        return MaximizerBoostExecutor(
            gameDatabase = db,
            inventoryManager = inv,
            equipmentRequest = equip,
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            familiarManager = null,
            modeableRequest = null,
            retrieveItemService = null,
            mallManager = null,
            cliExecutor = { cmd ->
                cliCommands += cmd
                cliResult
            },
        )
    }

    @Test
    fun execute_cliFallbackForCastCommand() = runBlocking {
        val commands = mutableListOf<String>()
        val exec = executor(commands)
        assertTrue(exec.execute("cast 1 Patience of the Tortoise"))
        assertEquals(listOf("cast 1 Patience of the Tortoise"), commands)
    }

    @Test
    fun execute_retrievePrefixThenCastUsesCliFallback() = runBlocking {
        val commands = mutableListOf<String>()
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(InventoryState(items = emptyMap()))
        }
        val equip = EquipmentRequest(HttpClient(MockEngine { respond("ok") }), character = character)
        val retrieve = object : RetrieveItemService(
            inventoryManager = inv,
            closetRequest = null,
            storageRequest = null,
            npcBuyRequest = null,
            mallManager = null,
            gameDatabase = db,
        ) {
            override suspend fun retrieve(itemId: Int, qty: Int): Int = 1
        }
        val exec = MaximizerBoostExecutor(
            gameDatabase = db,
            inventoryManager = inv,
            equipmentRequest = equip,
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            familiarManager = null,
            modeableRequest = null,
            retrieveItemService = retrieve,
            mallManager = null,
            cliExecutor = { cmd ->
                commands += cmd
                true
            },
        )
        assertTrue(exec.execute("make \u00B6100;cast 1 Patience of the Tortoise"))
        assertEquals(listOf("cast 1 Patience of the Tortoise"), commands)
    }

    @Test
    fun execute_unknownCommandWithoutCliExecutorFails() = runBlocking {
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(InventoryState(items = emptyMap()))
        }
        val equip = EquipmentRequest(HttpClient(MockEngine { respond("ok") }), character = character)
        val exec = MaximizerBoostExecutor(
            gameDatabase = db,
            inventoryManager = inv,
            equipmentRequest = equip,
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            familiarManager = null,
            modeableRequest = null,
            retrieveItemService = null,
            mallManager = null,
        )
        assertFalse(exec.execute("cast 1 Unknown Skill"))
    }

    @Test
    fun execute_equipStillHandledWithoutCli() = runBlocking {
        val commands = mutableListOf<String>()
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(InventoryState(items = emptyMap()))
        }
        val equip = object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> =
                Result.success(Unit)
        }
        val exec = MaximizerBoostExecutor(
            gameDatabase = db,
            inventoryManager = inv,
            equipmentRequest = equip,
            closetRequest = null,
            storageRequest = null,
            displayCaseRequest = null,
            clanStashRequest = null,
            familiarManager = null,
            modeableRequest = null,
            retrieveItemService = null,
            mallManager = null,
            cliExecutor = { cmd ->
                commands += cmd
                true
            },
        )
        assertTrue(exec.execute("equip HAT \u00B6100"))
        assertTrue(commands.isEmpty())
    }
}
