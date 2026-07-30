package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.data.WereProfessorDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences

class ResearchBenchRequestTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private class TestResearchBenchRequest(
        client: HttpClient,
        effects: EffectManager,
        preferences: Preferences,
    ) : ResearchBenchRequest(client, effects, preferences) {
        override fun isMildManneredProfessor(): Boolean = true
    }

    @Test
    fun research_rejectsUnknownField() = runTest {
        val request = TestResearchBenchRequest(
            HttpClient(MockEngine { respond("", HttpStatusCode.OK) }),
            EffectManager(HttpClient(MockEngine { respond("", HttpStatusCode.OK) }), GameEventBus()),
            prefs(),
        )
        assertFailsWith<ResearchBenchRequest.ResearchBenchError.InvalidResearch> {
            request.research("not_a_skill").getOrThrow()
        }
    }

    @Test
    fun visitBench_getsResearchBenchPlace() = runTest {
        var visitedPlace = false
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("place.php")) {
                visitedPlace = true
                assertTrue(request.url.parameters["whichplace"] == "wereprof_cottage")
                assertTrue(request.url.parameters["action"] == "wereprof_researchbench")
                respond(
                    """
                    <p>You have 50 research points (rp).
                    <input type="hidden" name="r" value="wereprof_mus1" />
                    """.trimIndent(),
                    HttpStatusCode.OK,
                )
            } else {
                respond("", HttpStatusCode.OK)
            }
        })
        WereProfessorDatabase.resetForTest()
        WereProfessorDatabase.injectForTest(
            WereProfessorDatabase.parseForTest(
                "1	mus1	10	none	Osteocalcin injection	Mus +20%",
            ),
        )
        val preferences = prefs()
        val request = TestResearchBenchRequest(
            client,
            EffectManager(HttpClient(MockEngine { respond("", HttpStatusCode.OK) }), GameEventBus()),
            preferences,
        )

        request.visitBench().getOrThrow()
        assertTrue(visitedPlace)
        assertEquals(50, preferences.getInt("wereProfessorResearchPoints", 0))
        WereProfessorDatabase.resetForTest()
    }

    @Test
    fun research_postsChoice1523WithWereprofField() = runTest {
        var postedField: String? = null
        val benchHtml = """
            <p>You have 100 research points (rp).
            <input type="hidden" name="r" value="wereprof_mus1" />
        """.trimIndent()
        val client = HttpClient(MockEngine { request ->
            when {
                request.url.encodedPath.endsWith("place.php") ->
                    respond(benchHtml, HttpStatusCode.OK)
                request.method == HttpMethod.Post -> {
                    val form = request.body as FormDataContent
                    postedField = form.formData["r"]
                    assertEquals(WereProfessorDatabase.RESEARCH_BENCH_CHOICE.toString(), form.formData["whichchoice"])
                    assertEquals("1", form.formData["option"])
                    respond(
                        "You successfully research Osteocalcin injection.",
                        HttpStatusCode.OK,
                    )
                }
                else -> respond("", HttpStatusCode.OK)
            }
        })
        WereProfessorDatabase.resetForTest()
        WereProfessorDatabase.injectForTest(
            WereProfessorDatabase.parseForTest(
                "1	mus1	10	none	Osteocalcin injection	Mus +20%",
            ),
        )
        val preferences = prefs()
        preferences.setInt(AdventureManager.LAST_CHOICE_ID, 0)
        val request = TestResearchBenchRequest(
            client,
            EffectManager(HttpClient(MockEngine { respond("", HttpStatusCode.OK) }), GameEventBus()),
            preferences,
        )

        request.research("mus1").getOrThrow()
        assertEquals("wereprof_mus1", postedField)
        WereProfessorDatabase.resetForTest()
    }

    @Test
    fun buildResearchUrl_formatsChoice1523Link() {
        assertEquals(
            "choice.php?whichchoice=1523&option=1&r=wereprof_mus1",
            ResearchBenchRequest.buildResearchUrl("mus1"),
        )
    }
}
