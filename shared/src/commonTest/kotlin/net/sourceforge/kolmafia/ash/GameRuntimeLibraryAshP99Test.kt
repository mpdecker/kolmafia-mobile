package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.SkillDefinition
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BirdOfTheDaySync
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillManager

class GameRuntimeLibraryAshP99Test {

    @AfterTest
    fun tearDown() {
        SkillDefinitionDatabase.resetForTest()
    }

    @Test
    fun revision_phase141() {
        assertEquals("phase479", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun birdDescVisit_learnsSeekBirdSkillLocally() {
        SkillDefinitionDatabase.registerForTest(
            SkillDefinition(
                id = BirdOfTheDaySync.SEEK_OUT_A_BIRD_SKILL_ID,
                name = BirdOfTheDaySync.SEEK_OUT_A_BIRD_BASE_NAME,
                image = "findbird.gif",
                tags = setOf("nc", "effect", "self"),
                mpCost = 5,
                duration = 10,
                isPassive = false,
                isCombat = false,
                isNonCombat = true,
                isSong = false,
            ),
        )
        val skillsJson = """
            {
              "7323": {"name": "Seek out a Turkey", "type": 1, "dailylimit": 0, "timescast": 0, "mpcost": 20}
            }
        """.trimIndent()
        val engine = MockEngine { request ->
            when {
                request.url.parameters["what"] == "skills" ->
                    respond(
                        skillsJson,
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val skillManager = SkillManager(client, SkillCastRequest(client), GameEventBus())
        val prefs = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(
            skillManager = skillManager,
            preferences = prefs,
        )
        val html = """<b>Seek out a Turkey</b><br><b>MP Cost:</b> 20"""
        lib.processVisitResponseHooks(
            html,
            "desc_skill.php?whichskill=${BirdOfTheDaySync.SEEK_OUT_A_BIRD_SKILL_ID}&self=true",
        )
        assertEquals(1, prefs.getInt("skillLevel7323", 0))
        assertTrue(prefs.getBoolean("_canSeekBirds", false))
        assertTrue(
            lib.resolvedSkillNames().any {
                it.equals("Seek out a Turkey", ignoreCase = true)
            },
        )
        assertTrue(
            outputLib(lib, """print(have_skill(to_skill("Seek out a Turkey")));""").toBoolean(),
        )
    }
}
