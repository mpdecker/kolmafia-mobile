package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.ash.outputLib
import net.sourceforge.kolmafia.character.CharpaneValhallaSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.chat.PlayerIdRegistry
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.AscensionHistoryManager

class AscensionHistoryRequestTest {

    @AfterTest
    fun tearDown() {
        PlayerIdRegistry.clearForTest()
        CharpaneValhallaSync.reset()
    }

    @Test
    fun remember_tracksCompareSummaryBetweenFetches() {
        val manager = AscensionHistoryManager()
        manager.remember(listOf(AscensionRecord(182, "Seal Clubber", "Avatar of Boris", 469, 2)))
        manager.remember(listOf(
            AscensionRecord(183, "Pastamancer", "Zombie Slayer", 400, 1),
            AscensionRecord(182, "Seal Clubber", "Avatar of Boris", 500, 2),
        ))

        val compare = manager.lastCompare()
        assertEquals(1, compare.newAscensions.size)
        assertEquals(183, compare.newAscensions.single().number)
        assertEquals(mapOf(182 to 31), compare.turnDeltas)
        assertTrue(manager.statusLines().any { it.contains("New ascension 183") })
    }

    @Test
    fun fetch_getsAscensionHistoryBackSelf() = runTest {
        val captured = mutableListOf<CapturedRequest>()
        val request = historyRequest(client(captured, CURRENT_HTML))

        val result = request.fetch()

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(listOf(HttpMethod.Get), captured.map { it.method })
        assertEquals(listOf("/ascensionhistory.php"), captured.map { it.path })
        assertEquals("back=self", captured.single().query)
        assertEquals(
            listOf(
                AscensionRecord(182, "Seal Clubber", "Avatar of Boris", 469, 2),
                AscensionRecord(181, "Sauceror", "None", 512, 1),
            ),
            result.getOrThrow(),
        )
    }

    @Test
    fun fetch_optionalWhoIsQueryOnlyGet() = runTest {
        val captured = mutableListOf<CapturedRequest>()
        val request = historyRequest(client(captured, CURRENT_HTML))

        val result = request.fetch(177122)

        assertTrue(result.isSuccess, result.exceptionOrNull()?.message)
        assertEquals(listOf(HttpMethod.Get), captured.map { it.method })
        assertEquals("/ascensionhistory.php", captured.single().path)
        assertTrue(captured.single().query.contains("back=self"))
        assertTrue(captured.single().query.contains("who=177122"))
        assertEquals(2, result.getOrThrow().size)
    }

    @Test
    fun parse_preservesUnknownClassAndPathText() {
        val records = AscensionHistoryRequest.parse(UNKNOWN_HTML)
        assertEquals(1, records.size)
        assertEquals(
            AscensionRecord(9, "Mystery Class", "Path of Unknowable Bees", 100, 1),
            records.single(),
        )
    }

    @Test
    fun parse_missingNumericValuesAreNull() {
        val records = AscensionHistoryRequest.parse(MISSING_VALUES_HTML)
        assertEquals(1, records.size)
        val record = records.single()
        assertNull(record.number)
        assertEquals("Accordion Thief", record.className)
        assertEquals("Standard", record.pathName)
        assertNull(record.turns)
        assertNull(record.points)
    }

    @Test
    fun parse_toleratesChangedWhitespaceAndHtmlStructure() {
        val records = AscensionHistoryRequest.parse(WHITESPACE_HTML)
        assertEquals(
            listOf(AscensionRecord(3, "Disco Bandit", "One Crazy Random Summer", 88, 2)),
            records,
        )
    }

    @Test
    fun parse_historicRowsKeepClassAndPathStrings() {
        val records = AscensionHistoryRequest.parse(HISTORIC_HTML)
        assertEquals(
            listOf(AscensionRecord(1, "SC", "Standard", 1200, 1)),
            records,
        )
    }

    @Test
    fun fetch_httpErrorDoesNotOverwriteStateOrCache() = runTest {
        val preferences = Preferences(MapSettings())
        preferences.setInt("borisPoints", 7)
        preferences.setInt("awolPointsCowpuncher", 4)
        CharpaneValhallaSync.markInValhalla()
        val character = KoLCharacter()
        character.setPlayerId(99)
        val before = character.state.value
        val manager = AscensionHistoryManager()
        manager.remember(listOf(AscensionRecord(1, "cached", "cached", 1, 1)))
        val request = AscensionHistoryRequest(
            client = HttpClient(MockEngine { respond("no", HttpStatusCode.InternalServerError) }),
            manager = manager,
            character = character,
            preferences = preferences,
        )

        val result = request.fetch()

        assertTrue(result.isFailure)
        assertEquals(7, preferences.getInt("borisPoints", 0))
        assertEquals(4, preferences.getInt("awolPointsCowpuncher", 0))
        assertTrue(CharpaneValhallaSync.inValhalla)
        assertEquals(before, character.state.value)
        assertEquals(listOf(AscensionRecord(1, "cached", "cached", 1, 1)), manager.records())
    }

    @Test
    fun parse_neverOverwritesCharacterPrefsOrValhalla() {
        val preferences = Preferences(MapSettings())
        preferences.setInt("borisPoints", 43)
        CharpaneValhallaSync.markInValhalla()
        val character = KoLCharacter()
        character.setPlayerId(177122)
        val before = character.state.value
        val manager = AscensionHistoryManager()
        val request = AscensionHistoryRequest(
            client = HttpClient(MockEngine { respond("") }),
            manager = manager,
            character = character,
            preferences = preferences,
        )

        val records = request.parse(CURRENT_HTML)

        assertEquals(2, records.size)
        assertEquals(43, preferences.getInt("borisPoints", 0))
        assertTrue(CharpaneValhallaSync.inValhalla)
        assertEquals(before, character.state.value)
        assertEquals(records, manager.records())
    }

    @Test
    fun cli_reportsCachedRecordsWhenHttpUnavailable() {
        val manager = AscensionHistoryManager()
        manager.remember(listOf(AscensionRecord(5, "Turtle Tamer", "None", 200, 1)))
        val library = GameRuntimeLibrary(
            ascensionHistoryRequest = AscensionHistoryRequest(
                client = HttpClient(MockEngine { respond("no", HttpStatusCode.ServiceUnavailable) }),
                manager = manager,
            ),
        )

        val out = outputLib(library, """cli_execute("ascensionhistory");""")
        assertTrue(out.contains("Turtle Tamer"), out)
        assertTrue(out.contains("200"), out)
    }

    @Test
    fun cli_reportsHttpUnavailableWhenThereIsNoCache() {
        val library = GameRuntimeLibrary(
            ascensionHistoryRequest = AscensionHistoryRequest(
                client = HttpClient(MockEngine { respond("no", HttpStatusCode.ServiceUnavailable) }),
                manager = AscensionHistoryManager(),
            ),
        )

        val out = outputLib(library, """cli_execute("ascensionhistory");""")
        assertEquals("Ascension history HTTP unavailable.", out)
    }

    @Test
    fun cli_resolvesPlayerNameThroughPlayerIdRegistry() = runTest {
        PlayerIdRegistry.register("the Tristero", "177122")
        val captured = mutableListOf<CapturedRequest>()
        val manager = AscensionHistoryManager()
        val library = GameRuntimeLibrary(
            ascensionHistoryRequest = AscensionHistoryRequest(
                client = client(captured, CURRENT_HTML),
                manager = manager,
            ),
        )

        val out = outputLib(library, """cli_execute("ascensionhistory the Tristero");""")
        assertTrue(out.contains("Seal Clubber"), out)
        assertEquals("/ascensionhistory.php", captured.single().path)
        assertEquals(HttpMethod.Get, captured.single().method)
        assertTrue(captured.single().query.contains("who=177122"), captured.single().query)
        assertTrue(captured.single().query.contains("back=self"), captured.single().query)
    }

    @Test
    fun cli_unknownPlayerNameDoesNotFetchSelfHistory() {
        val captured = mutableListOf<CapturedRequest>()
        val library = GameRuntimeLibrary(
            ascensionHistoryRequest = AscensionHistoryRequest(
                client = client(captured, CURRENT_HTML),
                manager = AscensionHistoryManager(),
            ),
        )

        val out = outputLib(library, """cli_execute("ascensionhistory NobodyKnown");""")
        assertTrue(out.contains("Unknown player"), out)
        assertTrue(out.contains("NobodyKnown"), out)
        assertTrue(captured.isEmpty(), "unknown player must not fetch self history: $captured")
    }

    @Test
    fun visitHook_cachesHistoryWithoutMutatingCharacter() {
        val preferences = Preferences(MapSettings())
        preferences.setInt("borisPoints", 11)
        CharpaneValhallaSync.markInValhalla()
        val character = KoLCharacter()
        character.setPlayerId(177122)
        val before = character.state.value
        val manager = AscensionHistoryManager()
        val library = GameRuntimeLibrary(
            character = character,
            preferences = preferences,
            ascensionHistoryRequest = AscensionHistoryRequest(
                client = HttpClient(MockEngine { respond("") }),
                manager = manager,
                character = character,
                preferences = preferences,
            ),
        )

        library.processVisitResponseHooks(CURRENT_HTML, "ascensionhistory.php?back=self")
        library.processVisitResponseHooks(CURRENT_HTML, "ascensionhistory.php?back=self")

        assertEquals(2, manager.records().size)
        assertEquals("Seal Clubber", manager.records().first().className)
        assertEquals(11, preferences.getInt("borisPoints", 0))
        assertTrue(CharpaneValhallaSync.inValhalla)
        assertEquals(before, character.state.value)
    }

    private fun historyRequest(client: HttpClient): AscensionHistoryRequest =
        AscensionHistoryRequest(client, AscensionHistoryManager())

    private fun client(
        captured: MutableList<CapturedRequest>,
        html: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpClient = HttpClient(MockEngine { request ->
        captured += CapturedRequest(
            method = request.method,
            path = request.url.encodedPath,
            query = request.url.encodedQuery,
        )
        respond(html, status)
    })

    private data class CapturedRequest(
        val method: HttpMethod,
        val path: String,
        val query: String,
    )

    companion object {
        private const val CURRENT_HTML =
            """<table class="sortable" id="history"><tr><td class=small><b>#</b></td></tr>""" +
                """<td class=small valign=center>182 &nbsp;&nbsp;</td>""" +
                """<td height=30 class=small valign=center>06/25/07&nbsp;&nbsp;</td>""" +
                """<td class=small valign=center><span title="Level at Ascension: 17">13</span></td>""" +
                """<td class=small valign=center><img src="/images/itemimages/club.gif" alt="Seal Clubber" title="Seal Clubber"></td>""" +
                """<td class=small valign=center>Mongoose&nbsp;&nbsp;</td>""" +
                """<td class=small valign=center><span title='Total Turns: 1,223'>469</span></td>""" +
                """<td class=small valign=center><span title='Total Days: 3'>2</span></td>""" +
                """<td></td>""" +
                """<td class=small valign=center><img src="/images/itemimages/hardcore.gif" alt="Hardcore"><img src="/images/itemimages/boris.gif" alt="Avatar of Boris"></td></tr>""" +
                """<td class=small valign=center>181 &nbsp;&nbsp;</td>""" +
                """<td height=30 class=small valign=center>06/24/07&nbsp;&nbsp;</td>""" +
                """<td class=small valign=center>13</td>""" +
                """<td class=small valign=center><img src="/images/itemimages/saucepan.gif" alt="Sauceror"></td>""" +
                """<td class=small valign=center>Wallaby</td>""" +
                """<td class=small valign=center>512</td>""" +
                """<td class=small valign=center>2</td>""" +
                """<td></td>""" +
                """<td class=small valign=center><img src="/images/otherimages/spacer.gif" width=30 height=30></td></tr>""" +
                """</table>"""

        private const val UNKNOWN_HTML =
            """</tr><td class=small>9</td><td class=small>01/01/20</td><td class=small>13</td>""" +
                """<td class=small><img alt="Mystery Class"></td><td class=small>Vole</td>""" +
                """<td class=small>100</td><td class=small>1</td><td></td>""" +
                """<td class=small><img src="/images/otherimages/spacer.gif"><img alt="Path of Unknowable Bees"></td></tr>"""

        private const val MISSING_VALUES_HTML =
            """</tr><td class=small></td><td class=small>01/02/20</td><td class=small></td>""" +
                """<td class=small><img alt="Accordion Thief"></td><td class=small>Blender</td>""" +
                """<td class=small></td><td class=small></td><td></td>""" +
                """<td class=small>Standard</td></tr>"""

        private const val WHITESPACE_HTML =
            """
            </tr>
            <td class = "small" valign="center">  3  </td>
            <td class="small">01/03/20</td>
            <td class="small">13</td>
            <td class="small">
              <img src='/images/itemimages/discoball.gif' alt = "Disco Bandit" title="Disco Bandit">
            </td>
            <td class="small">Vole</td>
            <td class="small"><span title="Total Turns: 200">88</span></td>
            <td class="small">1</td>
            <td></td>
            <td class="small">
              <img src="/images/itemimages/hardcore.gif" alt="Hardcore">
              <img src="/images/itemimages/dice.gif" alt="One Crazy Random Summer">
            </td>
            </tr>
            """

        private const val HISTORIC_HTML =
            """</tr><td class=small>1</td><td class=small>01/01/06</td><td class=small>13</td>""" +
                """<td class=small>SC</td><td class=small>Mongoose</td><td class=small>1200</td>""" +
                """<td class=small>30</td><td class=small>Normal,Standard</td></tr>"""
    }
}
