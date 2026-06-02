package net.sourceforge.kolmafia.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatParserTest {

    private val sampleJson = """
        {
          "msgs": [
            { "type": "public", "who": {"id":"123","name":"Tester"},
              "channel": "clan", "msg": "hello", "time": "1717300000" },
            { "type": "private", "who": {"id":"456","name":"Other"},
              "channel": "", "msg": "hey", "time": "1717300001" }
          ],
          "last": "1717300001",
          "delay": 3000
        }
    """.trimIndent()

    @Test
    fun parse_returnsCorrectMessageCount() {
        val result = ChatParser.parse(sampleJson)
        assertEquals(2, result.messages.size)
    }

    @Test
    fun parse_publicMessage_setsChannel() {
        val result = ChatParser.parse(sampleJson)
        val msg = result.messages[0]
        assertEquals("clan", msg.channel)
        assertEquals("Tester", msg.sender)
        assertEquals("hello", msg.content)
        assertNull(msg.recipient)
    }

    @Test
    fun parse_privateMessage_nullChannel() {
        val result = ChatParser.parse(sampleJson)
        val msg = result.messages[1]
        assertNull(msg.channel)
        assertEquals("Other", msg.sender)
    }

    @Test
    fun parse_setsLastTimeAndDelay() {
        val result = ChatParser.parse(sampleJson)
        assertEquals("1717300001", result.lastTime)
        assertEquals(3000L, result.delayMillis)
    }

    @Test
    fun parse_emptyMsgs_returnsEmpty() {
        val json = """{"msgs":[],"last":"0","delay":3000}"""
        val result = ChatParser.parse(json)
        assertTrue(result.messages.isEmpty())
        assertEquals("0", result.lastTime)
    }

    @Test
    fun parse_slashMeMsg_isAction() {
        val json = """
            {"msgs":[{"type":"public","who":{"id":"1","name":"A"},
            "channel":"clan","msg":"/me waves","time":"1"}],"last":"1","delay":3000}
        """.trimIndent()
        val result = ChatParser.parse(json)
        assertTrue(result.messages[0].isAction)
    }

    @Test
    fun parse_systemMsg_skipped() {
        val json = """
            {"msgs":[{"type":"system","msg":"[chat connected]","time":"1"}],
             "last":"1","delay":3000}
        """.trimIndent()
        val result = ChatParser.parse(json)
        assertTrue(result.messages.isEmpty())
    }
}
