package net.sourceforge.kolmafia.clan

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClanIdSyncTest {

    @AfterTest
    fun tearDown() {
        ClanManager.resetForTest()
    }

    @Test
    fun apply_parsesClanLinkFromCharpane() {
        val html = """
            Clan: <b><a class=nounder href="showclan.php?whichclan=12345">Awesome Clan</a></b>
        """.trimIndent()

        ClanIdSync.apply(html)

        assertEquals(12345, ClanManager.getClanId())
        assertEquals("Awesome Clan", ClanManager.getClanName())
    }

    @Test
    fun apply_clearsClanWhenNotInClan() {
        ClanManager.setClan(99, "Old Clan")

        ClanIdSync.apply("You aren't in a clan right now.")

        assertEquals(0, ClanManager.getClanId())
        assertEquals("", ClanManager.getClanName())
    }
}
