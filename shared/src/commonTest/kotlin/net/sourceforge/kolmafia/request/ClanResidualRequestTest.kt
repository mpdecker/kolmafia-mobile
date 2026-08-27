package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.clan.ClanManager
import net.sourceforge.kolmafia.preferences.Preferences

class ClanResidualRequestTest {
    @AfterTest
    fun tearDown() {
        ClanManager.resetForTest()
        ClanWarRequest.resetForTest()
        ClanStashSync.resetForTest()
    }

    @Test
    fun loungeSearchParsesFurnitureAndDailyImages() = runTest {
        ClanManager.setClan(42, "Test Clan")
        val prefs = Preferences(MapSettings())
        val html = """
            <table><b style="color: white">Clan VIP Lounge (Attic)</b>
            <center><b>Test Clan</b></center><table>
            <img src="lookingglass.gif"><img src="tree5.gif"><img src="hottub2.gif">
            </table>
        """.trimIndent()
        val request = ClanLoungeRequest(HttpClient(MockEngine { respond(html) }))

        assertTrue(request.searchLounge(2, prefs).isSuccess)
        assertTrue(ClanManager.getClanLounge().any { it.first == "Looking Glass" })
        assertEquals(3, prefs.getInt("_hotTubSoaks"))
        assertTrue(prefs.getBoolean("_crimboTree"))
    }

    @Test
    fun rumpusFurnitureAndBallCountPopulateCache() {
        ClanManager.setClan(42, "Test Clan")
        ClanRumpusRequest.parseResponse(
            "clan_rumpus.php",
            """action=click&spot=3&furni=2 action=click&spot=7&furni=1 with 1,234 balls""",
        )
        assertEquals(listOf("Jukebox", "Awesome Ball Pit", "Awesome Ball Pit (1,234)"),
            ClanManager.getClanRumpus())
        assertEquals("Comfy Sofa", ClanRumpusFurniture.Equipment.equipmentName(5, 3))
    }

    @Test
    fun membersRanksAndHallUpdateManager() {
        ClanManager.setClan(42, "Test Clan")
        ClanMembersRequest.parseResponse(
            "showclan.php",
            """<a class=nounder href="showplayer.php?who=123">Alice</a></b>&nbsp;</td><td class=small>Boss&nbsp;</td><td class=small>13</td>""",
        )
        ClanMembersRequest.parseResponse(
            "clan_members.php",
            """<select name=level><option>Leader</option><option>Member</option></select>""",
        )
        assertTrue(ClanManager.isCurrentMember("Alice"))
        assertEquals("Boss", ClanManager.getTitle("Alice"))
        assertEquals(listOf("leader", "member"), ClanManager.getRankList())

        ClanHallRequest.parseResponse("clan_hall.php", "<center><b>Another Clan</b>")
        assertEquals(0, ClanManager.getClanId())
    }

    @Test
    fun stashAndLogPopulateManagerCaches() {
        ClanManager.setClan(42, "Test Clan")
        ClanStashRequest.storeContents(mapOf(11 to 3))
        assertEquals(3, ClanManager.getStash().single().quantity)
        assertEquals(3, ClanStashSync.stashCounts?.get(11))

        ClanLogRequest.parseResponse(
            "clan_log.php",
            """08/27/26, 09:10AM: <a class=nounder href='showplayer.php?who=1'>Alice (#1)</a> added 2 seal-clubbing clubs.<br>""" +
                """08/27/26, 09:11AM: Bob launched an attack against Enemy Clan.<br>""",
        )
        assertEquals(2, ClanManager.getStashLog().size)
    }

    @Test
    fun warAndBuffRequestsMirrorDesktopForms() = runTest {
        val prefs = Preferences(MapSettings())
        ClanWarRequest.parseResponse(
            "clan_attack.php",
            "name=whichclan value=7></td><td><b>Enemy</td><td>1</td>",
            prefs,
        )
        assertEquals("Enemy", ClanWarRequest.enemyClans.single().name)
        assertTrue(prefs.getBoolean("clanAttacksEnabled"))

        var body = ""
        val client = HttpClient(MockEngine { request ->
            body = request.body.toByteArray().decodeToString()
            respond("ok")
        })
        assertTrue(ClanBuffRequest(client).buyBuff(23).isSuccess)
        assertTrue(body.contains("size=3"))
        assertTrue(body.contains("whichgift=2"))
        assertEquals(25, ClanBuffRequest.requestList.size)

        ClanWarRequest.parseResponse("clan_war.php", "not enabled", prefs)
        assertFalse(prefs.getBoolean("clanAttacksEnabled"))
    }
}
