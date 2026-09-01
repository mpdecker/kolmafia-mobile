package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.PandamoniumRequest
import net.sourceforge.kolmafia.request.SendMailRequest
import net.sourceforge.kolmafia.request.SpadeRequest
import net.sourceforge.kolmafia.session.ClanCliManager
import net.sourceforge.kolmafia.session.DadManager
import net.sourceforge.kolmafia.session.SlimeStackManager

class CliTier4ParityTest {
    @AfterTest
    fun resetDad() {
        DadManager.elementalWeakness.indices.forEach { DadManager.elementalWeakness[it] = DadManager.Element.NONE }
    }

    @Test
    fun dadReportsAllRounds() {
        DadManager.elementalWeakness[1] = DadManager.Element.HOT
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("dad");""")
        assertTrue(out.contains("Round 1: hot"))
        assertTrue(out.contains("Round 10: none"))
    }

    @Test
    fun slimeStackReportsQueuedNextCombat() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(SlimeStackManager.STACKS_DROPPED_PREF, 2)
        prefs.setInt(SlimeStackManager.STACKS_DUE_PREF, 5)
        val out = outputLib(GameRuntimeLibrary(preferences = prefs), """cli_execute("slime-stack");""")
        assertTrue(out.contains("3 slime stacks queued"))
        assertTrue(out.contains("Next: #3"))
        assertTrue(out.contains("6 total Slimeling combats"))
    }

    @Test
    fun clanStatusIsReadOnlyWithoutRequests() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("clan status");""")
        assertTrue(out.contains("Clan:"))
        assertTrue(out.contains("Members:"))
    }

    @Test
    fun helpAdvertisesTier4CommandsAndStickerAlias() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help");""")
        for (command in listOf("clan", "dad", "slime-stack", "spade", "sticker", "tcrs")) {
            assertTrue(out.contains(command), "missing $command: $out")
        }
    }

    @Test
    fun typedCommandHelpersValidateInputs() {
        assertEquals("Flargwurm", PandamoniumRequest.bandMember("bassist"))
        assertEquals("observe", PandamoniumRequest.comedyType("OBSERVE"))
        val prefs = Preferences(MapSettings())
        prefs.setString("spadingData", "payload|bot|reason|malformed")
        val request = SpadeRequest(
            sendMailRequest = object : SendMailRequest(
                HttpClient(MockEngine { respond("") }),
            ) {},
            preferences = prefs,
        )
        assertEquals(1, request.pending().size)
        assertEquals("payload", request.pending().single().contents)
        assertEquals(4, ClanCliManager().statusLines().size)
    }

    @Test
    fun spadeSubmitSendsLiveKmailAndClearsOnlySuccessfulData() = runTest {
        val prefs = Preferences(MapSettings())
        prefs.setString("spadingData", "payload|spadebot|for testing")
        val sent = mutableListOf<Pair<String, String>>()
        val request = SpadeRequest(
            sendMailRequest = object : SendMailRequest(HttpClient(MockEngine { respond("") })) {
                override suspend fun send(recipient: String, message: String): Result<Unit> {
                    sent += recipient to message
                    return Result.success(Unit)
                }
            },
            preferences = prefs,
        )
        val result = request.submit().getOrThrow()
        assertEquals(1, result.sent)
        assertTrue(sent.single() == ("spadebot" to "payload"))
        assertEquals("", prefs.getString("spadingData", "not-cleared"))
    }

    @Test
    fun pandamoniumGivePostsValidatedForm() = runTest {
        val bodies = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            bodies += request.body.toByteArray().decodeToString()
            respond("Sven accepts the gift.", HttpStatusCode.OK)
        })
        val request = PandamoniumRequest(client)
        assertTrue(request.give("bassist", 4673).isSuccess)
        assertTrue(bodies.single().contains("action=sven"))
        assertTrue(bodies.single().contains("bandmember=Flargwurm"))
        assertTrue(bodies.single().contains("togive=4673"))
    }
}
