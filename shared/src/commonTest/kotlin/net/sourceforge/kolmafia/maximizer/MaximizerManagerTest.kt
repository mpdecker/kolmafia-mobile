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
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.data.ModifierDatabase
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
import net.sourceforge.kolmafia.request.ModeableRequest
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.EquipmentData
import net.sourceforge.kolmafia.data.ItemDatabase

class MaximizerManagerTest {

    @AfterTest
    fun cleanupCreatableFixtures() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    private fun GameDatabase.syncTestItemModifiers(vararg names: String) {
        runBlocking { ModifierDatabase.load() }
        for (name in names) {
            itemModifier(name)?.let { entry ->
                ModifierDatabase.overrideModifier("Item", entry.name, entry.modifiers)
            }
        }
    }

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
        val db = StubDb().also {
            it.syncTestItemModifiers("myst hat", "plain hat")
        }
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
        val mgr = MaximizerManager(db, inv, equip, character)
        val result = mgr.maximize("mysticality")
        assertTrue(result.success)
        assertEquals(1, equippedId)
    }

    @Test fun maximize_noImprovement_returnsFalse() = runBlocking {
        val db = StubDb().also {
            it.syncTestItemModifiers("myst hat", "plain hat")
        }
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
        val mgr = MaximizerManager(db, inv, equip, character)
        assertFalse(mgr.maximize("mysticality").success)
    }

    @Test fun maximize_rejectsFailedConstraintGoal() = runBlocking {
        val db = StubDb().also {
            it.syncTestItemModifiers("myst hat", "plain hat")
        }
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.HAT, "myst hat")
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
        val equip = EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        )
        val mgr = MaximizerManager(db, inv, equip, character)
        assertFalse(mgr.maximize("500 min, mysticality").success)
    }

    @Test fun maximize_retrievesFromCloset_whenBestNotInInventory() = runBlocking {
        val db = StubDb().also {
            it.syncTestItemModifiers("myst hat", "plain hat")
        }
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
        val mgr = MaximizerManager(db, inv, equip, character, closet)
        assertTrue(mgr.maximize("mysticality").success)
        assertEquals(1, equippedId)
        assertTrue(fetchCount >= 1)
    }

    @Test fun maximize_retrievesFromDisplayCase_whenBestNotInInventory() = runBlocking {
        val db = StubDb().also {
            it.syncTestItemModifiers("myst hat", "plain hat")
        }
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
        val mgr = MaximizerManager(db, inv, equip, character, displayCaseRequest = display)
        assertTrue(mgr.maximize("mysticality").success)
        assertEquals(1, equippedId)
    }

    @Test fun maximize_retrievesFromClanStash_whenBestNotInInventory() = runBlocking {
        val db = StubDb().also {
            it.syncTestItemModifiers("myst hat", "plain hat")
        }
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
        val mgr = MaximizerManager(db, inv, equip, character, clanStashRequest = stash)
        assertTrue(mgr.maximize("mysticality").success)
        assertEquals(1, equippedId)
    }

    @Test fun maximize_switchFamiliar_beforeEquip() = runBlocking {
        val db = StubDb().also {
            it.syncTestItemModifiers("myst hat")
        }
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
        val mgr = MaximizerManager(db, inv, equip, character, familiarManager = familiar)
        val result = mgr.maximize("mysticality, switch Miniature Donkey")
        assertTrue(result.success)
        assertEquals("Miniature Donkey", result.familiarSwitched)
        assertEquals("Miniature Donkey", switched)
    }

    @Test fun maximize_switchFamiliar_picksFirstOwnedWhenScoresTied() = runBlocking {
        val db = StubDb().also {
            it.syncTestItemModifiers("myst hat")
        }
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
                        FamiliarData(1, "He-Boulder", "He-Boulder", 5, 0, 0),
                        FamiliarData(2, "Piano", "Piano Cat", 5, 0, 0),
                    ),
                ),
            )
        }
        val equip = EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        )
        val mgr = MaximizerManager(db, inv, equip, character, familiarManager = familiar)
        val result = mgr.maximize("mysticality, switch He-Boulder, Piano Cat")
        assertTrue(result.success)
        assertEquals("He-Boulder", result.familiarSwitched)
        assertEquals("He-Boulder", switched)
    }

    @Test fun maximize_switchFamiliar_skipsUnusableBeeRaceOnBeecore() = runBlocking {
        val db = StubDb().also {
            it.syncTestItemModifiers("myst hat")
        }
        val character = KoLCharacter()
        character.updateFromApiResponse(
            CharacterApiResponse(path = "Bees Hate You", kingliberated = "0"),
        )
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
                        FamiliarData(8, "Barn", "Barrrnacle", 5, 0, 0),
                        FamiliarData(1, "Donkey", "Miniature Donkey", 5, 0, 0),
                    ),
                ),
            )
        }
        val equip = EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        )
        val mgr = MaximizerManager(db, inv, equip, character, familiarManager = familiar)
        val result = mgr.maximize("mysticality, switch Barrrnacle, switch Miniature Donkey")
        assertTrue(result.success)
        assertEquals("Miniature Donkey", result.familiarSwitched)
        assertEquals("Miniature Donkey", switched)
    }

    @Test fun maximize_enthroneSkipsUnusableBeeRaceOnBeecore() = runBlocking {
        val db = StubDb().also {
            it.syncTestItemModifiers("Crown of Thrones")
        }
        val character = KoLCharacter()
        character.updateFromApiResponse(
            CharacterApiResponse(path = "Bees Hate You", kingliberated = "0"),
        )
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
        var enthroned: String? = null
        val familiar = object : FamiliarManager(
            HttpClient(MockEngine { respond("ok") }),
            GameEventBus(),
        ) {
            override suspend fun setEnthroned(name: String): Result<Unit> {
                enthroned = name
                return Result.success(Unit)
            }
        }.also {
            it.testSetState(
                FamiliarState(
                    ownedFamiliars = listOf(
                        FamiliarData(8, "Barn", "Barrrnacle", 5, 0, 0),
                        FamiliarData(1, "Donkey", "Miniature Donkey", 5, 0, 0),
                    ),
                ),
            )
        }
        val equip = EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        )
        val mgr = MaximizerManager(db, inv, equip, character, familiarManager = familiar)
        val result = mgr.maximize("mysticality, enthrone Barrrnacle, enthrone Miniature Donkey")
        assertTrue(result.success)
        assertEquals("Miniature Donkey", result.enthronedSwitched)
        assertEquals("Miniature Donkey", enthroned)
    }

    @Test fun maximize_enthroneCarriesCrownOfThrones() = runBlocking {
        val db = StubDb().also {
            it.syncTestItemModifiers("Crown of Thrones")
        }
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
        val mgr = MaximizerManager(db, inv, equip, character, familiarManager = familiar)
        val result = mgr.maximize("mysticality, enthrone Mosquito")
        assertTrue(result.success)
        assertEquals(4614, equippedId)
        assertEquals("Mosquito", result.enthronedSwitched)
    }

    @Test fun maximize_discoversEnthroneWhenCrownRanksWithoutGoal() = runBlocking {
        val db = StubDb().also {
            it.syncTestItemModifiers("Crown of Thrones")
        }
        ModifierDatabase.injectForTest("Familiar", "Miniature Donkey", "Mysticality: +5")
        ModifierDatabase.injectForTest("Familiar", "Mosquito", "Mysticality: +1")
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
        var enthroned: String? = null
        val familiar = object : FamiliarManager(
            HttpClient(MockEngine { respond("ok") }),
            GameEventBus(),
        ) {
            override suspend fun setEnthroned(name: String): Result<Unit> {
                enthroned = name
                return Result.success(Unit)
            }
        }.also {
            it.testSetState(
                FamiliarState(
                    activeFamiliar = FamiliarData(1, "Mosquito", "Mosquito", 5, 0, 0),
                    ownedFamiliars = listOf(
                        FamiliarData(1, "Mosquito", "Mosquito", 5, 0, 0),
                        FamiliarData(2, "Donkey", "Miniature Donkey", 5, 0, 0),
                    ),
                ),
            )
        }
        val equip = EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        )
        val mgr = MaximizerManager(db, inv, equip, character, familiarManager = familiar)
        val result = mgr.maximize("mysticality")
        assertTrue(result.success)
        assertEquals("Miniature Donkey", result.enthronedSwitched)
        assertEquals("Miniature Donkey", enthroned)
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
        val db = StubDb()
        db.syncTestItemModifiers("myst hat", "plain hat")
        val mgr = MaximizerManager(
            db, inv, equip, character,
            preferences = preferences,
            skillManager = skills,
        )
        val result = mgr.maximize("item, switch thrall Spice Ghost")
        assertTrue(result.success)
        assertEquals("Spice Ghost", result.thrallSwitched)
        assertEquals(3039, castSkillId)
    }

    @Test fun maximize_accessoryCombination_equipsMultipleAccessories() = runBlocking {
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    101 to InventoryItem(101, "acc alpha", 1, ItemType.ACCESSORY),
                    102 to InventoryItem(102, "acc beta", 1, ItemType.ACCESSORY),
                    103 to InventoryItem(103, "acc gamma", 1, ItemType.ACCESSORY),
                    104 to InventoryItem(104, "acc delta", 1, ItemType.ACCESSORY),
                ))
            )
        }
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = when (id) {
                101 -> ItemData(101, "acc alpha", "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null)
                102 -> ItemData(102, "acc beta", "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null)
                103 -> ItemData(103, "acc gamma", "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null)
                104 -> ItemData(104, "acc delta", "", "", ItemPrimaryUse.ACCESSORY, emptySet(), setOf('t'), 0, null)
                else -> null
            }
            override fun item(name: String): ItemData? = when (name.lowercase()) {
                "acc alpha" -> item(101)
                "acc beta" -> item(102)
                "acc gamma" -> item(103)
                "acc delta" -> item(104)
                else -> null
            }
            override fun itemModifier(name: String): ModifierEntry? = when (name.lowercase()) {
                "acc alpha" -> ModifierEntry("Item", "acc alpha", "Mysticality: +5")
                "acc beta" -> ModifierEntry("Item", "acc beta", "Mysticality: +4")
                "acc gamma" -> ModifierEntry("Item", "acc gamma", "Mysticality: +3")
                "acc delta" -> ModifierEntry("Item", "acc delta", "Mysticality: +2")
                else -> null
            }
        }.also {
            it.syncTestItemModifiers("acc alpha", "acc beta", "acc gamma", "acc delta")
        }
        var equippedCount = 0
        val equip = object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
                equippedCount++
                return Result.success(Unit)
            }
        }
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        preferences.setInt(MaximizerManager.COMBINATION_LIMIT_PREF, 64)
        val mgr = MaximizerManager(db, inv, equip, character, preferences = preferences)
        val result = mgr.maximize("mysticality")
        assertTrue(result.success)
        assertTrue(equippedCount >= 3)
    }

    @Test fun maximize_weaponOffhandCombination_equipsBoth() = runBlocking {
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    201 to InventoryItem(201, "big sword", 1, ItemType.WEAPON),
                    202 to InventoryItem(202, "better sword", 1, ItemType.WEAPON),
                    301 to InventoryItem(301, "small shield", 1, ItemType.OFFHAND),
                    302 to InventoryItem(302, "big shield", 1, ItemType.OFFHAND),
                ))
            )
        }
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = when (id) {
                201 -> ItemData(201, "big sword", "", "", ItemPrimaryUse.WEAPON, emptySet(), setOf('t'), 0, null)
                202 -> ItemData(202, "better sword", "", "", ItemPrimaryUse.WEAPON, emptySet(), setOf('t'), 0, null)
                301 -> ItemData(301, "small shield", "", "", ItemPrimaryUse.OFFHAND, emptySet(), setOf('t'), 0, null)
                302 -> ItemData(302, "big shield", "", "", ItemPrimaryUse.OFFHAND, emptySet(), setOf('t'), 0, null)
                else -> null
            }
            override fun item(name: String): ItemData? = when (name.lowercase()) {
                "big sword" -> item(201)
                "better sword" -> item(202)
                "small shield" -> item(301)
                "big shield" -> item(302)
                else -> null
            }
            override fun itemModifier(name: String): ModifierEntry? = when (name.lowercase()) {
                "big sword" -> ModifierEntry("Item", "big sword", "Mysticality: +3")
                "better sword" -> ModifierEntry("Item", "better sword", "Mysticality: +5")
                "small shield" -> ModifierEntry("Item", "small shield", "Mysticality: +2")
                "big shield" -> ModifierEntry("Item", "big shield", "Mysticality: +4")
                else -> null
            }
        }.also {
            it.syncTestItemModifiers("big sword", "better sword", "small shield", "big shield")
        }
        var weaponEquipped = false
        var offhandEquipped = false
        val equip = object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
                if (slot == EquipmentSlot.WEAPON) weaponEquipped = true
                if (slot == EquipmentSlot.OFFHAND) offhandEquipped = true
                return Result.success(Unit)
            }
        }
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        preferences.setInt(MaximizerManager.COMBINATION_LIMIT_PREF, 64)
        val mgr = MaximizerManager(db, inv, equip, character, preferences = preferences)
        val result = mgr.maximize("mysticality")
        assertTrue(result.success)
        assertTrue(weaponEquipped)
        assertTrue(offhandEquipped)
    }

    @Test fun maximize_armorCombination_equipsHatShirtPants() = runBlocking {
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    401 to InventoryItem(401, "good hat", 1, ItemType.HAT),
                    402 to InventoryItem(402, "best hat", 1, ItemType.HAT),
                    501 to InventoryItem(501, "good shirt", 1, ItemType.SHIRT),
                    502 to InventoryItem(502, "best shirt", 1, ItemType.SHIRT),
                    601 to InventoryItem(601, "good pants", 1, ItemType.PANTS),
                    602 to InventoryItem(602, "best pants", 1, ItemType.PANTS),
                ))
            )
        }
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = when (id) {
                401 -> ItemData(401, "good hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
                402 -> ItemData(402, "best hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
                501 -> ItemData(501, "good shirt", "", "", ItemPrimaryUse.SHIRT, emptySet(), setOf('t'), 0, null)
                502 -> ItemData(502, "best shirt", "", "", ItemPrimaryUse.SHIRT, emptySet(), setOf('t'), 0, null)
                601 -> ItemData(601, "good pants", "", "", ItemPrimaryUse.PANTS, emptySet(), setOf('t'), 0, null)
                602 -> ItemData(602, "best pants", "", "", ItemPrimaryUse.PANTS, emptySet(), setOf('t'), 0, null)
                else -> null
            }
            override fun item(name: String): ItemData? = when (name.lowercase()) {
                "good hat" -> item(401)
                "best hat" -> item(402)
                "good shirt" -> item(501)
                "best shirt" -> item(502)
                "good pants" -> item(601)
                "best pants" -> item(602)
                else -> null
            }
            override fun itemModifier(name: String): ModifierEntry? = when (name.lowercase()) {
                "good hat" -> ModifierEntry("Item", "good hat", "Mysticality: +2")
                "best hat" -> ModifierEntry("Item", "best hat", "Mysticality: +4")
                "good shirt" -> ModifierEntry("Item", "good shirt", "Mysticality: +3")
                "best shirt" -> ModifierEntry("Item", "best shirt", "Mysticality: +5")
                "good pants" -> ModifierEntry("Item", "good pants", "Mysticality: +2")
                "best pants" -> ModifierEntry("Item", "best pants", "Mysticality: +4")
                else -> null
            }
        }.also {
            it.syncTestItemModifiers(
                "good hat", "best hat", "good shirt", "best shirt", "good pants", "best pants",
            )
        }
        var hatEquipped = false
        var shirtEquipped = false
        var pantsEquipped = false
        val equip = object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
                when (slot) {
                    EquipmentSlot.HAT -> hatEquipped = true
                    EquipmentSlot.SHIRT -> shirtEquipped = true
                    EquipmentSlot.PANTS -> pantsEquipped = true
                    else -> Unit
                }
                return Result.success(Unit)
            }
        }
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        preferences.setInt(MaximizerManager.COMBINATION_LIMIT_PREF, 64)
        val mgr = MaximizerManager(db, inv, equip, character, preferences = preferences)
        val result = mgr.maximize("mysticality")
        assertTrue(result.success)
        assertTrue(hatEquipped)
        assertTrue(shirtEquipped)
        assertTrue(pantsEquipped)
    }

    private class BeeosityStubDb : GameDatabase() {
        override fun item(id: Int): ItemData? = when (id) {
            1 -> ItemData(1, "plain hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
            2 -> ItemData(2, "babbling book", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
            else -> null
        }
        override fun item(name: String): ItemData? = when (name.lowercase()) {
            "plain hat" -> item(1)
            "babbling book" -> item(2)
            else -> null
        }
        override fun itemModifier(name: String): ModifierEntry? = when (name.lowercase()) {
            "plain hat" -> ModifierEntry("Item", "plain hat", "Mysticality: +1")
            "babbling book" -> ModifierEntry("Item", "babbling book", "Mysticality: +10")
            else -> null
        }
    }

    @Test fun maximize_beecore_skipsHighBeeosityItem() = runBlocking {
        val db = BeeosityStubDb().also {
            it.syncTestItemModifiers("plain hat", "babbling book")
        }
        val character = KoLCharacter()
        character.updateFromApiResponse(
            CharacterApiResponse(path = "Bees Hate You", kingliberated = "0"),
        )
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    1 to InventoryItem(1, "plain hat", 1, ItemType.HAT),
                    2 to InventoryItem(2, "babbling book", 1, ItemType.HAT),
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
        val mgr = MaximizerManager(db, inv, equip, character)
        val result = mgr.maximize("mysticality, beeosity")
        assertTrue(result.success)
        assertEquals(1, equippedId)
    }

    @Test fun maximize_multiWeightGoal_prefersItemDropHat() = runBlocking {
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.HAT, "plain hat")
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    10 to InventoryItem(10, "item drop hat", 1, ItemType.HAT),
                    11 to InventoryItem(11, "meat drop hat", 1, ItemType.HAT),
                    2 to InventoryItem(2, "plain hat", 1, ItemType.HAT),
                ))
            )
        }
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = when (id) {
                10 -> ItemData(10, "item drop hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
                11 -> ItemData(11, "meat drop hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
                2 -> ItemData(2, "plain hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
                else -> null
            }
            override fun item(name: String): ItemData? = when (name.lowercase()) {
                "item drop hat" -> item(10)
                "meat drop hat" -> item(11)
                "plain hat" -> item(2)
                else -> null
            }
            override fun itemModifier(name: String): ModifierEntry? = when (name.lowercase()) {
                "item drop hat" -> ModifierEntry("Item", "item drop hat", "Item Drop: +20")
                "meat drop hat" -> ModifierEntry("Item", "meat drop hat", "Meat Drop: +20")
                "plain hat" -> ModifierEntry("Item", "plain hat", "Mysticality: +1")
                else -> null
            }
        }.also {
            it.syncTestItemModifiers("item drop hat", "meat drop hat", "plain hat")
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
        val mgr = MaximizerManager(db, inv, equip, character)
        val result = mgr.maximize("2 item, 1 meat")
        assertTrue(result.success)
        assertEquals(10, equippedId)
    }

    @Test fun maximize_handsGoal_prefersDualWieldOneHanders() = runBlocking {
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    702 to InventoryItem(702, "sharp knife", 1, ItemType.WEAPON),
                    703 to InventoryItem(703, "rusty dagger", 1, ItemType.WEAPON),
                ))
            )
        }
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = when (id) {
                702 -> ItemData(702, "sharp knife", "", "", ItemPrimaryUse.WEAPON, emptySet(), setOf('t'), 0, null)
                703 -> ItemData(703, "rusty dagger", "", "", ItemPrimaryUse.WEAPON, emptySet(), setOf('t'), 0, null)
                else -> null
            }
            override fun item(name: String): ItemData? = when (name.lowercase()) {
                "sharp knife" -> item(702)
                "rusty dagger" -> item(703)
                else -> null
            }
            override fun itemModifier(name: String): ModifierEntry? = when (name.lowercase()) {
                "sharp knife" -> ModifierEntry("Item", "sharp knife", "Mysticality: +6")
                "rusty dagger" -> ModifierEntry("Item", "rusty dagger", "Mysticality: +5")
                else -> null
            }
        }.also {
            net.sourceforge.kolmafia.data.EquipmentDatabase.registerForTest(
                702, net.sourceforge.kolmafia.data.EquipmentData("sharp knife", 100, null, 1, "knife"),
            )
            net.sourceforge.kolmafia.data.EquipmentDatabase.registerForTest(
                703, net.sourceforge.kolmafia.data.EquipmentData("rusty dagger", 100, null, 1, "knife"),
            )
            it.syncTestItemModifiers("sharp knife", "rusty dagger")
        }
        var weaponId: Int? = null
        var offhandId: Int? = null
        val equip = object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
                when (slot) {
                    EquipmentSlot.WEAPON -> weaponId = itemId
                    EquipmentSlot.OFFHAND -> offhandId = itemId
                    else -> Unit
                }
                return Result.success(Unit)
            }
        }
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        preferences.setInt(MaximizerManager.COMBINATION_LIMIT_PREF, 64)
        val mgr = MaximizerManager(db, inv, equip, character, preferences = preferences)
        val result = mgr.maximize("mysticality, +hands")
        assertTrue(result.success)
        assertEquals(702, weaponId)
        assertEquals(703, offhandId)
    }

    @Test fun maximize_creatableGoal_ranksItemWithZeroPhysicalCopies() = runBlocking {
        net.sourceforge.kolmafia.data.ItemDatabase.registerForTest(
            ItemData(801, "craft hat", "d801", "img", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null),
        )
        net.sourceforge.kolmafia.data.ItemDatabase.registerForTest(
            ItemData(802, "paste", "d802", "img", ItemPrimaryUse.USABLE, emptySet(), setOf('t'), 0, null),
        )
        net.sourceforge.kolmafia.data.ConcoctionDatabase.injectForTest(
            net.sourceforge.kolmafia.data.ConcoctionData(
                result = "craft hat",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(net.sourceforge.kolmafia.data.ConcoctionIngredient("paste", 1)),
            ),
        )
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    802 to InventoryItem(802, "paste", 3, ItemType.USABLE),
                ))
            )
        }
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = net.sourceforge.kolmafia.data.ItemDatabase.getById(id)
            override fun item(name: String): ItemData? = net.sourceforge.kolmafia.data.ItemDatabase.getByName(name)
            override fun itemModifier(name: String): ModifierEntry? = when (name.lowercase()) {
                "craft hat" -> ModifierEntry("Item", "craft hat", "Mysticality: +12")
                else -> null
            }
        }.also { it.syncTestItemModifiers("craft hat") }
        val equip = object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> =
                Result.success(Unit)
        }
        val mgr = MaximizerManager(db, inv, equip, character)
        val speculateLines = mgr.speculate("mysticality, creatable")
        assertFalse(
            speculateLines.any { it.contains("No improvement", ignoreCase = true) },
            speculateLines.joinToString("\n"),
        )
        assertTrue(
            speculateLines.any { it.contains("craft hat", ignoreCase = true) },
            speculateLines.joinToString("\n"),
        )
    }

    @Test
    fun buildCandidateIds_includesUnownedEquipmentWhenSpecPresent() {
        ItemDatabase.registerForTest(
            ItemData(
                9001, "scanned hat", "d9001", "img", ItemPrimaryUse.HAT,
                emptySet(), setOf('t'), 0, null,
            ),
        )
        EquipmentDatabase.registerForTest(
            9001,
            EquipmentData("scanned hat", 100, null, 0, "hat"),
        )
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = ItemDatabase.getById(id)
            override fun item(name: String): ItemData? = ItemDatabase.getByName(name)
        }
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(InventoryState())
        }
        val equip = object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> =
                Result.success(Unit)
        }
        val mgr = MaximizerManager(db, inv, equip, character)
        val withoutSpec = mgr.buildCandidateIds(
            inv.state.value,
            emptyMap(), emptyMap(), emptyMap(), emptyMap(),
            spec = null,
        )
        val withSpec = mgr.buildCandidateIds(
            inv.state.value,
            emptyMap(), emptyMap(), emptyMap(), emptyMap(),
            spec = MaximizeSpec(
                primary = net.sourceforge.kolmafia.modifiers.DoubleModifier.MYS,
                evaluator = Evaluator("mysticality"),
            ),
        )
        assertFalse(9001 in withoutSpec)
        assertTrue(9001 in withSpec)
    }

    @Test
    fun maximize_itemGoal_prefersUmbrellaBucketStyle() = runBlocking {
        runBlocking { ModifierDatabase.load() }
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    10899 to InventoryItem(10899, "unbreakable umbrella", 1, ItemType.OFFHAND),
                    2 to InventoryItem(2, "plain hat", 1, ItemType.HAT),
                )),
            )
        }
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = when (id) {
                10899 -> ItemData(
                    10899, "unbreakable umbrella", "", "", ItemPrimaryUse.OFFHAND,
                    emptySet(), setOf('t'), 0, null,
                )
                2 -> ItemData(2, "plain hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
                else -> null
            }
            override fun item(name: String): ItemData? = when (name.lowercase()) {
                "unbreakable umbrella" -> item(10899)
                "plain hat" -> item(2)
                else -> null
            }
            override fun itemModifier(name: String): ModifierEntry? = when (name.lowercase()) {
                "plain hat" -> ModifierEntry("Item", "plain hat", "Mysticality: +1")
                else -> null
            }
        }.also {
            EquipmentDatabase.registerForTest(
                10899, EquipmentData("unbreakable umbrella", 100, null, 0, "umbrella"),
            )
            it.syncTestItemModifiers("plain hat")
        }
        var offhandId: Int? = null
        val equip = object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
                if (slot == EquipmentSlot.OFFHAND) offhandId = itemId
                return Result.success(Unit)
            }
        }
        val mgr = MaximizerManager(db, inv, equip, character)
        val speculateLines = mgr.speculate("item")
        assertTrue(
            speculateLines.any { it.contains("unbreakable umbrella", ignoreCase = true) },
            speculateLines.toString(),
        )
        val result = mgr.maximize("item")
        assertTrue(result.success)
        assertEquals(10899, offhandId)
    }

    @Test
    fun maximize_switchesUmbrellaModeWhenPrefDiffers() = runBlocking {
        runBlocking { ModifierDatabase.load() }
        val preferences = Preferences(MapSettings()).apply {
            setString("umbrellaState", "broken")
        }
        val character = KoLCharacter()
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("ok") }),
            eventBus = GameEventBus(),
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    10899 to InventoryItem(10899, "unbreakable umbrella", 1, ItemType.OFFHAND),
                )),
            )
        }
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = when (id) {
                10899 -> ItemData(
                    10899, "unbreakable umbrella", "", "", ItemPrimaryUse.OFFHAND,
                    emptySet(), setOf('t'), 0, null,
                )
                else -> null
            }
            override fun item(name: String): ItemData? = when (name.lowercase()) {
                "unbreakable umbrella" -> item(10899)
                else -> null
            }
        }.also {
            EquipmentDatabase.registerForTest(
                10899, EquipmentData("unbreakable umbrella", 100, null, 0, "umbrella"),
            )
        }
        var equipCount = 0
        val equip = object : EquipmentRequest(
            HttpClient(MockEngine { respond("ok") }),
            character = character,
        ) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> {
                if (itemId == 10899 && slot == EquipmentSlot.OFFHAND) equipCount++
                return Result.success(Unit)
            }
        }
        val modeCalls = mutableListOf<Pair<Modeable, String>>()
        val modeRequest = object : ModeableRequest(
            client = HttpClient(MockEngine { respond("ok") }),
            choiceRequest = ChoiceRequest(HttpClient(MockEngine { respond("ok") })),
            preferences = preferences,
        ) {
            override suspend fun setMode(modeable: Modeable, mode: String): Result<Unit> {
                modeCalls += modeable to mode
                preferences.setString("umbrellaState", mode)
                return Result.success(Unit)
            }
        }
        val mgr = MaximizerManager(
            db, inv, equip, character,
            preferences = preferences,
            modeableRequest = modeRequest,
        )
        val speculateLines = mgr.speculate("item")
        assertTrue(speculateLines.any { it.contains("umbrella bucket style", ignoreCase = true) })
        val result = mgr.maximize("item")
        assertTrue(result.success)
        assertEquals("bucket style", result.modeSwitched[Modeable.UMBRELLA])
        assertEquals(listOf(Modeable.UMBRELLA to "bucket style"), modeCalls)
        assertTrue(equipCount >= 2, "expected initial equip + mustEquipAfterChange re-equip")
    }
}
