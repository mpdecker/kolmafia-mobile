package net.sourceforge.kolmafia.buffbot

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.chat.ChatSender
import kotlin.test.*

class BuffBotManagerTest {

    private fun makeSenderCapturing(): Pair<ChatSender, MutableList<String>> {
        val sent = mutableListOf<String>()
        val engine = MockEngine { request ->
            sent += request.body.toByteArray().decodeToString()
            respond("{}", HttpStatusCode.OK)
        }
        return ChatSender(HttpClient(engine)) to sent
    }

    @Test
    fun requestBuff_sendsCorrectPm() = runTest {
        val (sender, sent) = makeSenderCapturing()
        val db = BuffBotDatabase(listOf(
            BuffCost(buffId = 3004, buffName = "Empathy of the Newt", meatCost = 100L, turns = 10)
        ))
        val mgr = BuffBotManager(chatSender = sender, database = db)

        mgr.requestBuff(botName = "OakBot", buffId = 3004, turns = 10)

        assertEquals(1, sent.size)
        val body = sent[0]
        // Should have /msg OakBot 3004 10 in the POST body
        assertTrue(body.contains("OakBot"), "body: $body")
        assertTrue(body.contains("3004"), "body: $body")
        assertTrue(body.contains("10"), "body: $body")
    }

    @Test
    fun requestBuff_unknownBuffId_returnsFailure() = runTest {
        val (sender, _) = makeSenderCapturing()
        val mgr = BuffBotManager(chatSender = sender, database = BuffBotDatabase(emptyList()))

        val result = mgr.requestBuff(botName = "OakBot", buffId = 9999, turns = 5)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("9999") == true)
    }

    @Test
    fun requestBuff_success_returnsSuccess() = runTest {
        val (sender, _) = makeSenderCapturing()
        val db = BuffBotDatabase(listOf(
            BuffCost(buffId = 3004, buffName = "Empathy of the Newt", meatCost = 100L, turns = 10)
        ))
        val result = BuffBotManager(sender, db).requestBuff("OakBot", 3004, 10)

        assertTrue(result.isSuccess)
    }
}
