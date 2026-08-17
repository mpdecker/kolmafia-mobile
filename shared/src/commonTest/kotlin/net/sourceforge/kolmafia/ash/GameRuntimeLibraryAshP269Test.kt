package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.chat.ChatManager
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP269Test {

    @Test
    fun revision_phase246() {
        assertEquals("phase550", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun chatNotify_headless_returnsVoid() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("", outputLib(lib, """chat_notify("hi", "green");""").trim())
    }

    @Test
    fun chatNotify_wiredManager_stripsColorQuotes() = runTest {
        val manager = ChatManager()
        val lib = GameRuntimeLibrary(chatManager = manager)
        outputLib(lib, """chat_notify("ping", "\"blue\"");""")
        val msg = manager.channelFlow(ChatManager.EVENTS_CHANNEL).first().single()
        assertEquals("ping", msg.content)
        assertEquals("blue", msg.color)
        assertEquals("", msg.sender)
    }
}
