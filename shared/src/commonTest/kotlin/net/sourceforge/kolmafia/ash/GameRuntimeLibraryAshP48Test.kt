package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager

class GameRuntimeLibraryAshP48Test {

    private fun skillManagerWithOverclocked(): SkillManager {
        val json =
            """{"$OVERCLOCKED_SKILL_ID":{"name":"Overclocked","type":5,"dailylimit":0,"timescast":0,"mpcost":0}}"""
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

    @Test
    fun jumpChance_missingInit_returnsMinusOne() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "-1",
            outputLib(lib, """print(jump_chance(to_monster("crazy bastard")));""").trim(),
        )
    }

    @Test
    fun jumpChance_overclockedSkillWired_mosquitoUnaffected() = runBlocking {
        val db = GameDatabase()
        db.load()
        val without = GameRuntimeLibrary(gameDatabase = db)
        val with = GameRuntimeLibrary(
            gameDatabase = db,
            skillManager = skillManagerWithOverclocked(),
        )
        assertTrue(
            with.skillManager?.state?.value?.skills?.any { it.id == OVERCLOCKED_SKILL_ID } == true,
        )
        // Non-Source-Agent monsters ignore Overclocked
        assertEquals(
            outputLib(without, """print(jump_chance(to_monster("huge mosquito"), 0, 0));""").trim(),
            outputLib(with, """print(jump_chance(to_monster("huge mosquito"), 0, 0));""").trim(),
        )
    }

    @Test
    fun jumpChance_zeroArg_missingInitLastMonster() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_MONSTER, "crazy bastard")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals("-1", outputLib(lib, """print(jump_chance());""").trim())
    }
}
