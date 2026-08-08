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
import net.sourceforge.kolmafia.data.HotDogAvailability
import net.sourceforge.kolmafia.data.SpeakeasyAvailability

class ClanLoungeRequestTest {

    @AfterTest
    fun tearDown() {
        ConcoctionDatabase.resetForTest()
        HotDogAvailability.resetForTest()
        SpeakeasyAvailability.resetForTest()
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
        val client = HttpClient(MockEngine { req ->
            url = req.url.toString()
            respond("ok")
        })
        ClanLoungeRequest(client).playPoolGame()
        assertTrue(url.contains("clan_viplounge.php"), "url=$url")
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
}
