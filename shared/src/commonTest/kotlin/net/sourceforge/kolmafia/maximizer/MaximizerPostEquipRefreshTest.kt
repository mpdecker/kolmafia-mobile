package net.sourceforge.kolmafia.maximizer

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectData
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.EquipmentData
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaximizerPostEquipRefreshTest {

    @AfterTest
    fun cleanupFixtures() {
        EffectDatabase.resetForTest()
        SkillDefinitionDatabase.resetForTest()
        ModifierDatabase.resetForTest()
        UneffectSkillEffectMap.rebuild()
    }

    @Test
    fun refresh_callsStatusSyncAndEffectFetch() = runBlocking {
        var syncCalled = false
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }),
            eventBus = GameEventBus(),
        ) {
            override suspend fun refreshCharacterStatus(effectManager: EffectManager?): Boolean {
                syncCalled = true
                return true
            }
        }
        var effectFetchCount = 0
        val client = HttpClient(MockEngine { request ->
            if (request.url.parameters["what"] == "effects") {
                effectFetchCount++
            }
            respond("{}", HttpStatusCode.OK)
        })
        val effects = EffectManager(client, GameEventBus())
        MaximizerPostEquipRefresh.refresh(inv, effects)
        assertTrue(syncCalled)
    }

    @Test
    fun refresh_statusOnlyWhenEffectManagerNull() = runBlocking {
        var syncCalled = false
        val inv = object : InventoryManager(
            client = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }),
            eventBus = GameEventBus(),
        ) {
            override suspend fun refreshCharacterStatus(effectManager: EffectManager?): Boolean {
                syncCalled = effectManager == null
                return true
            }
        }
        MaximizerPostEquipRefresh.refresh(inv, null)
        assertTrue(syncCalled)
    }

    @Test
    fun refresh_noobcoreUsesCharpaneNotApiStatus() = runBlocking {
        var apiStatusCalls = 0
        var charpaneCalls = 0
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.parameters["what"] == "status" -> {
                    apiStatusCalls++
                    respond("{}", HttpStatusCode.OK)
                }
                request.url.encodedPath.endsWith("charpane.php") -> {
                    charpaneCalls++
                    respond(
                        """
                        <br>Lvl. 5
                        >Mus</td><td><b>50</b></td>>Mys</td><td><b>40</b></td>>Mox</td><td><b>30</b></td>
                        HP: <b>75/100</b>
                        MP: <b>40/50</b>
                        """.trimIndent(),
                        HttpStatusCode.OK,
                    )
                }
                else -> respond("{}", HttpStatusCode.OK)
            }
        })
        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(
                path = AscensionPath.GELATINOUS_NOOB.apiName,
            ),
        )
        val inv = InventoryManager(
            client = client,
            eventBus = GameEventBus(),
            characterRequest = CharacterRequest(client),
            character = char,
        )
        val ok = inv.refreshCharacterStatus(null)
        assertTrue(ok)
        assertEquals(0, apiStatusCalls)
        assertEquals(1, charpaneCalls)
        assertEquals(50, char.state.value.buffedMusc)
    }

    @Test
    fun liveRescore_preSearchAndPostEquipBothRefresh() = runBlocking {
        runBlocking { ModifierDatabase.load() }
        EffectDatabase.registerForTest(
            EffectData(
                id = 96003,
                name = "Dual Refresh Cast",
                image = "cast.gif",
                descId = "d96003",
                quality = EffectQuality.GOOD,
                attributes = emptySet(),
                actions = "cast 1 Dual Refresh Cast",
            ),
        )
        ModifierDatabase.injectForTest("Effect", "Dual Refresh Cast", "Mysticality: +100")
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = 96003,
                name = "Dual Refresh Cast",
                image = "cast.gif",
                tags = setOf("nc", "effect"),
                mpCost = 10,
                duration = 5,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = false,
            ),
        )
        UneffectSkillEffectMap.rebuild()
        val character = KoLCharacter()
        character.updateFromApiResponse(
            CharacterApiResponse(
                hat = "plain hat",
                level = "15",
            ),
        )
        var refreshCount = 0
        val client = HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) })
        val inv = object : InventoryManager(
            client = client,
            eventBus = GameEventBus(),
            characterRequest = CharacterRequest(client),
            character = character,
        ) {
            override val state = MutableStateFlow(
                InventoryState(items = mapOf(
                    1 to InventoryItem(1, "plain hat", 1, ItemType.HAT),
                    2 to InventoryItem(2, "myst hat", 1, ItemType.HAT),
                )),
            )
            override suspend fun refreshCharacterStatus(effectManager: EffectManager?): Boolean {
                refreshCount++
                return super.refreshCharacterStatus(effectManager)
            }
        }
        val db = object : GameDatabase() {
            override fun item(id: Int): ItemData? = when (id) {
                1 -> ItemData(1, "plain hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
                2 -> ItemData(2, "myst hat", "", "", ItemPrimaryUse.HAT, emptySet(), setOf('t'), 0, null)
                else -> null
            }
            override fun item(name: String): ItemData? = when (name.lowercase()) {
                "plain hat" -> item(1)
                "myst hat" -> item(2)
                else -> null
            }
            override fun itemModifier(name: String): ModifierEntry? = when (name.lowercase()) {
                "plain hat" -> ModifierEntry("Item", "plain hat", "Mysticality: +1")
                "myst hat" -> ModifierEntry("Item", "myst hat", "Mysticality: +5")
                else -> null
            }
        }.also {
            EquipmentDatabase.registerForTest(1, EquipmentData("plain hat", 100, null, 0, "hat"))
            EquipmentDatabase.registerForTest(2, EquipmentData("myst hat", 100, null, 0, "hat"))
            ModifierDatabase.injectForTest("Item", "plain hat", "Mysticality: +1")
            ModifierDatabase.injectForTest("Item", "myst hat", "Mysticality: +5")
        }
        val equip = object : EquipmentRequest(client, character = character) {
            override suspend fun equipItem(itemId: Int, slot: EquipmentSlot): Result<Unit> =
                Result.success(Unit)
        }
        val skills = SkillManager(client, SkillCastRequest(client), GameEventBus())
        skills.learnLocalSkill(
            SkillData(
                id = 96003,
                name = "Dual Refresh Cast",
                type = SkillType.NONCOMBAT,
                mpCost = 10,
                dailyLimit = 0,
                timesCast = 0,
            ),
        )
        val prefs = Preferences(MapSettings())
        prefs.setInt(MaximizerManager.COMBINATION_LIMIT_PREF, 64)
        val mgr = MaximizerManager(
            db,
            inv,
            equip,
            character,
            preferences = prefs,
            skillManager = skills,
            effectManager = EffectManager(client, GameEventBus()),
        )
        mgr.cliExecutor = { true }
        val result = mgr.maximize(
            "5 max, mysticality",
            filters = setOf(MaximizerFilterType.EQUIP, MaximizerFilterType.CAST),
        )
        assertTrue(result.success, result.toString())
        assertTrue(refreshCount >= 2, "pre-search + post-equip refresh expected, got $refreshCount")
    }
}
