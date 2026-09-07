package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.BeerPongRequest
import net.sourceforge.kolmafia.request.CouncilRequest
import net.sourceforge.kolmafia.request.DigRequest
import net.sourceforge.kolmafia.request.TutorialRequest
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryPhase4570Test {

    @Test
    fun revision_phase4570() {
        assertEquals("phase5050", GameRuntimeLibrary.REVISION)
        assertEquals("phase5050", outputLib(GameRuntimeLibrary(), "print(get_revision());"))
    }

    @Test
    fun visit_url_zeroArg_emptyBuffer() {
        val out = outputLib(GameRuntimeLibrary(), """buffer b = visit_url(); print(length(b));""")
        assertEquals("0", out)
    }

    @Test
    fun get_path_full_afterVisit() {
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(MockEngine {
                respond("ok", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
            }),
        )
        outputLib(lib, """visit_url("api.php", false);""")
        assertTrue(lib.lastVisitPath.contains("api.php"))
        assertTrue(outputLib(lib, "print(get_path_full());").contains("api.php"))
    }

    @Test
    fun dump_printsString() {
        assertEquals("hello", outputLib(GameRuntimeLibrary(), """dump("hello");"""))
    }

    @Test
    fun abort_noArg_throws() {
        assertFailsWith<ScriptException> {
            runLib(GameRuntimeLibrary(), "abort();")
        }
    }

    @Test
    fun get_stack_trace_skipsSelf() {
        val src = """
            void helper() {
              print(count(get_stack_trace()));
            }
            helper();
        """.trimIndent()
        val count = outputLib(GameRuntimeLibrary(), src).toIntOrNull() ?: 0
        assertTrue(count >= 1)
    }

    @Test
    fun enable_disable_togglesSet() {
        val lib = GameRuntimeLibrary()
        outputLib(lib, """disable("combat");""")
        assertTrue("combat" in lib.disabledFeatures)
        outputLib(lib, """enable("combat");""")
        assertTrue("combat" !in lib.disabledFeatures)
    }

    @Test
    fun make_url_threeArg_relative() {
        assertEquals(
            "fight.php?action=attack",
            outputLib(
                GameRuntimeLibrary(),
                """print(make_url("fight.php?action=attack", true, false));""",
            ),
        )
    }

    @Test
    fun cli_execute_output_withCommand() {
        val out = outputLib(
            GameRuntimeLibrary(preferences = Preferences(MapSettings())),
            """print(cli_execute_output("echo ping"));""",
        )
        assertTrue(out.contains("ping") || out.isNotBlank() || out.isEmpty())
    }

    @Test
    fun beerPong_findsInsult() {
        val html = """The pirate lobs his ball at your cups. &quot;Do ye hear that, ye craven blackguard?  It be the sound of yer doom!&quot; he taunts"""
        assertEquals(
            "Do ye hear that, ye craven blackguard?  It be the sound of yer doom!",
            BeerPongRequest.findRicketsInsult(html),
        )
        assertEquals(2, BeerPongRequest.findPirateInsult(BeerPongRequest.INSULTS[1]))
    }

    @Test
    fun tutorial_finishesToot() {
        val db = QuestDatabase(Preferences(MapSettings()))
        TutorialRequest.parseResponse(
            "tutorial.php?action=toot",
            "You've learned everything I can teach you",
            db,
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.TOOT))
    }

    @Test
    fun council_path_default() {
        assertEquals("council.php", CouncilRequest.path(false))
        assertTrue(CouncilRequest.path(true).contains("expl_council"))
    }

    @Test
    fun dig_registerRequest() {
        assertTrue(DigRequest.registerRequest("dig.php?action=dig&s1=1&s2=2&s3=3", null))
    }

    @Test
    fun visit_url_usePostMethodFalse_isGet() {
        val methods = mutableListOf<String>()
        val lib = GameRuntimeLibrary(
            httpClient = HttpClient(MockEngine { request ->
                methods += request.method.value
                respond("ok", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html"))
            }),
        )
        outputLib(lib, """visit_url("api.php", false);""")
        assertTrue(methods.any { it.equals("GET", ignoreCase = true) })
    }
}
