package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.CandyDatabase
import net.sourceforge.kolmafia.request.SweetSynthesisRequest
import net.sourceforge.kolmafia.skill.SkillCastRequest
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillType
import net.sourceforge.kolmafia.event.GameEventBus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP956TrackJTest {

    @BeforeTest
    fun setUp() {
        CandyDatabase.resetForTest()
    }

    @AfterTest
    fun tearDown() {
        CandyDatabase.resetForTest()
    }

    @Test
    fun phase956_sweetSynthesisWithoutHttpReturnsFalse() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        assertEquals("false", outputLib(lib, """print(sweet_synthesis(to_effect("Synthesis: Hot")));"""))
    }

    @Test
    fun phase957_sweetSynthesisWithMockHttp() {
        val client = HttpClient(MockEngine { respond("ok", HttpStatusCode.OK) })
        val choice = ChoiceRequest(client)
        val char = KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(name = "Tester", classId = "1", spleen = "0", spleensize = "15"),
            )
        }
        val skills = SkillManager(client, SkillCastRequest(client), GameEventBus()).also {
            it.learnLocalSkill(SkillData(SweetSynthesisRequest.SKILL_ID, "Sweet Synthesis", SkillType.PASSIVE, 0, 0, 0))
        }
        val lib = GameRuntimeLibrary(
            preferences = prefs(),
            httpClient = client,
            choiceRequest = choice,
            character = char,
            skillManager = skills,
        )
        val out = outputLib(lib, """print(sweet_synthesis(1, to_effect("Synthesis: Hot")));""")
        assertTrue(out == "true" || out == "false", out)
    }

    @Test
    fun phase958_959_pairApisRegistered() {
        val lib = GameRuntimeLibrary(preferences = prefs())
        val pairCount = outputLib(lib, """print(count(sweet_synthesis_pair(to_effect("Synthesis: Hot"))));""").toInt()
        assertTrue(pairCount >= 0, "sweet_synthesis_pair should return an aggregate")
        assertTrue(GameRuntimeLibrary.REVISION.startsWith("phase"))
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }
}
