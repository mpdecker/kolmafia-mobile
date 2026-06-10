package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.request.AutosellRequest
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeInventoryManager(
    initialItems: Map<Int, InventoryItem> = emptyMap(),
    val equipCalls: MutableList<Pair<String, String>> = mutableListOf(),
    val unequipCalls: MutableList<String> = mutableListOf(),
) : InventoryManager(
    HttpClient(MockEngine { respond("") }),
    GameEventBus()
) {
    private val _items: Map<Int, InventoryItem> = initialItems

    override val state get() = kotlinx.coroutines.flow.MutableStateFlow(
        InventoryState(items = _items)
    )

    override suspend fun equipItem(item: InventoryItem, slot: String): Result<Unit> {
        equipCalls.add(item.name to slot)
        return Result.success(Unit)
    }

    override suspend fun unequipSlot(slot: String): Result<Unit> {
        unequipCalls.add(slot)
        return Result.success(Unit)
    }
}

private class FakeAutosellRequest(
    val sellCalls: MutableList<Pair<Int, Int>> = mutableListOf()
) : AutosellRequest(HttpClient(MockEngine { respond("") })) {
    override suspend fun autosell(itemId: Int, quantity: Int): Result<String> {
        sellCalls.add(itemId to quantity)
        return Result.success("ok")
    }
}

private class EquipDb : GameDatabase() {
    private val sword = ItemData(
        id = 10,
        name = "rusty sword",
        descId = "desc",
        image = "sword.gif",
        primaryUse = ItemPrimaryUse.WEAPON,
        secondaryUses = emptySet(),
        access = setOf('t', 'd'),
        autosellPrice = 5,
        plural = null
    )
    override fun item(name: String): ItemData? =
        if (name.equals("rusty sword", ignoreCase = true)) sword else null
    override fun item(id: Int): ItemData? = if (id == 10) sword else null
}

class GameRuntimeLibraryCliEquipTest {

    private fun swordInBackpack() = mapOf(
        10 to InventoryItem(10, "rusty sword", 1, ItemType.WEAPON)
    )

    @Test
    fun cliExecute_equipWithSlot_callsEquipItem() {
        val equipCalls = mutableListOf<Pair<String, String>>()
        val invMgr = FakeInventoryManager(swordInBackpack(), equipCalls)
        val lib = GameRuntimeLibrary(inventoryManager = invMgr)
        runLib(lib, """cli_execute("equip weapon rusty sword");""")
        assertEquals(listOf("rusty sword" to "weapon"), equipCalls)
    }

    @Test
    fun cliExecute_equipWithSlotUnknownSlot_echoesCommand() {
        val invMgr = FakeInventoryManager(swordInBackpack())
        val lib = GameRuntimeLibrary(inventoryManager = invMgr)
        val out = outputLib(lib, """cli_execute("equip badslot rusty sword");""")
        assertEquals("[cli] equip: unknown slot badslot", out)
    }

    @Test
    fun cliExecute_equipNoSlot_callsEquipItemWithDefault() {
        val equipCalls = mutableListOf<Pair<String, String>>()
        val invMgr = FakeInventoryManager(swordInBackpack(), equipCalls)
        val lib = GameRuntimeLibrary(inventoryManager = invMgr)
        runLib(lib, """cli_execute("equip rusty sword");""")
        assertEquals(listOf("rusty sword" to "default"), equipCalls)
    }

    @Test
    fun cliExecute_equipNoSlot_silentNoOpForUnknownItem() {
        val equipCalls = mutableListOf<Pair<String, String>>()
        val invMgr = FakeInventoryManager(emptyMap(), equipCalls)
        val lib = GameRuntimeLibrary(inventoryManager = invMgr)
        val out = outputLib(lib, """cli_execute("equip mystery item");""")
        assertEquals("", out)
        assertEquals(emptyList<Pair<String, String>>(), equipCalls)
    }

    @Test
    fun cliExecute_unequip_callsUnequipSlot() {
        val unequipCalls = mutableListOf<String>()
        val invMgr = FakeInventoryManager(unequipCalls = unequipCalls)
        val lib = GameRuntimeLibrary(inventoryManager = invMgr)
        runLib(lib, """cli_execute("unequip weapon");""")
        assertEquals(listOf("weapon"), unequipCalls)
    }

    @Test
    fun cliExecute_sell_callsAutosell() {
        val sellCalls = mutableListOf<Pair<Int, Int>>()
        val fakeAutosell = FakeAutosellRequest(sellCalls)
        val lib = GameRuntimeLibrary(gameDatabase = EquipDb(), autosellRequest = fakeAutosell)
        runLib(lib, """cli_execute("sell 5 rusty sword");""")
        assertEquals(listOf(10 to 5), sellCalls)
    }

    @Test
    fun cliExecute_autosell_callsAutosell() {
        val sellCalls = mutableListOf<Pair<Int, Int>>()
        val fakeAutosell = FakeAutosellRequest(sellCalls)
        val lib = GameRuntimeLibrary(gameDatabase = EquipDb(), autosellRequest = fakeAutosell)
        runLib(lib, """cli_execute("autosell 3 rusty sword");""")
        assertEquals(listOf(10 to 3), sellCalls)
    }

    @Test
    fun cliExecute_sell_silentNoOpForUnknownItem() {
        val sellCalls = mutableListOf<Pair<Int, Int>>()
        val fakeAutosell = FakeAutosellRequest(sellCalls)
        val lib = GameRuntimeLibrary(gameDatabase = EquipDb(), autosellRequest = fakeAutosell)
        val out = outputLib(lib, """cli_execute("sell 1 no such thing");""")
        assertEquals("", out)
        assertEquals(emptyList<Pair<Int, Int>>(), sellCalls)
    }
}
