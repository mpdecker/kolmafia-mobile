package net.sourceforge.kolmafia.session

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ProfileRequest

class PvpManagerTest {

    @BeforeTest
    fun reset() {
        PvpManager.resetForTest()
    }

    @AfterTest
    fun cleanup() {
        PvpManager.resetForTest()
    }

    private val sampleDropdown = """
        <select name="stance"><option value="0" >Bear Hugs All Around</option><option value="1" selected>Beary Famous</option><option value="2" >Barely Dressed</option><option value="3" >Basket Reaver</option><option value="7" >Most Things Eaten</option><option value="11" >Most Murderous</option></select>
    """.trimIndent()

    @Test
    fun parseStances_readsOptionIdsAndNames() {
        PvpManager.parseStances(sampleDropdown)
        assertTrue(PvpManager.stancesKnown)
        assertEquals("Beary Famous", PvpManager.findStance(1))
        assertEquals(1, PvpManager.findStance("Beary Famous"))
        assertEquals(0, PvpManager.stanceToOption["Bear Hugs All Around"])
        assertEquals(6, PvpManager.optionToStance.size)
    }

    @Test
    fun findStance_uniqueSubstring_matches() {
        PvpManager.parseStances(sampleDropdown)
        assertEquals(1, PvpManager.findStance("beary"))
    }

    @Test
    fun findStance_ambiguousSubstring_returnsMinusOne() {
        PvpManager.parseStances(sampleDropdown)
        assertEquals(-1, PvpManager.findStance("most"))
    }

    @Test
    fun findStance_unknownName_returnsMinusOne() {
        PvpManager.parseStances(sampleDropdown)
        assertEquals(-1, PvpManager.findStance("swagger"))
    }

    @Test
    fun findStance_numericLookup_missingOption() {
        PvpManager.parseStances(sampleDropdown)
        assertNull(PvpManager.findStance(99))
    }

    @Test
    fun parseStances_emptyHtml_leavesStancesUnknown() {
        PvpManager.parseStances("<html>no dropdown here</html>")
        assertFalse(PvpManager.stancesKnown)
        assertEquals(-1, PvpManager.findStance("beary"))
        assertTrue(PvpManager.optionToStance.isEmpty())
    }

    @Test
    fun reset_clearsKnownStances() {
        PvpManager.parseStances(sampleDropdown)
        PvpManager.reset()
        assertFalse(PvpManager.stancesKnown)
        assertTrue(PvpManager.stanceToOption.isEmpty())
        assertFalse(PvpManager.noFight)
        assertNull(PvpManager.abortReason)
    }

    private fun character(fights: Int, stoneBroken: Boolean): KoLCharacter =
        KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    pvpfights = fights.toString(),
                    hippystone = if (stoneBroken) "1" else "0",
                ),
            )
        }

    private fun winHtml(remaining: Int) = """
        You have $remaining fights remaining today.
        <div class="fight"><a href="showplayer.php?who=1"><b>Hero</b></a> calls out <a href="showplayer.php?who=2"><b>Villain</b></a> for battle!
        <span class="win"><b>Hero</b> won the fight, <b>8</b> to <b>3</b>!
    """.trimIndent()

    @Test
    fun executePvpRequest_loopsRequestedFights() = runTest {
        val char = character(fights = 5, stoneBroken = true)
        var fightPosts = 0
        val client = HttpClient(MockEngine {
            fightPosts++
            val remaining = (5 - fightPosts).coerceAtLeast(0)
            respond(winHtml(remaining), HttpStatusCode.OK)
        })
        PvpManager.executePvpRequest(
            attacks = 3,
            mission = "flowers",
            stance = 1,
            tougher = false,
            client = client,
            character = char,
        )
        assertEquals(3, fightPosts)
        assertEquals(2, char.state.value.pvpFightsLeft)
    }

    @Test
    fun executePvpRequest_smashesWhenStoneUnbroken() = runTest {
        val char = character(fights = 0, stoneBroken = false)
        val urls = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            urls += request.url.toString()
            when {
                request.url.toString().contains("smashstone") ->
                    respond("You shatter your Magical Mystical Hippy Stone.", HttpStatusCode.OK)
                else -> respond(winHtml(9), HttpStatusCode.OK)
            }
        })
        PvpManager.executePvpRequest(
            attacks = 1,
            mission = "flowers",
            stance = 0,
            tougher = false,
            client = client,
            character = char,
        )
        assertTrue(urls.any { it.contains("smashstone") })
        assertTrue(char.state.value.hippyStoneBroken)
        assertEquals(9, char.state.value.pvpFightsLeft)
    }

    @Test
    fun executePvpRequest_noFightDoesNotCount() = runTest {
        val char = character(fights = 3, stoneBroken = true)
        var fightPosts = 0
        val client = HttpClient(MockEngine {
            fightPosts++
            if (fightPosts == 1) {
                respond(
                    "Your opponent contains a Mystical Magical Hippy Stone. You have 3 fights remaining today.",
                    HttpStatusCode.OK,
                )
            } else {
                respond(winHtml(2), HttpStatusCode.OK)
            }
        })
        PvpManager.executePvpRequest(
            attacks = 1,
            mission = "flowers",
            stance = 0,
            tougher = false,
            client = client,
            character = char,
        )
        assertEquals(2, fightPosts)
        assertEquals(2, char.state.value.pvpFightsLeft)
        assertFalse(PvpManager.noFight)
    }

    @Test
    fun checkStances_visitsFightWhenUnknown() = runTest {
        val char = character(fights = 0, stoneBroken = true)
        val html = """
            You have 4 fights remaining today.
            <select name="stance"><option value="0" >Bear Hugs All Around</option></select>
        """.trimIndent()
        val client = HttpClient(MockEngine { respond(html, HttpStatusCode.OK) })
        assertTrue(PvpManager.checkStances(client, char))
        assertTrue(PvpManager.stancesKnown)
        assertEquals(4, char.state.value.pvpFightsLeft)
    }

    @Test
    fun executeDirected_skipsCurrentVictories() = runTest {
        val char = character(fights = 3, stoneBroken = true)
        val prefs = Preferences(MapSettings())
        prefs.setString("currentPvpVictories", "Villain,")
        var fightPosts = 0
        val client = HttpClient(MockEngine {
            fightPosts++
            respond(winHtml(2), HttpStatusCode.OK)
        })
        PvpManager.executeDirectedPvpRequest(
            targets = listOf(ProfileRequest("Villain", "2")),
            mission = "flowers",
            stance = 1,
            client = client,
            character = char,
            preferences = prefs,
        )
        assertEquals(0, fightPosts)
    }

    @Test
    fun executeDirected_skipsDevster() = runTest {
        val char = character(fights = 3, stoneBroken = true)
        var fightPosts = 0
        val client = HttpClient(MockEngine {
            fightPosts++
            respond(winHtml(2), HttpStatusCode.OK)
        })
        PvpManager.executeDirectedPvpRequest(
            targets = listOf(ProfileRequest("Devster99", "9")),
            mission = "flowers",
            stance = 1,
            client = client,
            character = char,
        )
        assertEquals(0, fightPosts)
    }

    @Test
    fun executeDirected_forcesFlowersWhenTargetInRonin() = runTest {
        val char = character(fights = 3, stoneBroken = true)
        var body = ""
        val client = HttpClient(MockEngine { request ->
            body = request.body.toByteArray().decodeToString()
            respond(winHtml(2), HttpStatusCode.OK)
        })
        PvpManager.executeDirectedPvpRequest(
            targets = listOf(ProfileRequest("Target", "2", inRonin = true)),
            mission = "lootwhatever",
            stance = 1,
            client = client,
            character = char,
        )
        assertTrue(body.contains("ranked=0"))
        assertTrue(body.contains("who=Target"))
        assertTrue(body.contains("attacktype=flowers"))
    }

    @Test
    fun executeDirected_printsDignityLoss() = runTest {
        val char = character(fights = 3, stoneBroken = true)
        val printed = mutableListOf<String>()
        val client = HttpClient(MockEngine {
            respond("You lost some dignity in the attempt", HttpStatusCode.OK)
        })
        PvpManager.executeDirectedPvpRequest(
            targets = listOf(
                ProfileRequest("Target", "2"),
                ProfileRequest("Second", "3"),
            ),
            mission = "flowers",
            stance = 1,
            client = client,
            character = char,
            print = { printed += it },
        )
        assertTrue(printed.any { it.contains("You lost to Target.") })
        assertEquals("You lost to Target.", PvpManager.abortReason)
        assertFalse(printed.any { it.contains("Attacking Second") })
    }

    @Test
    fun executePvpRequest_runsBeforePvpScriptEachFight() = runTest {
        val char = character(fights = 2, stoneBroken = true)
        val prefs = Preferences(MapSettings())
        prefs.setString("beforePVPScript", "joke")
        val scripts = mutableListOf<String>()
        val client = HttpClient(MockEngine {
            respond(winHtml(1), HttpStatusCode.OK)
        })
        PvpManager.executePvpRequest(
            attacks = 2,
            mission = "flowers",
            stance = 0,
            tougher = false,
            client = client,
            character = char,
            preferences = prefs,
            cliExecutor = { scripts += it },
        )
        assertEquals(listOf("joke", "joke"), scripts)
    }
}
