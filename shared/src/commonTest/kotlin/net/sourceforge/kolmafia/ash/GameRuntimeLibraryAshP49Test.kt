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
import net.sourceforge.kolmafia.session.DreadKissesTracker
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager

class GameRuntimeLibraryAshP49Test {

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
    fun sourceAgent_initEvaluatesPref() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("sourceAgentsDefeated", "0")
        val lib = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        assertEquals(
            "25",
            outputLib(lib, """print(monster_initiative(to_monster("Source Agent")));""").trim(),
        )
        prefs.setString("sourceAgentsDefeated", "2")
        assertEquals(
            "75",
            outputLib(lib, """print(monster_initiative(to_monster("Source Agent")));""").trim(),
        )
    }

    @Test
    fun sourceAgent_jumpChance_stacksOverclocked() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        prefs.setString("sourceAgentsDefeated", "0")
        val without = GameRuntimeLibrary(gameDatabase = db, preferences = prefs)
        val with = GameRuntimeLibrary(
            gameDatabase = db,
            preferences = prefs,
            skillManager = skillManagerWithOverclocked(),
        )
        // Init 25, Atk 30 (expr), mainstat 0 → jump 100-25=75; Overclocked → 100
        assertEquals(
            "75",
            outputLib(without, """print(jump_chance(to_monster("Source Agent"), 0, 0));""").trim(),
        )
        assertEquals(
            "100",
            outputLib(with, """print(jump_chance(to_monster("Source Agent"), 0, 0));""").trim(),
        )
    }

    @Test
    fun dreadKiss_initEvaluatesKW() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        val kisses = DreadKissesTracker(prefs)
        kisses.setKissesForTest("The Primordial Soup Woods", 3)
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            preferences = prefs,
            dreadKissesTracker = kisses,
        )
        // cold bugbear Init: [15+KW*10] with KW=3 → 45
        assertEquals(
            "45",
            outputLib(lib, """print(monster_initiative(to_monster("cold bugbear")));""").trim(),
        )
    }

    @Test
    fun numericInit_unchanged() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "20",
            outputLib(lib, """print(monster_initiative(to_monster("huge mosquito")));""").trim(),
        )
        assertEquals(
            "80",
            outputLib(lib, """print(jump_chance(to_monster("huge mosquito"), 0, 0));""").trim(),
        )
    }

    @Test
    fun missingInit_stillMinusOne() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "-1",
            outputLib(lib, """print(jump_chance(to_monster("crazy bastard")));""").trim(),
        )
    }

    @Test
    fun revision_startsWithPhase() {
        assertTrue(GameRuntimeLibrary.REVISION.startsWith("phase"))
    }
}
