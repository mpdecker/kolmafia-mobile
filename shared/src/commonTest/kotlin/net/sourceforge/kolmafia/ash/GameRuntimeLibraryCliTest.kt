package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.maximizer.MaximizerManager
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameRuntimeLibraryCliTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Creates a fake SkillManager with [skills] in its state and records every
     * [cast] call into [castCalls].
     */
    private fun fakeSkillManager(
        skills: List<SkillData>,
        castCalls: MutableList<Pair<String, Int>>
    ): SkillManager {
        // Encode skills as the KoL api.php?what=skills JSON: {"id": {...}, ...}
        val json = "{" + skills.joinToString(",") { s ->
            """"${s.id}":{"name":"${s.name}","type":5,"dailylimit":${s.dailyLimit},"timescast":${s.timesCast},"mpcost":${s.mpCost}}"""
        } + "}"

        val engine = MockEngine {
            respond(
                content = json,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val eventBus = GameEventBus()

        return object : SkillManager(client, SkillCastRequest(client), eventBus) {
            override suspend fun cast(skill: SkillData, quantity: Int): Result<Unit> {
                castCalls.add(skill.name to quantity)
                return Result.success(Unit)
            }
        }.also { mgr ->
            runBlocking { mgr.fetchSkills() }
        }
    }

    /**
     * Creates a fake FamiliarManager that records every [setFamiliar] call into
     * [switchCalls] without touching the network.
     */
    private fun fakeFamiliarManager(
        switchCalls: MutableList<String>,
        enthroneCalls: MutableList<String>? = null,
        bjornifyCalls: MutableList<String>? = null,
    ): FamiliarManager {
        val client = HttpClient(MockEngine { respond("") })
        val eventBus = GameEventBus()
        return object : FamiliarManager(client, eventBus) {
            override suspend fun setFamiliar(name: String): Result<Unit> {
                switchCalls.add(name)
                return Result.success(Unit)
            }
            override suspend fun setEnthroned(name: String): Result<Unit> {
                enthroneCalls?.add(name)
                return Result.success(Unit)
            }
            override suspend fun setBjornified(name: String): Result<Unit> {
                bjornifyCalls?.add(name)
                return Result.success(Unit)
            }
        }
    }

    // ── existing CLI tests ───────────────────────────────────────────────────

    @Test
    fun cliExecute_setCommand_writesPreference() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        runLib(lib, """cli_execute("set myPref=hello");""")
        assertEquals("hello", p.getString("myPref", ""))
    }

    @Test
    fun cliExecute_getCommand_printsPrefValue() {
        val p = prefs()
        p.setString("myKey", "someValue")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("get myKey");""")
        assertEquals("someValue", out)
    }

    @Test
    fun cliExecute_unknownCommand_echoesWithCliPrefix() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("doSomethingWeird");""")
        assertEquals("[cli] doSomethingWeird", out)
    }

    @Test
    fun cliExecute_alwaysReturnsTrue() {
        val lib = GameRuntimeLibrary()
        // cli_execute returns boolean true; print captures it alongside the echo fallback
        val out = outputLib(lib, """boolean result = cli_execute("anything"); print(result);""")
        assertTrue(out.contains("true"), "Expected output to contain 'true', got: $out")
    }

    // ── cast dispatch tests ──────────────────────────────────────────────────

    @Test
    fun cliExecute_castWithCount_callsSkillManager() {
        val castCalls = mutableListOf<Pair<String, Int>>()
        val fakeSkillMgr = fakeSkillManager(
            skills = listOf(
                SkillData(id = 6003, name = "Leash of Linguini", type = SkillType.BUFF,
                    mpCost = 1, dailyLimit = 0, timesCast = 0)
            ),
            castCalls = castCalls
        )
        val lib = GameRuntimeLibrary(skillManager = fakeSkillMgr)
        runLib(lib, """cli_execute("cast 3 Leash of Linguini");""")
        assertEquals(listOf("Leash of Linguini" to 3), castCalls)
    }

    @Test
    fun cliExecute_castSingleNoCount_callsSkillManagerOnce() {
        val castCalls = mutableListOf<Pair<String, Int>>()
        val fakeSkillMgr = fakeSkillManager(
            skills = listOf(
                SkillData(id = 6003, name = "Leash of Linguini", type = SkillType.BUFF,
                    mpCost = 1, dailyLimit = 0, timesCast = 0)
            ),
            castCalls = castCalls
        )
        val lib = GameRuntimeLibrary(skillManager = fakeSkillMgr)
        runLib(lib, """cli_execute("cast Leash of Linguini");""")
        assertEquals(listOf("Leash of Linguini" to 1), castCalls)
    }

    @Test
    fun cliExecute_castUnknownSkill_echoesCommand() {
        // No skillManager provided; "cast skill-name" form with unknown skill echoes
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("cast Nonexistent Skill");""")
        assertEquals("[cli] cast Nonexistent Skill", out)
    }

    @Test
    fun cliExecute_castWithCountUnknownSkill_silentNoOp() {
        // "cast N skill" with no manager — silent no-op, no echo
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("cast 1 Nonexistent Skill");""")
        assertEquals("", out)
    }

    // ── familiar dispatch test ───────────────────────────────────────────────

    @Test
    fun cliExecute_familiar_callsFamiliarManager() {
        val switchCalls = mutableListOf<String>()
        val fakeFamiliarMgr = fakeFamiliarManager(switchCalls)
        val lib = GameRuntimeLibrary(familiarManager = fakeFamiliarMgr)
        runLib(lib, """cli_execute("familiar Mosquito");""")
        assertEquals(listOf("Mosquito"), switchCalls)
    }

    @Test
    fun cliExecute_enthrone_callsSetEnthroned() {
        val enthroneCalls = mutableListOf<String>()
        val lib = GameRuntimeLibrary(
            familiarManager = fakeFamiliarManager(mutableListOf(), enthroneCalls)
        )
        runLib(lib, """cli_execute("enthrone Mosquito");""")
        assertEquals(listOf("Mosquito"), enthroneCalls)
    }

    @Test
    fun cliExecute_bjornify_callsSetBjornified() {
        val bjornifyCalls = mutableListOf<String>()
        val lib = GameRuntimeLibrary(
            familiarManager = fakeFamiliarManager(mutableListOf(), bjornifyCalls = bjornifyCalls)
        )
        runLib(lib, """cli_execute("bjornify none");""")
        assertEquals(listOf("none"), bjornifyCalls)
    }

    @Test
    fun cliExecute_eat_callsEatFoodRequest() {
        val engine = MockEngine { respond("ok", HttpStatusCode.OK) }
        val client = HttpClient(engine)
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 5, name = "salmon", descId = "", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null
            )
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            eatFoodRequest = net.sourceforge.kolmafia.request.EatFoodRequest(client)
        )
        runLib(lib, """cli_execute("eat 2 salmon");""")
        assertTrue(engine.requestHistory.isNotEmpty())
    }

    @Test
    fun cliExecute_goalAdd_registersItemGoal() {
        val gm = net.sourceforge.kolmafia.session.GoalManager()
        val lib = GameRuntimeLibrary(goalManager = gm)
        runLib(lib, """cli_execute("goal add seal tooth");""")
        assertTrue(gm.hasItemGoalByName("seal tooth"))
    }

    @Test
    fun cliExecute_goalClear_clearsGoals() {
        val gm = net.sourceforge.kolmafia.session.GoalManager()
        gm.addItemGoalByName("seal tooth")
        val lib = GameRuntimeLibrary(goalManager = gm)
        runLib(lib, """cli_execute("goal clear");""")
        assertEquals(false, gm.hasItemGoals())
    }

    @Test
    fun cliExecute_goalMeat_setsMeatGoal() {
        val gm = net.sourceforge.kolmafia.session.GoalManager()
        val lib = GameRuntimeLibrary(goalManager = gm)
        runLib(lib, """cli_execute("goal meat 5000");""")
        assertTrue(gm.hasMeatGoalSet())
    }

    @Test
    fun cliExecute_goalLevel_setsLevelGoal() {
        val gm = net.sourceforge.kolmafia.session.GoalManager()
        val lib = GameRuntimeLibrary(goalManager = gm)
        runLib(lib, """cli_execute("goal level 13");""")
        assertTrue(gm.hasLevelGoalSet())
    }

    @Test
    fun cliExecute_location_printsLastLocation() {
        val prefs = com.russhwolf.settings.MapSettings()
        prefs.putString(net.sourceforge.kolmafia.preferences.Preferences.LAST_LOCATION, "The Spooky Forest")
        val lib = GameRuntimeLibrary(preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs))
        val out = outputLib(lib, """cli_execute("location");""")
        assertTrue(out.contains("The Spooky Forest"))
    }

    @Test
    fun cliExecute_emptyCloset_callsClosetRequest() {
        var called = false
        val closet = object : net.sourceforge.kolmafia.request.ClosetRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun emptyCloset(): Result<Int> {
                called = true
                return Result.success(0)
            }
        }
        val lib = GameRuntimeLibrary(closetRequest = closet)
        runLib(lib, """cli_execute("empty closet");""")
        assertTrue(called)
    }

    @Test
    fun cliExecute_overdrink_callsDrinkRequest() {
        val engine = MockEngine { respond("ok", HttpStatusCode.OK) }
        val client = HttpClient(engine)
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 9, name = "martini", descId = "", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null
            )
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            drinkBoozeRequest = net.sourceforge.kolmafia.request.DrinkBoozeRequest(client)
        )
        runLib(lib, """cli_execute("overdrink 1 martini");""")
        assertTrue(engine.requestHistory.isNotEmpty())
    }

    @Test
    fun cliExecute_echo_printsText() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("echo hello world");""")
        assertEquals("hello world", out)
    }

    @Test
    fun cliExecute_status_printsCharacterSummary() {
        val char = net.sourceforge.kolmafia.character.KoLCharacter()
        char.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(
                name = "TestPlayer",
                level = "5",
                adventures = "12",
                meat = "999",
                hp = "30",
                hpmax = "50",
                mp = "10",
                mpmax = "20",
            )
        )
        val lib = GameRuntimeLibrary(character = char)
        val out = outputLib(lib, """cli_execute("status");""")
        assertTrue(out.contains("TestPlayer"))
        assertTrue(out.contains("Level 5"))
        assertTrue(out.contains("12 adventures"))
    }

    @Test
    fun cliExecute_stop_doesNotThrowWhenManagerPresent() {
        val client = HttpClient(MockEngine { respond("") })
        val mgr = net.sourceforge.kolmafia.adventure.AdventureManager(
            adventureRequest = net.sourceforge.kolmafia.adventure.AdventureRequest(client),
            fightRequest = net.sourceforge.kolmafia.adventure.FightRequest(client),
            choiceRequest = net.sourceforge.kolmafia.adventure.ChoiceRequest(client),
            characterRequest = net.sourceforge.kolmafia.request.CharacterRequest(client),
            character = net.sourceforge.kolmafia.character.KoLCharacter(),
            preferences = prefs(),
            eventBus = net.sourceforge.kolmafia.event.GameEventBus(),
        )
        val lib = GameRuntimeLibrary(adventureManager = mgr)
        runLib(lib, """cli_execute("abort");""")
    }

    @Test
    fun cliExecute_wiki_printsWikiUrl() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("wiki seal tooth");""")
        assertEquals("https://wiki.a.kolmafia.us/wiki/seal_tooth", out)
    }

    @Test
    fun cliExecute_configGetSet_aliasesGetSet() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        runLib(lib, """cli_execute("config set myKey value123");""")
        val out = outputLib(lib, """cli_execute("config get myKey");""")
        assertEquals("value123", out)
    }

    @Test
    fun cliExecute_goalAddId_registersItemGoalById() {
        val gm = net.sourceforge.kolmafia.session.GoalManager()
        val lib = GameRuntimeLibrary(goalManager = gm)
        runLib(lib, """cli_execute("goal add id:42");""")
        assertTrue(gm.hasItemGoal(42))
    }

    @Test
    fun cliExecute_putCloset_callsClosetPutIn() {
        var putItemId = 0
        var putQty = 0
        val closet = object : net.sourceforge.kolmafia.request.ClosetRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun putIn(itemId: Int, quantity: Int): Result<String> {
                putItemId = itemId
                putQty = quantity
                return Result.success("")
            }
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 7, name = "seal tooth", descId = "", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null
            )
        }
        val lib = GameRuntimeLibrary(closetRequest = closet, gameDatabase = db)
        runLib(lib, """cli_execute("put_closet 3 seal tooth");""")
        assertEquals(7, putItemId)
        assertEquals(3, putQty)
    }

    // ── Phase 21 CLI batch 4 + cli_execute_output ────────────────────────────

    @Test
    fun cliExecuteOutput_capturesMultiLineOutput() {
        val lib = GameRuntimeLibrary()
        runLib(lib, """
            cli_execute("echo line1");
            cli_execute("echo line2");
        """.trimIndent())
        val out = outputLib(lib, "print(cli_execute_output());")
        assertEquals("line2", out)
    }

    @Test
    fun cliExecuteOutput_emptyAfterDirectPrint() {
        val lib = GameRuntimeLibrary()
        runLib(lib, """print("direct");""")
        assertEquals("", outputLib(lib, "print(cli_execute_output());"))
    }

    @Test
    fun cliExecute_takeCloset_callsClosetTakeOut() {
        var called = false
        val closet = object : net.sourceforge.kolmafia.request.ClosetRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun takeOut(itemId: Int, quantity: Int): Result<String> {
                called = true
                return Result.success("")
            }
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 7, name = "seal tooth", descId = "", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null
            )
        }
        val lib = GameRuntimeLibrary(closetRequest = closet, gameDatabase = db)
        runLib(lib, """cli_execute("take_closet 2 seal tooth");""")
        assertTrue(called)
    }

    @Test
    fun cliExecute_dump_printsSummary() {
        val char = net.sourceforge.kolmafia.character.KoLCharacter()
        char.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(
                name = "TestPlayer", level = "5", adventures = "12", meat = "999",
                hp = "30", hpmax = "50", mp = "10", mpmax = "20",
            )
        )
        val p = prefs()
        p.setString(net.sourceforge.kolmafia.preferences.Preferences.LAST_LOCATION, "The Spooky Forest")
        val lib = GameRuntimeLibrary(character = char, preferences = p)
        val out = outputLib(lib, """cli_execute("dump");""")
        assertTrue(out.contains("TestPlayer"))
        assertTrue(out.contains("loc=The Spooky Forest"))
    }

    @Test
    fun cliExecute_batchOpenIncrementsPref() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        runLib(lib, """cli_execute("batch open"); cli_execute("batch open");""")
        assertEquals(2, p.getInt("batching", 0))
        runLib(lib, """cli_execute("batch close");""")
        assertEquals(1, p.getInt("batching", 0))
    }

    @Test
    fun cliExecute_goalFactoid_registersFactoidGoal() {
        val gm = net.sourceforge.kolmafia.session.GoalManager()
        val lib = GameRuntimeLibrary(goalManager = gm)
        runLib(lib, """cli_execute("goal factoid You found the thing");""")
        assertTrue(gm.hasFactoidGoalSet())
        assertTrue(gm.matchesFactoid("Something happened. You found the thing!"))
    }

    @Test
    fun cliExecute_uneffect_callsUneffectRequest() {
        var uneffectId = 0
        val uneffect = object : net.sourceforge.kolmafia.request.UneffectRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun uneffect(effectId: Int): Result<Unit> {
                uneffectId = effectId
                return Result.success(Unit)
            }
        }
        val effectsJson = """{"42":{"name":"Muscular","duration":10}}"""
        val client = HttpClient(MockEngine { respond(effectsJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val effectMgr = net.sourceforge.kolmafia.effect.EffectManager(client, GameEventBus())
        runBlocking { effectMgr.fetchEffects() }
        assertEquals(1, effectMgr.state.value.effects.size)
        val lib = GameRuntimeLibrary(effectManager = effectMgr, uneffectRequest = uneffect)
        runLib(lib, """cli_execute("uneffect Muscular");""")
        assertEquals(42, uneffectId)
    }

    @Test
    fun getVersion_returnsMobileVersion() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals(GameRuntimeLibrary.VERSION, outputLib(lib, "print(get_version());"))
    }

    @Test
    fun getRevision_returnsMobileRevision() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals(GameRuntimeLibrary.REVISION, outputLib(lib, "print(get_revision());"))
    }

    @Test
    fun cliExecute_goalAutostop_registersFactoidGoal() {
        val gm = net.sourceforge.kolmafia.session.GoalManager()
        val lib = GameRuntimeLibrary(goalManager = gm)
        runLib(lib, """cli_execute("goal autostop You win");""")
        assertTrue(gm.hasFactoidGoalSet())
    }

    @Test
    fun cliExecute_zone_printsZoneName() {
        val p = prefs()
        p.setString(net.sourceforge.kolmafia.preferences.Preferences.LAST_LOCATION, "The Haunted Pantry")
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("zone");""")
        // Without loaded AdventureDatabase, falls back to location name
        assertTrue(out.isNotBlank())
    }

    @Test
    fun cliExecute_count_printsInventoryQty() {
        val inv = object : net.sourceforge.kolmafia.inventory.InventoryManager(
            HttpClient(MockEngine { respond("") }),
            GameEventBus(),
        ) {
            private val _s = kotlinx.coroutines.flow.MutableStateFlow(
                net.sourceforge.kolmafia.inventory.InventoryState(
                    items = mapOf(
                        7 to net.sourceforge.kolmafia.inventory.InventoryItem(
                            7, "seal tooth", 4, net.sourceforge.kolmafia.inventory.ItemType.OTHER
                        )
                    )
                )
            )
            override val state = _s
        }
        val lib = GameRuntimeLibrary(inventoryManager = inv)
        val out = outputLib(lib, """cli_execute("count seal tooth");""")
        assertEquals("4", out)
    }

    @Test
    fun cliExecute_putStorage_callsStorageDeposit() {
        var deposited = false
        val storage = object : net.sourceforge.kolmafia.request.StorageRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun deposit(itemId: Int, quantity: Int): Result<String> {
                deposited = true
                return Result.success("")
            }
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 7, name = "seal tooth", descId = "", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null
            )
        }
        val lib = GameRuntimeLibrary(storageRequest = storage, gameDatabase = db)
        runLib(lib, """cli_execute("put_storage 2 seal tooth");""")
        assertTrue(deposited)
    }

    @Test
    fun write_routesToRuntimePrint() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("hello", outputLib(lib, """write("hello");"""))
    }

    @Test
    fun cliExecute_turns_printsAdventuresLeft() {
        val char = net.sourceforge.kolmafia.character.KoLCharacter()
        char.updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(adventures = "17")
        )
        val lib = GameRuntimeLibrary(character = char)
        assertEquals("17", outputLib(lib, """cli_execute("turns");"""))
        assertEquals("17", outputLib(lib, """cli_execute("turnsleft");"""))
    }

    @Test
    fun cliExecute_javadoc_printsWikiUrl() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("javadoc seal tooth");""")
        assertEquals("https://wiki.a.kolmafia.us/wiki/seal_tooth", out)
    }

    @Test
    fun cliExecute_homepage_visitsMainPage() {
        var visited = false
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.contains("main.php")) visited = true
            respond("", HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(httpClient = client)
        runLib(lib, """cli_execute("homepage");""")
        assertTrue(visited)
    }

    @Test
    fun cliExecute_hermit_callsHermitRequest() {
        var tradeItemId = 0
        var tradeQty = 0
        val hermit = object : net.sourceforge.kolmafia.request.HermitRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun trade(itemId: Int, quantity: Int): Result<String> {
                tradeItemId = itemId
                tradeQty = quantity
                return Result.success("")
            }
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 24, name = "smurf", descId = "", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null
            )
        }
        val lib = GameRuntimeLibrary(hermitRequest = hermit, gameDatabase = db)
        runLib(lib, """cli_execute("hermit 3 smurf");""")
        assertEquals(24, tradeItemId)
        assertEquals(3, tradeQty)
    }

    @Test
    fun cliExecute_relayOn_setsPref() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        runLib(lib, """cli_execute("relay on");""")
        assertTrue(p.getBoolean("relayActive", false))
        runLib(lib, """cli_execute("relay off");""")
        assertFalse(p.getBoolean("relayActive", true))
    }

    @Test
    fun cliExecute_questlog_syncsQuestLog() {
        var synced = false
        val questLog = object : net.sourceforge.kolmafia.request.QuestLogRequest(
            HttpClient(MockEngine { respond("") }),
            net.sourceforge.kolmafia.quest.QuestDatabase(prefs()),
        ) {
            override suspend fun syncAll() {
                synced = true
            }
        }
        val lib = GameRuntimeLibrary(questLogRequest = questLog)
        runLib(lib, """cli_execute("questlog");""")
        assertTrue(synced)
        synced = false
        runLib(lib, """cli_execute("quests");""")
        assertTrue(synced)
    }

    @Test
    fun cliExecute_description_printsItemSummary() {
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 1, name = "seal tooth", descId = "x", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 50, plural = null
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val out = outputLib(lib, """cli_execute("description seal tooth");""")
        assertEquals("seal tooth [none] autosell=50", out)
    }

    @Test
    fun cliExecute_contactsAndMail_visitPages() {
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths.add(request.url.encodedPath)
            respond("", HttpStatusCode.OK)
        })
        val lib = GameRuntimeLibrary(httpClient = client)
        runLib(lib, """cli_execute("contacts");""")
        runLib(lib, """cli_execute("mail");""")
        assertTrue(paths.any { it.contains("contacts.php") })
        assertTrue(paths.any { it.contains("mail.php") })
    }

    @Test
    fun cliExecute_inv_refreshesInventory() {
        var fetched = false
        val inv = object : net.sourceforge.kolmafia.inventory.InventoryManager(
            HttpClient(MockEngine { respond("") }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ) {
            override suspend fun fetchInventory() { fetched = true }
            override suspend fun syncCharacterEquipment() { /* no-op */ }
        }
        val lib = GameRuntimeLibrary(inventoryManager = inv)
        runLib(lib, """cli_execute("inv");""")
        assertTrue(fetched)
    }

    @Test
    fun cliExecute_pool_callsClanLounge() {
        var played = false
        val lounge = object : net.sourceforge.kolmafia.request.ClanLoungeRequest(
            HttpClient(MockEngine { respond("") })
        ) {
            override suspend fun playPoolGame(): Result<Unit> {
                played = true
                return Result.success(Unit)
            }
        }
        runLib(GameRuntimeLibrary(clanLoungeRequest = lounge), """cli_execute("pool");""")
        assertTrue(played)
    }

    @Test
    fun cliExecute_itemnotify_setsPref() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        runLib(lib, """cli_execute("itemnotify on");""")
        assertTrue(p.getBoolean("itemNotify", false))
        runLib(lib, """cli_execute("itemnotify off");""")
        assertFalse(p.getBoolean("itemNotify", true))
    }

    @Test
    fun cliExecute_steal_callsFamiliarRequest() {
        var stealItemId = 0
        val stealReq = object : net.sourceforge.kolmafia.familiar.FamiliarRequest(
            HttpClient(MockEngine { respond("ok") })
        ) {
            override suspend fun stealItem(itemId: Int) = run {
                stealItemId = itemId
                Result.success("ok")
            }
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 88, name = "knob goblin firecracker", descId = "", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null
            )
        }
        runLib(
            GameRuntimeLibrary(familiarRequest = stealReq, gameDatabase = db),
            """cli_execute("steal 2 knob goblin firecracker");"""
        )
        assertEquals(88, stealItemId)
    }

    @Test
    fun cliExecute_sendmsg_callsChatSender() {
        var channel = ""
        var message = ""
        val sender = object : net.sourceforge.kolmafia.chat.ChatSender(
            HttpClient(MockEngine { respond("") })
        ) {
            override suspend fun send(ch: String, msg: String): Result<Unit> {
                channel = ch
                message = msg
                return Result.success(Unit)
            }
        }
        runLib(GameRuntimeLibrary(chatSender = sender), """cli_execute("sendmsg clan hello there");""")
        assertEquals("clan", channel)
        assertEquals("hello there", message)
    }

    @Test
    fun cliExecute_note_savesAndPrintsUserNote() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        runLib(lib, """cli_execute("note remember this");""")
        assertEquals("remember this", p.getString(Preferences.USER_NOTE, ""))
        assertEquals("remember this", outputLib(lib, """cli_execute("note");"""))
    }

    @Test
    fun cliExecute_absorb_printsAbsorbCount() {
        val char = net.sourceforge.kolmafia.character.KoLCharacter()
        char.updateClassResource(absorbs = 3)
        val lib = GameRuntimeLibrary(character = char, httpClient = HttpClient(MockEngine { respond("") }))
        assertEquals("3", outputLib(lib, """cli_execute("absorb");"""))
    }

    @Test
    fun cliExecute_version_printsMobileVersion() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals(GameRuntimeLibrary.VERSION, outputLib(lib, """cli_execute("version");"""))
    }

    @Test
    fun cliExecute_run_executesSavedScript() {
        val p = prefs()
        p.setString(
            ScriptManager.SCRIPTS_PREF_KEY,
            Json.encodeToString(listOf(ScriptEntry("demo", """print("cli run ok");"""))),
        )
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("run demo");""")
        assertTrue(out.contains("cli run ok"))
    }

    @Test
    fun cliExecute_runscript_aliasWorks() {
        val p = prefs()
        p.setString(
            ScriptManager.SCRIPTS_PREF_KEY,
            Json.encodeToString(listOf(ScriptEntry("alias", """print("alias ok");"""))),
        )
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("runscript alias");""")
        assertTrue(out.contains("alias ok"))
    }

    @Test
    fun cliExecute_run_missingScriptPrintsError() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val out = outputLib(lib, """cli_execute("run missing");""")
        assertTrue(out.contains("Script 'missing' not found"))
    }

    @Test
    fun cliExecute_maximizer_printsUsage() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("maximizer");""")
        assertTrue(out.contains("maximize"))
    }

    @Test
    fun cliExecute_speculate_printsCandidates() {
        val character = KoLCharacter()
        val stubMaximizer = object : MaximizerManager(
            gameDatabase = GameDatabase(),
            inventoryManager = InventoryManager(
                client = HttpClient(MockEngine { respond("ok") }),
                eventBus = GameEventBus(),
            ),
            equipmentRequest = EquipmentRequest(
                HttpClient(MockEngine { respond("ok") }),
                character = character,
            ),
            character = character,
        ) {
            override suspend fun speculate(goalText: String): List<String> =
                listOf("Hat: myst hat (5.0)", "Score: 0.0 -> 5.0")
        }
        val lib = GameRuntimeLibrary(maximizerManager = stubMaximizer)
        val out = outputLib(lib, """cli_execute("speculate +itemdrop");""")
        assertTrue(out.contains("myst hat"))
        assertFalse(out.contains("[cli] speculate"))
    }

    @Test
    fun cliExecute_goalChoice_setsChoiceGoal() {
        val goals = net.sourceforge.kolmafia.session.GoalManager()
        val lib = GameRuntimeLibrary(goalManager = goals)
        runLib(lib, """cli_execute("goal choice 3");""")
        assertTrue(goals.hasChoiceGoal(3))
    }

    @Test
    fun cliExecute_goalSubstats_setsSubstatsGoal() {
        val goals = net.sourceforge.kolmafia.session.GoalManager()
        val lib = GameRuntimeLibrary(goalManager = goals)
        runLib(lib, """cli_execute("goal substats");""")
        assertTrue(goals.hasSubstatsGoal())
    }

    @Test
    fun cliExecute_setAndGet_prefRoundTrip() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        runLib(lib, """cli_execute("set myPref hello");""")
        assertEquals("hello", outputLib(lib, """cli_execute("get myPref");"""))
    }

    @Test
    fun cliExecute_counter_getAndSet() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        assertEquals("0", outputLib(lib, """cli_execute("counter kills");"""))
        runLib(lib, """cli_execute("counter kills 5");""")
        assertEquals("5", outputLib(lib, """cli_execute("counter kills");"""))
    }

    @Test
    fun cliExecute_acquire_callsRetrieve() {
        var retrieved: Pair<Int, Int>? = null
        val retrieve = object : net.sourceforge.kolmafia.item.RetrieveItemService(
            null, null, null, null, null, null, null, null, null, null, null,
        ) {
            override suspend fun retrieve(itemId: Int, qty: Int): Int {
                retrieved = itemId to qty
                return qty
            }
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 42, name = "seal tooth", descId = "", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null,
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, retrieveItemService = retrieve)
        runLib(lib, """cli_execute("acquire 3 seal tooth");""")
        assertEquals(42 to 3, retrieved)
    }

    @Test
    fun cliExecute_pull_withdrawsFromStorage() {
        var withdrawn: Pair<Int, Int>? = null
        val storage = object : net.sourceforge.kolmafia.request.StorageRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun withdraw(itemId: Int, quantity: Int): Result<String> {
                withdrawn = itemId to quantity
                return Result.success("ok")
            }
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 7, name = "meat paste", descId = "", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null,
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, storageRequest = storage)
        runLib(lib, """cli_execute("pull 2 meat paste");""")
        assertEquals(7 to 2, withdrawn)
    }

    @Test
    fun cliExecute_setLocation_setsLastLocationPref() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        runLib(lib, """cli_execute("set location The Haunted Pantry");""")
        assertEquals("The Haunted Pantry", p.getString(Preferences.LAST_LOCATION, ""))
    }

    @Test
    fun cliExecute_ccsAndMacro_storeCombatMacro() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        runLib(lib, """cli_execute("ccs attack");""")
        assertEquals("attack", outputLib(lib, """cli_execute("macro");"""))
        assertEquals("attack", outputLib(lib, """cli_execute("ccprep");"""))
        assertEquals("attack", p.getString(Preferences.COMBAT_SCRIPT, ""))
    }

    @Test
    fun cliExecute_ccs_assignsSavedCombatScriptType() {
        val p = prefs()
        val scripts = listOf(ScriptEntry("my-ccs", "set_ccs_action(\"skill 1;\");"))
        p.setString(ScriptManager.SCRIPTS_PREF_KEY, kotlinx.serialization.json.Json.encodeToString(scripts))
        val lib = GameRuntimeLibrary(preferences = p)
        runLib(lib, """cli_execute("ccs my-ccs");""")
        val updated = kotlinx.serialization.json.Json.decodeFromString<List<ScriptEntry>>(
            p.getString(ScriptManager.SCRIPTS_PREF_KEY, "[]"),
        )
        assertEquals(ScriptType.COMBAT, updated.first { it.name == "my-ccs" }.type)
    }

    @Test
    fun cliExecute_counterAdd_incrementsNamedCounter() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        runLib(lib, """cli_execute("counter kills add 3");""")
        assertEquals("3", outputLib(lib, """cli_execute("counter kills");"""))
        runLib(lib, """cli_execute("counter kills add 2");""")
        assertEquals("5", outputLib(lib, """cli_execute("counter kills");"""))
    }

    @Test
    fun cliExecute_showAll_printsCharacterSummary() {
        val char = net.sourceforge.kolmafia.character.KoLCharacter().also {
            it.updateFromApiResponse(
                net.sourceforge.kolmafia.character.CharacterApiResponse(
                    name = "Hero",
                    playerid = "42",
                    level = "10",
                    classId = "5",
                    adventures = "15",
                    meat = "5000",
                ),
            )
        }
        val lib = GameRuntimeLibrary(character = char, preferences = prefs())
        val out = outputLib(lib, """cli_execute("show all");""")
        assertTrue(out.contains("Hero"))
        assertTrue(out.contains("5000"))
    }

    @Test
    fun cliExecute_joke_printsNoOpMessage() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("That's funny.", outputLib(lib, """cli_execute("joke");"""))
    }

    @Test
    fun cliExecute_tags_listsCounterNames() {
        val p = prefs()
        p.registerCounterName("foo")
        p.setInt("counter_foo", 1)
        val lib = GameRuntimeLibrary(preferences = p)
        val out = outputLib(lib, """cli_execute("tags");""")
        assertTrue(out.contains("foo"))
    }

    @Test
    fun cliExecute_pvpAttack_doesNotEchoUnknown() {
        val lib = GameRuntimeLibrary.forTesting()
        val out = outputLib(lib, """cli_execute("pvp attack someone");""")
        assertTrue(out.contains("PvP not available"))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun cliExecute_autoscript_setsPref() {
        val p = prefs()
        val lib = GameRuntimeLibrary(preferences = p)
        runLib(lib, """cli_execute("autoscript on");""")
        assertTrue(p.getBoolean(Preferences.AUTO_SCRIPTING, false))
        runLib(lib, """cli_execute("autoscript off");""")
        assertFalse(p.getBoolean(Preferences.AUTO_SCRIPTING, true))
    }

    @Test
    fun cliExecute_sync_runsRefresh() {
        var questSynced = false
        val questLog = object : net.sourceforge.kolmafia.request.QuestLogRequest(
            HttpClient(MockEngine { respond("") }),
            net.sourceforge.kolmafia.quest.QuestDatabase(prefs()),
        ) {
            override suspend fun syncAll() {
                questSynced = true
            }
        }
        val lib = GameRuntimeLibrary(questLogRequest = questLog)
        runLib(lib, """cli_execute("sync");""")
        assertTrue(questSynced)
    }

    @Test
    fun cliExecute_quest_printsQuestStatus() {
        val db = net.sourceforge.kolmafia.quest.QuestDatabase(prefs())
        db.setProgress(net.sourceforge.kolmafia.quest.Quest.BAT, "step2")
        val lib = GameRuntimeLibrary(questDatabase = db)
        assertEquals("step2", outputLib(lib, """cli_execute("quest BAT");"""))
    }

    @Test
    fun cliExecute_questBare_syncsQuestLog() {
        var synced = false
        val questLog = object : net.sourceforge.kolmafia.request.QuestLogRequest(
            HttpClient(MockEngine { respond("") }),
            net.sourceforge.kolmafia.quest.QuestDatabase(prefs()),
        ) {
            override suspend fun syncAll() {
                synced = true
            }
        }
        val lib = GameRuntimeLibrary(questLogRequest = questLog)
        runLib(lib, """cli_execute("quest");""")
        assertTrue(synced)
    }

    @Test
    fun cliExecute_skillAlias_callsSkillManager() {
        val castCalls = mutableListOf<Pair<String, Int>>()
        val fakeSkillMgr = fakeSkillManager(
            skills = listOf(
                SkillData(id = 6003, name = "Leash of Linguini", type = SkillType.BUFF,
                    mpCost = 1, dailyLimit = 0, timesCast = 0)
            ),
            castCalls = castCalls
        )
        val lib = GameRuntimeLibrary(skillManager = fakeSkillMgr)
        runLib(lib, """cli_execute("skill 2 Leash of Linguini");""")
        assertEquals(listOf("Leash of Linguini" to 2), castCalls)
    }

    @Test
    fun cliExecute_mallbuyAlias_wrapsPurchase() {
        var buyCount = 0
        val dummyClient = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
        val mall = object : net.sourceforge.kolmafia.mall.MallManager(
            net.sourceforge.kolmafia.mall.MallSearchRequest(dummyClient),
            net.sourceforge.kolmafia.mall.MallPurchaseRequest(dummyClient),
            null,
        ) {
            override suspend fun buy(itemId: Int, count: Int, maxPrice: Int): Int {
                buyCount = count
                return count
            }
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 99, name = "test widget", descId = "", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null,
            )
        }
        val lib = GameRuntimeLibrary(
            character = net.sourceforge.kolmafia.character.KoLCharacter(),
            gameDatabase = db,
            mallManager = mall,
            equipmentRequest = net.sourceforge.kolmafia.request.EquipmentRequest(dummyClient),
        )
        runLib(lib, """cli_execute("mallbuy 3 test widget");""")
        assertEquals(3, buyCount)
    }

    @Test
    fun cliExecute_kmail_sendsViaSendMailRequest() {
        var sent: Pair<String, String>? = null
        val mail = object : net.sourceforge.kolmafia.request.SendMailRequest(HttpClient(MockEngine { respond("") })) {
            override suspend fun send(recipient: String, message: String): Result<Unit> {
                sent = recipient to message
                return Result.success(Unit)
            }
        }
        val lib = GameRuntimeLibrary(sendMailRequest = mail)
        runLib(lib, """cli_execute("kmail SomePlayer hello there");""")
        assertEquals("SomePlayer" to "hello there", sent)
    }

    @Test
    fun cliExecute_coinmasterBuyQty_callsManager() {
        net.sourceforge.kolmafia.shop.CoinmasterDatabase.resetForTest()
        var bought: Triple<String, Int, Int>? = null
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val manager = object : net.sourceforge.kolmafia.shop.CoinmasterManager(
            net.sourceforge.kolmafia.shop.CoinmasterRequest(client),
            null,
            net.sourceforge.kolmafia.data.GameDatabase(),
            client,
        ) {
            override suspend fun buy(
                master: net.sourceforge.kolmafia.shop.CoinmasterData,
                itemId: Int,
                quantity: Int,
            ): Int {
                bought = Triple(master.nickname, itemId, quantity)
                return quantity
            }
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = if (name == "dinghy plans") {
                net.sourceforge.kolmafia.data.ItemData(
                    id = 146, name = "dinghy plans", descId = "", image = "",
                    primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                    secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null,
                )
            } else null
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, coinmasterManager = manager)
        runLib(lib, """cli_execute("coinmaster buy 2 shore dinghy plans");""")
        assertEquals(Triple("shore", 146, 2), bought)
    }

    @Test
    fun cliExecute_send_withAttachment_callsSendMailRequest() {
        var sent: Triple<String, List<net.sourceforge.kolmafia.request.MailAttachment>, Long>? = null
        val mail = object : net.sourceforge.kolmafia.request.SendMailRequest(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        ) {
            override suspend fun send(
                recipient: String,
                message: String,
                attachments: List<net.sourceforge.kolmafia.request.MailAttachment>,
                meat: Long,
            ): Result<Unit> {
                sent = Triple(recipient, attachments, meat)
                return Result.success(Unit)
            }
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 5, name = "seal tooth", descId = "", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 0, plural = null,
            )
        }
        val inv = object : net.sourceforge.kolmafia.inventory.InventoryManager(
            HttpClient(MockEngine { respond("") }),
            net.sourceforge.kolmafia.event.GameEventBus(),
        ) {
            override val state = kotlinx.coroutines.flow.MutableStateFlow(
                net.sourceforge.kolmafia.inventory.InventoryState(
                    items = mapOf(
                        5 to net.sourceforge.kolmafia.inventory.InventoryItem(
                            5, "seal tooth", 3, net.sourceforge.kolmafia.inventory.ItemType.OTHER,
                        ),
                    ),
                ),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, inventoryManager = inv, sendMailRequest = mail)
        runLib(lib, """cli_execute("send 1 seal tooth to Buffy");""")
        assertEquals("Buffy", sent?.first)
        assertEquals(1, sent?.second?.size)
        assertEquals(5, sent?.second?.first()?.itemId)
    }

    @Test
    fun cliExecute_send_meatRequiresCsend() {
        val lib = GameRuntimeLibrary()
        val out = outputLib(lib, """cli_execute("send 100 meat to Buffy");""")
        assertEquals("Please use 'csend' if you need to transfer meat.", out)
    }

    @Test
    fun cliExecute_csend_meat_callsSendMailRequest() {
        val meatSent = mutableListOf<Long>()
        val mail = object : net.sourceforge.kolmafia.request.SendMailRequest(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        ) {
            override suspend fun send(
                recipient: String,
                message: String,
                attachments: List<net.sourceforge.kolmafia.request.MailAttachment>,
                meat: Long,
            ): Result<Unit> {
                meatSent.add(meat)
                return Result.success(Unit)
            }
        }
        val lib = GameRuntimeLibrary(sendMailRequest = mail)
        runLib(lib, """cli_execute("csend 500 meat to Buffy");""")
        assertEquals(listOf(500L), meatSent)
    }

    @Test
    fun cliExecute_gift_callsSendGiftRequest() {
        val gifts = mutableListOf<Pair<String, Int>>()
        val gift = object : net.sourceforge.kolmafia.request.SendGiftRequest(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        ) {
            override suspend fun send(
                recipient: String,
                message: String,
                attachments: List<net.sourceforge.kolmafia.request.MailAttachment>,
                fromStorage: Boolean,
            ): Result<Unit> {
                gifts.add(recipient to attachments.single().itemId)
                return Result.success(Unit)
            }
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 99, name = "seal tooth", descId = "x", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 1, plural = null
            )
        }
        val inv = object : net.sourceforge.kolmafia.inventory.InventoryManager(
            HttpClient(MockEngine { respond("ok") }),
            GameEventBus(),
        ) {
            override val state = kotlinx.coroutines.flow.MutableStateFlow(
                net.sourceforge.kolmafia.inventory.InventoryState(
                    items = mapOf(
                        99 to net.sourceforge.kolmafia.inventory.InventoryItem(
                            99, "seal tooth", 1, net.sourceforge.kolmafia.inventory.ItemType.OTHER,
                        ),
                    ),
                ),
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db, inventoryManager = inv, sendGiftRequest = gift)
        runLib(lib, """cli_execute("gift 1 seal tooth to Buffy");""")
        assertEquals(listOf("Buffy" to 99), gifts)
    }

    @Test
    fun cliExecute_send_fallsBackToGiftOnMailFailure() {
        val giftCalls = mutableListOf<String>()
        val mail = object : net.sourceforge.kolmafia.request.SendMailRequest(
            HttpClient(MockEngine { respond("fail", HttpStatusCode.BadRequest) })
        ) {
            override suspend fun send(
                recipient: String,
                message: String,
                attachments: List<net.sourceforge.kolmafia.request.MailAttachment>,
                meat: Long,
            ): Result<Unit> = Result.failure(Exception("kmail failed"))
        }
        val gift = object : net.sourceforge.kolmafia.request.SendGiftRequest(
            HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        ) {
            override suspend fun send(
                recipient: String,
                message: String,
                attachments: List<net.sourceforge.kolmafia.request.MailAttachment>,
                fromStorage: Boolean,
            ): Result<Unit> {
                giftCalls.add(recipient)
                return Result.success(Unit)
            }
        }
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 99, name = "seal tooth", descId = "x", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 1, plural = null
            )
        }
        val inv = object : net.sourceforge.kolmafia.inventory.InventoryManager(
            HttpClient(MockEngine { respond("ok") }),
            GameEventBus(),
        ) {
            override val state = kotlinx.coroutines.flow.MutableStateFlow(
                net.sourceforge.kolmafia.inventory.InventoryState(
                    items = mapOf(
                        99 to net.sourceforge.kolmafia.inventory.InventoryItem(
                            99, "seal tooth", 1, net.sourceforge.kolmafia.inventory.ItemType.OTHER,
                        ),
                    ),
                ),
            )
        }
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            inventoryManager = inv,
            sendMailRequest = mail,
            sendGiftRequest = gift,
        )
        runLib(lib, """cli_execute("send 1 seal tooth to Buffy");""")
        assertEquals(listOf("Buffy"), giftCalls)
    }

    @Test
    fun cliExecute_counterRelay_listsRelayCounters() {
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        net.sourceforge.kolmafia.session.TurnCounter.startCounting(preferences, 10, 5, "Test Counter loc=*", "x.gif")
        val lib = GameRuntimeLibrary(
            character = net.sourceforge.kolmafia.character.KoLCharacter().also {
                it.updateFromApiResponse(
                    net.sourceforge.kolmafia.character.CharacterApiResponse(currentrun = "10", adventures = "40"),
                )
            },
            preferences = preferences,
        )
        val out = outputLib(lib, """cli_execute("counter relay");""")
        assertTrue(out.contains("Test Counter"))
        assertTrue(out.contains("5 turns"))
    }

    @Test
    fun cliExecute_thralls_printsPastaThrallLevels() {
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        preferences.setString("pastaThrall7", "10,Spice Ghost")
        val lib = GameRuntimeLibrary(preferences = preferences)
        val out = outputLib(lib, """cli_execute("thralls");""")
        assertTrue(out.contains("Spice Ghost"))
        assertTrue(out.contains("level 10"))
    }

    @Test
    fun cliExecute_counters_listsNamedCounters() {
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        preferences.setInt("counter_fights", 3)
        preferences.registerCounterName("fights")
        val lib = GameRuntimeLibrary(preferences = preferences)
        val out = outputLib(lib, """cli_execute("counters");""")
        assertTrue(out.contains("fights: 3"))
    }

    @Test
    fun cliExecute_clear_resetsCapturedOutput() {
        val lib = GameRuntimeLibrary()
        runLib(lib, """cli_execute("echo hello");""")
        assertTrue(lib.lastCliOutput.toString().contains("hello"))
        runLib(lib, """cli_execute("clear");""")
        assertEquals("", lib.lastCliOutput.toString())
    }

    @Test
    fun cliExecute_choice_appliesQuestChoiceRules() {
        val prefs = Preferences(com.russhwolf.settings.MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.NEMESIS, "step12")
        val choiceReq = net.sourceforge.kolmafia.adventure.ChoiceRequest(
            HttpClient(io.ktor.client.engine.mock.MockEngine { respond("You enter the cave.") }),
        )
        val lib = GameRuntimeLibrary(
            questDatabase = db,
            choiceRequest = choiceReq,
        )
        runLib(lib, """cli_execute("choice 1088 1");""")
        assertEquals("step13", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun cliExecute_reset_clearsCapturedOutput() {
        val lib = GameRuntimeLibrary()
        runLib(lib, """cli_execute("echo hello");""")
        runLib(lib, """cli_execute("reset");""")
        assertEquals("", lib.lastCliOutput.toString())
    }

    @Test
    fun cliExecute_alias_listsAndExpands() {
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        val lib = GameRuntimeLibrary(preferences = preferences)
        lib.setCliAlias("hi", "echo hello")
        val listed = outputLib(lib, """cli_execute("alias");""")
        assertTrue(listed.contains("hi => echo hello"))
        val out = outputLib(lib, """cli_execute("hi");""")
        assertTrue(out.contains("hello"))
    }

    @Test
    fun cliExecute_enable_disable_togglesBooleanPref() {
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        val lib = GameRuntimeLibrary(preferences = preferences)
        runLib(lib, """cli_execute("enable autoScripting");""")
        assertTrue(preferences.getBoolean("autoScripting", false))
        runLib(lib, """cli_execute("disable autoScripting");""")
        assertFalse(preferences.getBoolean("autoScripting", true))
    }

    @Test
    fun cliExecute_factory_visitsPacoGuild() {
        val prefs = Preferences(com.russhwolf.settings.MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FACTORY, QuestDatabase.UNSTARTED)
        val html = "<html>7-foot Dwarves are working hard.</html>"
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(io.ktor.client.engine.mock.MockEngine { respond(html) }),
            questDatabase = db,
        )
        runLib(lib, """cli_execute("factory");""")
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.FACTORY))
    }

    @Test
    fun cliExecute_meatcar_visitsPacoGuild() {
        val html = "<html>Degrassi Knoll welcomes you.</html>"
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(io.ktor.client.engine.mock.MockEngine { respond(html) }),
        )
        runLib(lib, """cli_execute("meatcar");""")
    }

    @Test
    fun cliExecute_unalias_removesAlias() {
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        val lib = GameRuntimeLibrary(preferences = preferences)
        lib.setCliAlias("go", "echo hi")
        val out = outputLib(lib, """cli_execute("unalias go");""")
        assertTrue(out.contains("Alias removed"))
        assertTrue(lib.listCliAliases().isEmpty())
    }

    @Test
    fun cliExecute_scg_visitsSealClubberGuild() {
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(io.ktor.client.engine.mock.MockEngine { respond("ok") }),
        )
        runLib(lib, """cli_execute("scg");""")
    }

    @Test
    fun cliExecute_friars_visitsFriarsPage() {
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(io.ktor.client.engine.mock.MockEngine { respond("ok") }),
        )
        runLib(lib, """cli_execute("friars");""")
    }

    @Test
    fun cliExecute_desert_visitsDesertBeach() {
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(io.ktor.client.engine.mock.MockEngine { respond("ok") }),
        )
        runLib(lib, """cli_execute("desert");""")
    }

    @Test
    fun cliExecute_relayStatus_printsPrefState() {
        val prefs = com.russhwolf.settings.MapSettings()
        val preferences = net.sourceforge.kolmafia.preferences.Preferences(prefs)
        preferences.setBoolean("relayActive", true)
        val lib = GameRuntimeLibrary(preferences = preferences)
        val out = outputLib(lib, """cli_execute("relay status");""")
        assertTrue(out.contains("Relay is on"))
    }

    @Test
    fun cliExecute_woods_visitsDistantWoods() {
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(io.ktor.client.engine.mock.MockEngine { respond("ok") }),
        )
        runLib(lib, """cli_execute("woods");""")
    }

    @Test
    fun cliExecute_pyramid_visitsBuriedPyramid() {
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(io.ktor.client.engine.mock.MockEngine { respond("ok") }),
        )
        runLib(lib, """cli_execute("pyramid");""")
    }

    @Test
    fun cliExecute_whatis_printsItemSummary() {
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(name: String) = net.sourceforge.kolmafia.data.ItemData(
                id = 1, name = "seal tooth", descId = "x", image = "",
                primaryUse = net.sourceforge.kolmafia.data.ItemPrimaryUse.NONE,
                secondaryUses = emptySet(), access = setOf('t'), autosellPrice = 50, plural = null
            )
        }
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val out = outputLib(lib, """cli_execute("whatis seal tooth");""")
        assertTrue(out.contains("seal tooth"))
    }

    @Test
    fun cliExecute_guzzlrAcceptBronze_doesNotEchoStub() {
        val prefs = prefs()
        val db = net.sourceforge.kolmafia.quest.QuestDatabase(prefs)
        val choiceReq = net.sourceforge.kolmafia.adventure.ChoiceRequest(
            HttpClient(MockEngine { respond("ok") }),
        )
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            questDatabase = db,
            choiceRequest = choiceReq,
            httpClient = HttpClient(MockEngine { respond("ok") }),
        )
        val out = outputLib(lib, """cli_execute("guzzlr accept bronze");""")
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun cliExecute_guzzlrAcceptGold_withoutBronzeDeliveries_printsTierError() {
        val prefs = prefs()
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            questDatabase = net.sourceforge.kolmafia.quest.QuestDatabase(prefs),
        )
        val out = outputLib(lib, """cli_execute("guzzlr accept gold");""")
        assertTrue(out.contains("5 bronze deliveries"))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun cliExecute_mazeNuggletsAtStep4_runsHedgeMaze() {
        val prefs = prefs()
        prefs.setString(net.sourceforge.kolmafia.quest.Quest.FINAL.prefKey, "step4")
        val db = net.sourceforge.kolmafia.quest.QuestDatabase(prefs)
        val char = net.sourceforge.kolmafia.character.KoLCharacter()
        char.updateAdventuresLeft(20)
        val client = HttpClient(MockEngine { request ->
            if (request.url.toString().contains("ns_03_hedgemaze")) {
                db.setProgress(net.sourceforge.kolmafia.quest.Quest.FINAL, "step5")
            }
            respond("<html>ok</html>")
        })
        val adventureMgr = net.sourceforge.kolmafia.adventure.AdventureManager(
            adventureRequest = net.sourceforge.kolmafia.adventure.AdventureRequest(client),
            fightRequest = net.sourceforge.kolmafia.adventure.FightRequest(client),
            choiceRequest = net.sourceforge.kolmafia.adventure.ChoiceRequest(client),
            characterRequest = net.sourceforge.kolmafia.request.CharacterRequest(client),
            character = char,
            preferences = prefs,
            eventBus = net.sourceforge.kolmafia.event.GameEventBus(),
            questDatabase = db,
        )
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            questDatabase = db,
            httpClient = client,
            character = char,
            adventureManager = adventureMgr,
        )
        val out = outputLib(lib, """cli_execute("maze nugglets");""")
        assertEquals(1, prefs.getInt("choiceAdventure1005", 0))
        assertTrue(out.contains("Entering the Hedge Maze"))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun cliExecute_mazeBeforeOpen_printsError() {
        val prefs = prefs()
        prefs.setString(net.sourceforge.kolmafia.quest.Quest.FINAL.prefKey, "step2")
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            questDatabase = net.sourceforge.kolmafia.quest.QuestDatabase(prefs),
        )
        val out = outputLib(lib, """cli_execute("maze nugglets");""")
        assertTrue(out.contains("Hedge Maze"))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun cliExecute_doorAtStep5_runsTowerDoor() {
        val prefs = prefs()
        prefs.setString(net.sourceforge.kolmafia.quest.Quest.FINAL.prefKey, "step5")
        prefs.setString(
            net.sourceforge.kolmafia.adventure.TowerDoorConfig.KEYS_USED_PREF,
            "Boris's key,Jarlsberg's key,Sneaky Pete's key,Richard's star key,digital key,skeleton key",
        )
        val db = net.sourceforge.kolmafia.quest.QuestDatabase(prefs)
        val client = HttpClient(MockEngine { request ->
            val url = request.url.toString()
            when {
                url.contains("action=ns_doorknob") ->
                    respond("<html>You turn the knob and the door vanishes.</html>")
                else ->
                    respond("""<html><a href="ns_doorknob ">k</a></html>""")
            }
        })
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            questDatabase = db,
            httpClient = client,
            character = net.sourceforge.kolmafia.character.KoLCharacter(),
        )
        val out = outputLib(lib, """cli_execute("door");""")
        assertTrue(out.contains("Tower Door open!"), out)
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun towerCli_printsLockStatusTable() {
        val prefs = Preferences(com.russhwolf.settings.MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FINAL, "step5")
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            questDatabase = db,
            character = KoLCharacter(),
        )
        val out = outputLib(lib, """cli_execute("tower");""")
        assertTrue(out.contains("<table border=2"), out)
        assertTrue(out.contains("Boris's Lock"), out)
        assertTrue(out.contains("Have/Used"), out)
        assertFalse(out.contains("tower.php"), out)
    }

    @Test
    fun towerNeededCli_filtersOwnedLocks() {
        val prefs = Preferences(com.russhwolf.settings.MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FINAL, "step5")
        prefs.setString("nsTowerDoorKeysUsed", "Boris's key,Jarlsberg's key")
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            questDatabase = db,
            character = KoLCharacter(),
        )
        val out = outputLib(lib, """cli_execute("tower needed");""")
        assertFalse(out.contains("Boris's Lock"), out)
        assertTrue(out.contains("Star Lock"), out)
    }

    @Test
    fun lowkeyCli_printsLowKeyLockTable() {
        val prefs = Preferences(com.russhwolf.settings.MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FINAL, "step5")
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = "Low Key"))
        }
        val lib = GameRuntimeLibrary(preferences = prefs, questDatabase = db, character = char)
        val out = outputLib(lib, """cli_execute("lowkey");""")
        assertTrue(out.contains("Polka Dotted Lock"), out)
    }
}
