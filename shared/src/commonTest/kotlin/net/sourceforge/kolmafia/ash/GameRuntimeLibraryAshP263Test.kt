package net.sourceforge.kolmafia.ash

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.buffbot.BuffBotDatabase
import net.sourceforge.kolmafia.buffbot.BuffBotManager
import net.sourceforge.kolmafia.buffbot.BuffCost
import net.sourceforge.kolmafia.chat.ChatSender

class GameRuntimeLibraryAshP263Test {

    private fun testLib(): GameRuntimeLibrary {
        val db = BuffBotDatabase.forTest(
            costs = listOf(
                BuffCost(buffId = 3004, buffName = "Empathy of the Newt", meatCost = 100L, turns = 10),
            ),
        )
        val sender = ChatSender(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }))
        return GameRuntimeLibrary(
            buffBotManager = BuffBotManager(sender, db),
            buffBotDatabase = db,
        )
    }

    @Test
    fun revision_phase245() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun requestBuff_bySkillId_returnsTrue() {
        val lib = testLib()
        assertEquals("true", outputLib(lib, """print(request_buff("OakBot", 3004));""").trim())
    }

    @Test
    fun requestBuff_bySkillIdAndTurns_returnsTrue() {
        val lib = testLib()
        assertEquals("true", outputLib(lib, """print(request_buff("OakBot", 3004, 5));""").trim())
    }

    @Test
    fun requestBuff_unknownSkillId_returnsFalse() {
        val lib = testLib()
        assertEquals("false", outputLib(lib, """print(request_buff("OakBot", 9999));""").trim())
    }

    @Test
    fun requestBuff_optedOutBot_returnsFalse() {
        val db = BuffBotDatabase.forTest(
            costs = listOf(
                BuffCost(buffId = 3004, buffName = "Empathy of the Newt", meatCost = 100L, turns = 10),
            ),
            bots = listOf(
                net.sourceforge.kolmafia.buffbot.BuffBotEntry(
                    "OptOutBot",
                    "123",
                    BuffBotDatabase.OPTOUT_URL,
                ),
            ),
        )
        val sender = ChatSender(HttpClient(MockEngine { respond("{}", HttpStatusCode.OK) }))
        val lib = GameRuntimeLibrary(
            buffBotManager = BuffBotManager(sender, db),
            buffBotDatabase = db,
        )
        assertEquals("false", outputLib(lib, """print(request_buff("OptOutBot", 3004));""").trim())
    }

    @Test
    fun requestBuff_withoutManager_returnsFalse() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, """print(request_buff("OakBot", 3004));""").trim())
    }
}
