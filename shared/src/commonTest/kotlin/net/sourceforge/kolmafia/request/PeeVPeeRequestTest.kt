package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.PvpManager
import net.sourceforge.kolmafia.session.SessionLogger

class PeeVPeeRequestTest {

    @BeforeTest
    fun reset() {
        PvpManager.resetForTest()
    }

    @AfterTest
    fun cleanup() {
        PvpManager.resetForTest()
    }

    private fun character(fights: Int = 5, stoneBroken: Boolean = true): KoLCharacter =
        KoLCharacter().also {
            it.updateFromApiResponse(
                CharacterApiResponse(
                    pvpfights = fights.toString(),
                    hippystone = if (stoneBroken) "1" else "0",
                ),
            )
        }

    private val fightPageHtml = """
        You have 7 fights remaining today.
        <select name="stance"><option value="0" >Bear Hugs All Around</option><option value="1" selected>Beary Famous</option></select>
    """.trimIndent()

    @Test
    fun visitFight_getsPlaceFightAndParsesStances() = runTest {
        val char = character(fights = 0, stoneBroken = false)
        var requested = ""
        val client = HttpClient(MockEngine { request ->
            requested = request.url.toString()
            respond(fightPageHtml, HttpStatusCode.OK)
        })
        val result = PeeVPeeRequest.visitFight(client, char)
        assertTrue(result.isSuccess)
        assertTrue(requested.contains("peevpee.php"))
        assertTrue(requested.contains("place=fight"))
        assertEquals(7, char.state.value.pvpFightsLeft)
        assertTrue(PvpManager.stancesKnown)
        assertEquals(1, PvpManager.findStance("Beary Famous"))
    }

    @Test
    fun smashStone_getsConfirmUrlAndSetsTenFights() = runTest {
        val char = character(fights = 0, stoneBroken = false)
        var requested = ""
        val client = HttpClient(MockEngine { request ->
            requested = request.url.toString()
            respond("You shatter your Magical Mystical Hippy Stone.", HttpStatusCode.OK)
        })
        val result = PeeVPeeRequest.smashStone(client, char)
        assertTrue(result.isSuccess)
        assertTrue(requested.contains("action=smashstone"))
        assertTrue(requested.contains("confirm=on"))
        assertEquals(10, char.state.value.pvpFightsLeft)
        assertTrue(char.state.value.hippyStoneBroken)
    }

    @Test
    fun fight_postsDesktopFormFieldsAndLogsStart() = runTest {
        val char = character(fights = 4, stoneBroken = true)
        val prefs = Preferences(MapSettings())
        prefs.setString("defaultFlowerWinMessage", "WINLINE")
        prefs.setString("defaultFlowerLossMessage", "LOSELINE")
        val logger = SessionLogger(prefs, GameEventBus())
        var method = ""
        var url = ""
        var body = ""
        val html = """
            You have 3 fights remaining today.
            <div class="fight"><a href="showplayer.php?who=1"><b>Hero</b></a> calls out <a href="showplayer.php?who=2"><b>Villain</b></a> for battle!
            <span class="win"><b>Hero</b> won the fight, <b>10</b> to <b>4</b>!
        """.trimIndent()
        val client = HttpClient(MockEngine { request ->
            method = request.method.value
            url = request.url.toString()
            body = request.body.toByteArray().decodeToString()
            respond(html, HttpStatusCode.OK)
        })
        PvpManager.parseStances(
            """<select name="stance"><option value="1" selected>Beary Famous</option></select>""",
        )
        val result = PeeVPeeRequest.fight(
            client = client,
            opponent = "",
            stance = 1,
            mission = "flowers",
            tougher = false,
            character = char,
            preferences = prefs,
            sessionLogger = logger,
        )
        assertTrue(result.isSuccess)
        assertEquals(HttpMethod.Post.value, method)
        assertTrue(url.contains("peevpee.php"))
        assertTrue(body.contains("action=fight"))
        assertTrue(body.contains("place=fight"))
        assertTrue(body.contains("attacktype=flowers"))
        assertTrue(body.contains("ranked=1"))
        assertTrue(body.contains("stance=1"))
        assertTrue(body.contains("who="))
        assertTrue(body.contains("winmessage=WINLINE"))
        assertTrue(body.contains("losemessage=LOSELINE"))
        assertEquals(3, char.state.value.pvpFightsLeft)
        assertTrue(
            logger.recentLines().any {
                it.contains("Attack a random opponent for flowers via Beary Famous")
            },
        )
    }

    @Test
    fun fight_tougherUsesRankedTwo() = runTest {
        var body = ""
        val client = HttpClient(MockEngine { request ->
            body = request.body.toByteArray().decodeToString()
            respond("You have 1 fight remaining today.", HttpStatusCode.OK)
        })
        PeeVPeeRequest.fight(
            client = client,
            opponent = "",
            stance = 0,
            mission = "fame",
            tougher = true,
            character = character(),
        )
        assertTrue(body.contains("ranked=2"))
        assertTrue(body.contains("attacktype=fame"))
    }

    @Test
    fun fight_directedUsesRankedZeroAndWho() = runTest {
        var body = ""
        val client = HttpClient(MockEngine { request ->
            body = request.body.toByteArray().decodeToString()
            respond("You have 1 fight remaining today.", HttpStatusCode.OK)
        })
        PeeVPeeRequest.fight(
            client = client,
            opponent = "Target",
            stance = 1,
            mission = "lootwhatever",
            tougher = false,
            character = character(),
            ranked = "0",
        )
        assertTrue(body.contains("ranked=0"))
        assertTrue(body.contains("who=Target"))
        assertTrue(body.contains("attacktype=lootwhatever"))
    }
}
