package net.sourceforge.kolmafia.chat

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ChatManagerTest {

    private fun msg(
        sender: String = "A",
        channel: String? = "clan",
        recipient: String? = null,
        content: String = "hi",
        epochSeconds: Long = 1L
    ) = ChatMessage(
        sender = sender, senderId = "1", recipient = recipient,
        channel = channel, content = content, isAction = false, epochSeconds = epochSeconds
    )

    @Test
    fun dispatch_publicMessage_appearsInChannelFlow() = runTest {
        val manager = ChatManager()
        manager.dispatch(listOf(msg(sender = "Alice", channel = "clan", content = "hi")))

        val messages = manager.channelFlow("clan").first()
        assertEquals(1, messages.size)
        assertEquals("Alice", messages[0].sender)
    }

    @Test
    fun dispatch_privateMessage_appearsInPmFlow() = runTest {
        val manager = ChatManager()
        manager.dispatch(listOf(msg(sender = "Bob", channel = null, recipient = "Bob")))

        val messages = manager.pmFlow("Bob").first()
        assertEquals(1, messages.size)
        assertEquals("Bob", messages[0].sender)
    }

    @Test
    fun dispatch_updatesKnownChannels() = runTest {
        val manager = ChatManager()
        manager.dispatch(listOf(msg(channel = "trade")))

        assertTrue("trade" in manager.knownChannels.value)
    }

    @Test
    fun dispatch_multipleMessages_preservesOrder() = runTest {
        val manager = ChatManager()
        val msgs = (1..3).map { i ->
            msg(sender = "S$i", channel = "clan", content = "msg$i", epochSeconds = i.toLong())
        }

        manager.dispatch(msgs)

        val stored = manager.channelFlow("clan").first()
        assertEquals(listOf("msg1", "msg2", "msg3"), stored.map { it.content })
    }

    @Test
    fun dispatch_twoChannels_isolated() = runTest {
        val manager = ChatManager()
        manager.dispatch(listOf(msg(channel = "clan", content = "in clan")))
        manager.dispatch(listOf(msg(channel = "trade", content = "in trade")))

        assertEquals(1, manager.channelFlow("clan").first().size)
        assertEquals("in clan", manager.channelFlow("clan").first()[0].content)
        assertEquals(1, manager.channelFlow("trade").first().size)
    }

    @Test
    fun channelFlow_beforeAnyMessages_returnsEmpty() = runTest {
        val manager = ChatManager()
        val messages = manager.channelFlow("clan").first()
        assertTrue(messages.isEmpty())
    }

    @Test
    fun pmFlow_beforeAnyMessages_returnsEmpty() = runTest {
        val manager = ChatManager()
        val messages = manager.pmFlow("Alice").first()
        assertTrue(messages.isEmpty())
    }
}
