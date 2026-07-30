package net.sourceforge.kolmafia.chat

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatHtmlParserTest {

    @AfterTest
    fun tearDown() {
        PlayerIdRegistry.clearForTest()
    }

    @Test
    fun parsePlayerIds_registersShowPlayerLinks() {
        val html = """
            <a href="showplayer.php?who=685853">Light Ninja</a>
            <a href='showplayer.php?who=189466'>scullyangel</a>
        """.trimIndent()
        ChatHtmlParser.parsePlayerIds(html)
        assertEquals("685853", PlayerIdRegistry.getPlayerId("Light Ninja"))
        assertEquals("189466", PlayerIdRegistry.getPlayerId("scullyangel"))
    }

    @Test
    fun parseWhoClan_mapsOnlineStatusByColor() {
        val html = """
            <a href="showplayer.php?who=1"><font color='black'>Online Player</font></a>
            <a href="showplayer.php?who=2"><font color='gray'>Away Player</font></a>
            <a href="showplayer.php?who=3"><font color='blue'>Listening Player</font></a>
        """.trimIndent()
        val contacts = ChatHtmlParser.parseWhoClan(html)
        assertEquals(true, contacts["Online Player"])
        assertEquals(false, contacts["Away Player"])
        assertEquals(true, contacts["Listening Player"])
    }

    @Test
    fun cleanPlayerName_stripsTagsAndParenthetical() {
        assertEquals(
            "Player One",
            ChatHtmlParser.cleanPlayerName("<b>Player One (AFK)</b>"),
        )
    }

    @Test
    fun parseWhoClan_emptyHtml_returnsEmptyMap() {
        assertTrue(ChatHtmlParser.parseWhoClan("").isEmpty())
    }
}
