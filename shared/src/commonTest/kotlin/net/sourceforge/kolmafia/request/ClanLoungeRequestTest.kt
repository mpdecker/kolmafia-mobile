package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionRefreshContext
import net.sourceforge.kolmafia.data.FloundryAvailability
import net.sourceforge.kolmafia.data.HotDogAvailability
import net.sourceforge.kolmafia.data.SpeakeasyAvailability
import net.sourceforge.kolmafia.preferences.Preferences
import com.russhwolf.settings.MapSettings

class ClanLoungeRequestTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        HotDogAvailability.resetForTest()
        SpeakeasyAvailability.resetForTest()
        FloundryAvailability.resetForTest()
    }

    @Test fun useKlaw_success_returnsBody() = runTest {
        val client = HttpClient(MockEngine { respond("klaw body") })
        val result = ClanLoungeRequest(client).useKlaw()
        assertTrue(result.isSuccess)
        assertEquals("klaw body", result.getOrNull())
    }

    @Test fun useKlaw_serverError_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("err", HttpStatusCode.InternalServerError) })
        assertTrue(ClanLoungeRequest(client).useKlaw().isFailure)
    }

    @Test fun useKlaw_sendsKlawAction() = runTest {
        var url = ""
        val client = HttpClient(MockEngine { req ->
            url = req.url.toString()
            respond("klaw body")
        })
        ClanLoungeRequest(client).useKlaw()
        assertTrue(url.contains("clan_viplounge.php"), "url=$url")
    }

    @Test fun useLookingGlass_sendsCorrectAction() = runTest {
        var url = ""
        val client = HttpClient(MockEngine { req ->
            url = req.url.toString()
            respond("ok")
        })
        ClanLoungeRequest(client).useLookingGlass()
        assertTrue(url.contains("clan_viplounge.php"), "url=$url")
    }

    @Test fun visitFireworks_sendsCorrectAction() = runTest {
        var url = ""
        val client = HttpClient(MockEngine { req ->
            url = req.url.toString()
            respond("ok")
        })
        ClanLoungeRequest(client).visitFireworks()
        assertTrue(url.contains("clan_viplounge.php"), "url=$url")
    }

    @Test fun playPoolGame_sendsCorrectFormParams() = runTest {
        var url = ""
        var body = ""
        val client = HttpClient(MockEngine { req ->
            url = req.url.toString()
            body = req.body.toByteArray().decodeToString()
            respond("You take control of the table.")
        })
        val prefs = Preferences(MapSettings())
        ClanLoungeRequest(client).playPoolGame(stance = 2, preferences = prefs)
        assertTrue(url.contains("clan_viplounge.php"), "url=$url")
        assertTrue(body.contains("preaction=poolgame"), "body=$body")
        assertTrue(body.contains("stance=2"), "body=$body")
        assertTrue(body.contains("whichfloor=2"), "body=$body")
        assertEquals(1, prefs.getInt("_poolGames", 0))
    }

    @Test fun useKlaw_networkError_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { throw Exception("net") })
        assertTrue(ClanLoungeRequest(client).useKlaw().isFailure)
    }

    @Test fun visitHotDogStand_sendsHotdogstandAction() = runTest {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("stand html")
        })
        val result = ClanLoungeRequest(client).visitHotDogStand()
        assertTrue(result.isSuccess)
        assertTrue(bodies.single().contains("action=hotdogstand"), "body=${bodies.single()}")
    }

    @Test fun visitSpeakeasy_sendsSpeakeasyActionAndFloor() = runTest {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("speakeasy html")
        })
        val result = ClanLoungeRequest(client).visitSpeakeasy()
        assertTrue(result.isSuccess)
        assertTrue(bodies.single().contains("action=speakeasy"), "body=${bodies.single()}")
        assertTrue(bodies.single().contains("whichfloor=2"), "body=${bodies.single()}")
    }

    @Test fun visitFloundry_sendsFloundryAction() = runTest {
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("""<br>100 carp""")
        })
        val result = ClanLoungeRequest(client).visitFloundry(preferences = prefs)
        assertTrue(result.isSuccess)
        assertTrue(bodies.single().contains("action=floundry"), "body=${bodies.single()}")
        assertTrue(FloundryAvailability.isAvailable("carpe"))
    }

    @Test fun eatHotDog_sendsEathotdogForm() = runTest {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("You gain some stats.")
        })
        HotDogAvailability.addForTest("basic hot dog")
        ConcoctionDatabase.refreshConcoctionsNow(ConcoctionRefreshContext.EMPTY)
        val result = ClanLoungeRequest(client).eatHotDog(-92)
        assertTrue(result.isSuccess)
        assertTrue(bodies.single().contains("preaction=eathotdog"), "body=${bodies.single()}")
        assertTrue(bodies.single().contains("whichdog=-92"), "body=${bodies.single()}")
    }

    @Test fun drinkSpeakeasy_sendsSpeakeasydrinkForm() = runTest {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { req ->
            bodies += req.body.toByteArray().decodeToString()
            respond("You drink a Lucky Lindy.")
        })
        SpeakeasyAvailability.addLoungeId(4)
        ConcoctionDatabase.refreshConcoctionsNow(
            ConcoctionRefreshContext(characterState = CharacterState(meat = 500)),
        )
        val result = ClanLoungeRequest(client).drinkSpeakeasy(4)
        assertTrue(result.isSuccess)
        assertTrue(bodies.single().contains("preaction=speakeasydrink"), "body=${bodies.single()}")
        assertTrue(bodies.single().contains("drink=4"), "body=${bodies.single()}")
        assertTrue(bodies.single().contains("whichfloor=2"), "body=${bodies.single()}")
    }

    @Test fun eatHotDog_unavailableFailsBeforeHttp() = runTest {
        var called = false
        val client = HttpClient(MockEngine {
            called = true
            respond("ok")
        })
        val result = ClanLoungeRequest(client).eatHotDog("basic hot dog")
        assertTrue(result.isFailure)
        assertFalse(called)
    }

    @Test fun visitHotDogStand_serverError_returnsFailure() = runTest {
        val client = HttpClient(MockEngine { respond("err", HttpStatusCode.InternalServerError) })
        assertTrue(ClanLoungeRequest(client).visitHotDogStand().isFailure)
    }

    @Test fun useHotTub_sendsHottubAction() = runTest {
        var url = ""
        val client = HttpClient(MockEngine { req ->
            url = req.url.toString()
            respond("tub html")
        })
        val result = ClanLoungeRequest(client).useHotTub()
        assertTrue(result.isSuccess)
        assertTrue(url.contains("clan_viplounge.php"), "url=$url")
    }

    @Test fun useHotTub_parsesHotTubSoaksPref() = runTest {
        val prefs = net.sourceforge.kolmafia.preferences.Preferences(com.russhwolf.settings.MapSettings())
        val client = HttpClient(MockEngine { respond("""<img src="hottub3.gif">""") })
        ClanLoungeRequest(client).useHotTub(preferences = prefs)
        assertEquals(2, prefs.getInt(net.sourceforge.kolmafia.clan.ClanLoungeSync.HOT_TUB_SOAKS_PREF, -1))
    }
}
