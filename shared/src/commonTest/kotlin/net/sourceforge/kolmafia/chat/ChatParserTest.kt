package net.sourceforge.kolmafia.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatParserTest {

    private val sampleJson = """
        {
          "msgs": [
            { "type": "public", "who": {"id":"123","name":"Tester"},
              "channel": "clan", "msg": "hello", "time": "1717300000" },
            { "type": "private", "who": {"id":"456","name":"Other"},
              "for": {"id":"789","name":"Me"},
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
    fun parse_setsEpochSeconds() {
        val result = ChatParser.parse(sampleJson)
        assertEquals(1717300000L, result.messages[0].epochSeconds)
    }

    @Test
    fun parse_emptyMsgs_returnsEmpty() {
        val json = """{"msgs":[],"last":"0","delay":3000}"""
        val result = ChatParser.parse(json)
        assertTrue(result.messages.isEmpty())
        assertEquals("0", result.lastTime)
    }

    @Test
    fun parse_formatField1_isAction() {
        val json = """
            {"msgs":[{"type":"public","who":{"id":"1","name":"A"},
            "channel":"clan","msg":"waves","format":"1","time":"1"}],"last":"1","delay":3000}
        """.trimIndent()
        val result = ChatParser.parse(json)
        assertTrue(result.messages[0].isAction)
    }

    @Test
    fun parse_noFormatField_notAction() {
        val json = """
            {"msgs":[{"type":"public","who":{"id":"1","name":"A"},
            "channel":"clan","msg":"hello","time":"1"}],"last":"1","delay":3000}
        """.trimIndent()
        val result = ChatParser.parse(json)
        assertFalse(result.messages[0].isAction)
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

    @Test
    fun parse_eventMsg_skipped() {
        val json = """
            {"msgs":[{"type":"event","msg":"something happened","time":"1"}],
             "last":"1","delay":3000}
        """.trimIndent()
        val result = ChatParser.parse(json)
        assertTrue(result.messages.isEmpty())
    }

    @Test
    fun parse_malformedJson_returnsEmpty() {
        val result = ChatParser.parse("not valid json{{")
        assertTrue(result.messages.isEmpty())
        assertEquals("0", result.lastTime)
        assertEquals(3000L, result.delayMillis)
    }

    @Test
    fun parse_privateMessage_setsRecipientFromForField() {
        val json = """
            {"msgs":[{"type":"private","who":{"id":"1","name":"Alice"},
            "for":{"id":"2","name":"Bob"},"channel":"","msg":"hey","time":"1"}],
            "last":"1","delay":3000}
        """.trimIndent()
        val result = ChatParser.parse(json)
        assertEquals("Bob", result.messages[0].recipient)
        assertEquals("Alice", result.messages[0].sender)
    }

    @Test
    fun parse_malformedMessageElement_skipsItKeepsOthers() {
        // The middle element is a JSON array (not an object), which causes .jsonObject to throw.
        // The parser should skip it via runCatching and keep the two valid messages.
        val json = """
            {"msgs":[
              {"type":"public","who":{"id":"1","name":"A"},"channel":"clan","msg":"good","time":"1000"},
              [1,2,3],
              {"type":"public","who":{"id":"2","name":"B"},"channel":"clan","msg":"also good","time":"2000"}
            ],"last":"2000","delay":3000}
        """.trimIndent()
        val result = ChatParser.parse(json)
        assertEquals(2, result.messages.size)
        assertEquals("A", result.messages[0].sender)
        assertEquals("B", result.messages[1].sender)
    }
}
