package net.sourceforge.kolmafia.maximizer

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaximizerManagerTest {

    private class StubDb : GameDatabase() {
        override fun item(id: Int): ItemData? = when (id) {
            1 -> ItemData(1, "myst hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
            2 -> ItemData(2, "plain hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
            else -> null
        }
        override fun item(name: String): ItemData? = when (name.lowercase()) {
            "myst hat" -> item(1)
            "plain hat" -> item(2)
            else -> null
        }
        override fun itemModifier(name: String): ModifierEntry? = when (name.lowercase()) {
            "myst hat" -> ModifierEntry("Item", "myst hat", "Mysticality: +5")
            "plain hat" -> ModifierEntry("Item", "plain hat", "Mysticality: +1")
            else -> null
        }
    }

    @Test fun maximize_equipsBestItem() = runBlocking {
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.HAT, "plain hat")
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    1 to InventoryItem(1, "myst hat", 1, ItemType.HAT),
                    2 to InventoryItem(2, "plain hat", 1, ItemType.HAT),
                ))
            )
        }
        var equippedId: Int? = null
        val equip = object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
                equippedId = itemId
                return Result.success(Unit)
            }
        }
        val mgr = MaximizerManager(StubDb(), inv, equip, character)
        val result = mgr.maximize("mysticality")
        assertTrue(result.success)
        assertEquals(1, equippedId)
    }

    @Test fun maximize_noImprovement_returnsFalse() = runBlocking {
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.HAT, "myst hat")
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    1 to InventoryItem(1, "myst hat", 1, ItemType.HAT),
                ))
            )
        }
        val equip = EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        )
        val mgr = MaximizerManager(StubDb(), inv, equip, character)
        assertFalse(mgr.maximize("mysticality").success)
    }

    @Test fun maximize_retrievesFromCloset_whenBestNotInInventory() = runBlocking {
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.HAT, "plain hat")
        var fetchCount = 0
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(InventoryState(items = emptyMap()))
            override suspend fun fetchInventory() {
                fetchCount++
                state.value = InventoryState(items = mapOf(
                    1 to InventoryItem(1, "myst hat", 1, ItemType.HAT),
                ))
            }
        }
        val closet = object : ClosetRequest(HttpClient(MockEngine { respond("ok") })) {
            override suspend fun fetchContents() = mapOf(1 to 1)
            override suspend fun takeOut(itemId: Int, quantity: Int) = Result.success("ok")
        }
        var equippedId: Int? = null
        val equip = object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
                equippedId = itemId
                return Result.success(Unit)
            }
        }
        val mgr = MaximizerManager(StubDb(), inv, equip, character, closet)
        assertTrue(mgr.maximize("mysticality").success)
        assertEquals(1, equippedId)
        assertTrue(fetchCount >= 1)
    }

    @Test fun maximize_retrievesFromDisplayCase_whenBestNotInInventory() = runBlocking {
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(InventoryState(items = emptyMap()))
            override suspend fun fetchInventory() {
                state.value = InventoryState(items = mapOf(
                    1 to InventoryItem(1, "myst hat", 1, ItemType.HAT),
                ))
            }
        }
        val display = object : DisplayCaseRequest(HttpClient(MockEngine { respond("ok") })) {
            override suspend fun fetchContents() = mapOf(1 to 1)
            override suspend fun takeOut(itemId: Int, quantity: Int) = Result.success("ok")
        }
        var equippedId: Int? = null
        val equip = object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
                equippedId = itemId
                return Result.success(Unit)
            }
        }
        val mgr = MaximizerManager(StubDb(), inv, equip, character, displayCaseRequest = display)
        assertTrue(mgr.maximize("mysticality").success)
        assertEquals(1, equippedId)
    }

    @Test fun maximize_retrievesFromClanStash_whenBestNotInInventory() = runBlocking {
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(InventoryState(items = emptyMap()))
            override suspend fun fetchInventory() {
                state.value = InventoryState(items = mapOf(
                    1 to InventoryItem(1, "myst hat", 1, ItemType.HAT),
                ))
            }
        }
        val stash = object : ClanStashRequest(HttpClient(MockEngine { respond("ok") })) {
            override suspend fun fetchContents() = mapOf(1 to 1)
            override suspend fun takeOut(itemId: Int, quantity: Int) = Result.success("ok")
        }
        var equippedId: Int? = null
        val equip = object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
                equippedId = itemId
                return Result.success(Unit)
            }
        }
        val mgr = MaximizerManager(StubDb(), inv, equip, character, clanStashRequest = stash)
        assertTrue(mgr.maximize("mysticality").success)
        assertEquals(1, equippedId)
    }

    @Test fun maximize_switchFamiliar_beforeEquip() = runBlocking {
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    1 to InventoryItem(1, "myst hat", 1, ItemType.HAT),
                ))
            )
        }
        var switched: String? = null
        val familiar = object : FamiliarManager(
            HttpClient(MockEngine { respond("ok") }),
            GameEventBus(),
        ) {
            override suspend fun setFamiliar(name: String): Result<Unit> {
                switched = name
                return Result.success(Unit)
            }
        }.also {
            it.testSetState(
                FamiliarState(
                    ownedFamiliars = listOf(
                        FamiliarData(1, "Donkey", "Miniature Donkey", 5, 0, 0),
                    ),
                ),
            )
        }
        val equip = EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        )
        val mgr = MaximizerManager(StubDb(), inv, equip, character, familiarManager = familiar)
        val result = mgr.maximize("mysticality, switch Miniature Donkey")
        assertTrue(result.success)
        assertEquals("Miniature Donkey", result.familiarSwitched)
        assertEquals("Miniature Donkey", switched)
    }
}
