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
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType
import kotlin.test.Test
import kotlin.test.assertEquals
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
    private fun fakeFamiliarManager(switchCalls: MutableList<String>): FamiliarManager {
        val client = HttpClient(MockEngine { respond("") })
        val eventBus = GameEventBus()
        return object : FamiliarManager(client, eventBus) {
            override suspend fun setFamiliar(name: String): Result<Unit> {
                switchCalls.add(name)
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
}
