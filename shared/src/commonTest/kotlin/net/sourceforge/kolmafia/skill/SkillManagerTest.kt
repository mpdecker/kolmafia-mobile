package net.sourceforge.kolmafia.skill

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.event.GameEvent
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SkillManagerTest {

    // api.php?what=skills returns a JSON object keyed by skill ID string.
    private val skillsJson = """
        {
          "3": {"name": "Thrust-Smack", "type": 3, "dailylimit": 0, "timescast": 5, "mpcost": 0},
          "6": {"name": "Moxie of the Mariachi", "type": 5, "dailylimit": 3, "timescast": 1, "mpcost": 10}
        }
    """.trimIndent()

    private fun makeManager(
        skillsResponse: String = skillsJson,
        castResponseBody: String = "<html>You cast the skill!</html>",
        preferences: Preferences? = null,
    ): Pair<SkillManager, GameEventBus> {
        val engine = MockEngine { request ->
            when {
                request.url.parameters["what"] == "skills" ->
                    respond(skillsResponse, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"))
                request.url.encodedPath.contains("skills.php") ->
                    respond(castResponseBody, HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"))
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val bus = GameEventBus()
        return SkillManager(client, SkillCastRequest(client), bus, preferences) to bus
    }

    @Test
    fun fetchSkills_populatesSkillList() = runTest {
        val (manager, _) = makeManager()
        manager.fetchSkills()
        val state = manager.state.value
        assertEquals(2, state.skills.size)
        assertFalse(state.isStale)
    }

    @Test
    fun fetchSkills_mapsTypeCorrectly() = runTest {
        val (manager, _) = makeManager()
        manager.fetchSkills()
        val skills = manager.state.value.skills
        val combat = skills.find { it.id == 3 }
        val buff = skills.find { it.id == 6 }
        assertNotNull(combat)
        assertNotNull(buff)
        assertEquals(SkillType.COMBAT, combat.type)
        assertEquals(SkillType.BUFF, buff.type)
    }

    @Test
    fun fetchSkills_tracksDailyLimits() = runTest {
        val (manager, _) = makeManager()
        manager.fetchSkills()
        val buff = manager.state.value.skills.find { it.id == 6 }
        assertNotNull(buff)
        assertEquals(3, buff.dailyLimit)
        assertEquals(1, buff.timesCast)
        assertTrue(buff.canCastMore)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun castSkill_incrementsTimesCast_andEmitsEvent() = runTest {
        val (manager, bus) = makeManager()
        manager.fetchSkills()
        val received = mutableListOf<GameEvent>()
        // Launch collector; advanceUntilIdle will run it to subscription
        val collectJob = launch { bus.events.collect { received.add(it) } }
        advanceUntilIdle() // let collector subscribe

        val thrustSmack = manager.state.value.skills.first { it.id == 3 }
        manager.cast(thrustSmack, 1)
        advanceUntilIdle() // let collector receive the event

        collectJob.cancel()
        val castEvents = received.filterIsInstance<GameEvent.SkillCast>()
        assertEquals(1, castEvents.size)
        assertEquals(3, castEvents.first().skillId)
        assertEquals("Thrust-Smack", castEvents.first().skillName)
    }

    @Test
    fun castSkill_returnsFailure_onMpError() = runTest {
        val (manager, _) = makeManager(
            castResponseBody = "<html>You don't have enough MP to cast that skill.</html>"
        )
        manager.fetchSkills()
        val skill = manager.state.value.skills.first { it.id == 3 }
        val result = manager.cast(skill, 1)
        assertTrue(result.isFailure)
    }

    @Test
    fun initialState_isEmpty() {
        val (manager, _) = makeManager()
        assertTrue(manager.state.value.skills.isEmpty())
    }

    @Test
    fun castLibramSkill_incrementsLibramSummonsPref() = runTest {
        val p = MapSettings()
        p.putInt(Preferences.LIBRAM_SUMMONS, 2)
        val prefs = Preferences(p)
        val libramJson = """
            {
              "7219": {"name": "Summon Candy Heart", "type": 5, "dailylimit": 0, "timescast": 0, "mpcost": 1}
            }
        """.trimIndent()
        val (manager, _) = makeManager(skillsResponse = libramJson, preferences = prefs)
        manager.fetchSkills()
        val skill = manager.state.value.skills.first { it.id == 7219 }
        val result = manager.cast(skill, 3)
        assertTrue(result.isSuccess)
        assertEquals(5, prefs.getInt(Preferences.LIBRAM_SUMMONS, 0))
    }
}
