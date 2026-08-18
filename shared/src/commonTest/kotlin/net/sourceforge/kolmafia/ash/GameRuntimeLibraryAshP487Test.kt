package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import net.sourceforge.kolmafia.buffbot.BuffBotDatabase
import net.sourceforge.kolmafia.buffbot.BuffBotManager
import net.sourceforge.kolmafia.buffbot.BuffCost
import net.sourceforge.kolmafia.chat.ChatSender
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType

class GameRuntimeLibraryAshP487Test {

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
    }

    private fun def(
        id: Int,
        name: String,
        tags: Set<String>,
    ): SkillDefinition {
        val skill = SkillDefinition(
            id = id,
            name = name,
            image = "skill.gif",
            tags = tags,
            mpCost = 1,
            duration = 0,
            isPassive = "passive" in tags,
            isCombat = "combat" in tags,
            isNonCombat = "nc" in tags,
            isSong = "song" in tags,
        )
        SkillDefinitionDatabase.registerForTest(skill)
        return skill
    }

    private fun registerCatalog() {
        def(10, "Ambidextrous Funkslinging", setOf("passive"))
        def(15, "Lunging Thrust-Smack", setOf("combat"))
        def(1010, "Tongue of the Walrus", setOf("nc", "heal", "self"))
        def(3010, "Leash of Linguini", setOf("nc", "effect", "other"))
        def(6014, "The Ode to Booze", setOf("nc", "effect", "other", "song"))
    }

    private fun ownedSkills(): List<SkillData> = listOf(
        SkillData(10, "Ambidextrous Funkslinging", SkillType.PASSIVE, 0, 0, 0),
        SkillData(15, "Lunging Thrust-Smack", SkillType.COMBAT, 1, 0, 0),
        SkillData(1010, "Tongue of the Walrus", SkillType.NONCOMBAT, 3, 0, 0),
        SkillData(3010, "Leash of Linguini", SkillType.BUFF, 1, 0, 0),
        SkillData(6014, "The Ode to Booze", SkillType.BUFF, 50, 0, 0),
    )

    private fun fakeSkillManager(skills: List<SkillData>): SkillManager {
        val json = "{" + skills.joinToString(",") { s ->
            """"${s.id}":{"name":"${s.name}","type":5,"dailylimit":${s.dailyLimit},"timescast":${s.timesCast},"mpcost":${s.mpCost}}"""
        } + "}"
        val engine = MockEngine {
            respond(
                content = json,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return SkillManager(client, SkillCastRequest(client), GameEventBus()).also { mgr ->
            runBlocking { mgr.fetchSkills() }
        }
    }

    private fun skillsLib(): GameRuntimeLibrary {
        registerCatalog()
        return GameRuntimeLibrary(skillManager = fakeSkillManager(ownedSkills()))
    }

    private fun names(out: String): List<String> =
        out.lines().map { it.trim() }.filter { it.isNotEmpty() }

    @Test
    fun revision_phase487() {
        assertEquals("phase635", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun skills_listsAllOwnedNames() {
        val out = outputLib(skillsLib(), """cli_execute("skills");""")
        val listed = names(out)
        assertTrue(listed.contains("Ambidextrous Funkslinging"))
        assertTrue(listed.contains("Lunging Thrust-Smack"))
        assertTrue(listed.contains("Tongue of the Walrus"))
        assertTrue(listed.contains("Leash of Linguini"))
        assertTrue(listed.contains("The Ode to Booze"))
        assertEquals(5, listed.size)
    }

    @Test
    fun skill_bareListsAllOwnedNames() {
        val out = outputLib(skillsLib(), """cli_execute("skill");""")
        assertEquals(5, names(out).size)
        assertTrue(out.contains("Leash of Linguini"))
    }

    @Test
    fun skillsCombat_andCombatAlias_listCombatOnly() {
        val lib = skillsLib()
        val fromSkills = names(outputLib(lib, """cli_execute("skills combat");"""))
        val fromAlias = names(outputLib(lib, """cli_execute("combat");"""))
        assertEquals(listOf("Lunging Thrust-Smack"), fromSkills)
        assertEquals(fromSkills, fromAlias)
    }

    @Test
    fun passAndPassive_listPassiveOnly() {
        val lib = skillsLib()
        val fromPass = names(outputLib(lib, """cli_execute("pass");"""))
        val fromPassive = names(outputLib(lib, """cli_execute("passive");"""))
        val fromSkills = names(outputLib(lib, """cli_execute("skills passive");"""))
        assertEquals(listOf("Ambidextrous Funkslinging"), fromPass)
        assertEquals(fromPass, fromPassive)
        assertEquals(fromPass, fromSkills)
    }

    @Test
    fun self_listsSelfOnly() {
        val lib = skillsLib()
        val fromSkills = names(outputLib(lib, """cli_execute("skills self");"""))
        val fromAlias = names(outputLib(lib, """cli_execute("self");"""))
        assertEquals(listOf("Tongue of the Walrus"), fromSkills)
        assertEquals(fromSkills, fromAlias)
    }

    @Test
    fun buff_listsBuffSkillsAfterFetch() {
        val out = outputLib(skillsLib(), """cli_execute("buff");""")
        val listed = names(out)
        assertTrue(listed.contains("Leash of Linguini"))
        assertTrue(listed.contains("The Ode to Booze"))
        assertFalse(listed.contains("Lunging Thrust-Smack"))
        assertFalse(listed.contains("Ambidextrous Funkslinging"))
        assertFalse(listed.contains("Tongue of the Walrus"))
    }

    @Test
    fun skillsCast_andBareCast_listNoncombat() {
        val lib = skillsLib()
        val fromSkills = names(outputLib(lib, """cli_execute("skills cast");""")).toSet()
        val fromCast = names(outputLib(lib, """cli_execute("cast");""")).toSet()
        assertEquals(
            setOf("Tongue of the Walrus", "Leash of Linguini", "The Ode to Booze"),
            fromSkills,
        )
        assertEquals(fromSkills, fromCast)
        assertFalse(fromSkills.contains("Lunging Thrust-Smack"))
        assertFalse(fromSkills.contains("Ambidextrous Funkslinging"))
    }

    @Test
    fun skills_substringFiltersName() {
        val out = outputLib(skillsLib(), """cli_execute("skills linguini");""")
        assertEquals(listOf("Leash of Linguini"), names(out))
    }

    @Test
    fun skillsCombat_typePrefixClearsRemainder() {
        val out = outputLib(skillsLib(), """cli_execute("skills combat linguini");""")
        assertEquals(listOf("Lunging Thrust-Smack"), names(out))
    }

    @Test
    fun skillsSong_listsSongs() {
        val out = outputLib(skillsLib(), """cli_execute("skills song");""")
        assertEquals(listOf("The Ode to Booze"), names(out))
    }

    @Test
    fun buffBot_stillRequestsBot() {
        val db = BuffBotDatabase.forTest(
            costs = listOf(
                BuffCost(buffId = 3004, buffName = "Empathy of the Newt", meatCost = 100L, turns = 10),
            ),
        )
        var recipient = ""
        var message = ""
        val sender = object : ChatSender(
            HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }),
        ) {
            override suspend fun sendPrivate(rec: String, msg: String): Result<Unit> {
                recipient = rec
                message = msg
                return Result.success(Unit)
            }
        }
        registerCatalog()
        val lib = GameRuntimeLibrary(
            skillManager = fakeSkillManager(ownedSkills()),
            buffBotManager = BuffBotManager(sender, db),
            buffBotDatabase = db,
        )
        val out = outputLib(lib, """cli_execute("buff OakBot 3004 10");""")
        assertTrue(out.contains("Buff request sent to OakBot"))
        assertEquals("OakBot", recipient)
        assertEquals("3004 10", message)
        assertFalse(out.contains("Leash of Linguini"))
    }
}
