package net.sourceforge.kolmafia.maximizer

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
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
            4614 -> ItemData(4614, "Crown of Thrones", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
            else -> null
        }
        override fun item(name: String): ItemData? = when (name.lowercase()) {
            "myst hat" -> item(1)
            "plain hat" -> item(2)
            "crown of thrones" -> item(4614)
            else -> null
        }
        override fun itemModifier(name: String): ModifierEntry? = when (name.lowercase()) {
            "myst hat" -> ModifierEntry("Item", "myst hat", "Mysticality: +5")
            "plain hat" -> ModifierEntry("Item", "plain hat", "Mysticality: +1")
            "crown of thrones" -> ModifierEntry("Item", "Crown of Thrones", "Mysticality: +10")
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

    @Test fun maximize_enthroneCarriesCrownOfThrones() = runBlocking {
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    4614 to InventoryItem(4614, "Crown of Thrones", 1, ItemType.HAT),
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
        val familiar = object : FamiliarManager(
            HttpClient(MockEngine { respond("ok") }),
            GameEventBus(),
        ) {
            override suspend fun setEnthroned(name: String): Result<Unit> = Result.success(Unit)
        }.also {
            it.testSetState(
                FamiliarState(
                    ownedFamiliars = listOf(
                        FamiliarData(1, "Mosquito", "Mosquito", 5, 0, 0),
                    ),
                ),
            )
        }
        val mgr = MaximizerManager(StubDb(), inv, equip, character, familiarManager = familiar)
        val result = mgr.maximize("mysticality, enthrone Mosquito")
        assertTrue(result.success)
        assertEquals(4614, equippedId)
        assertEquals("Mosquito", result.enthronedSwitched)
    }

    @Test fun maximize_switchThrall_bindsSkill() = runBlocking {
        net.sourceforge.kolmafia.data.ModifierDatabase.load()
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        preferences.setString("_currentThrall", "Vampieroghi")
        preferences.setString("pastaThrall7", "10,Spice Ghost")
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(InventoryState(items = emptyMap()))
        }
        var castSkillId: Int? = null
        val skillJson = """{"3039":{"name":"Bind Spice Ghost","type":1,"dailylimit":0,"timescast":0,"mpcost":250}}"""
        val skillEngine = MockEngine { request ->
            when {
                request.url.parameters["what"] == "skills" ->
                    respond(skillJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                else -> respond("ok", HttpStatusCode.OK)
            }
        }
        val skillClient = HttpClient(skillEngine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val skills = object : net.sourceforge.kolmafia.skill.SkillManager(
            skillClient,
            net.sourceforge.kolmafia.skill.SkillCastRequest(skillClient),
            GameEventBus(),
        ) {
            override suspend fun cast(skill: net.sourceforge.kolmafia.skill.SkillData, quantity: Int): Result<Unit> {
                castSkillId = skill.id
                return Result.success(Unit)
            }
        }
        skills.fetchSkills()
        val equip = EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        )
        val mgr = MaximizerManager(
            StubDb(), inv, equip, character,
            preferences = preferences,
            skillManager = skills,
        )
        val result = mgr.maximize("item, switch thrall Spice Ghost")
        assertTrue(result.success)
        assertEquals("Spice Ghost", result.thrallSwitched)
        assertEquals(3039, castSkillId)
    }
}
