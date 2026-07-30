package net.sourceforge.kolmafia.faxbot

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlin.test.AfterTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class FaxBotDatabaseTest {

    @AfterTest
    fun tearDown() {
        FaxBotDatabase.instance.clearForTest()
    }

    @Test
    fun fetchConfigForEntry_registersBotsFromXml() = kotlinx.coroutines.test.runTest {
        val xml = """
            <faxbot>
                <botdata><name>onlyfax</name><playerid>1</playerid></botdata>
                <monsterdata>
                    <name>rockfish</name><actual_name>rockfish</actual_name>
                    <command>rockfish</command><category>fish</category>
                </monsterdata>
            </faxbot>
        """.trimIndent()
        val db = FaxBotDatabase.instance
        db.clearForTest()
        db.fetchConfigForEntry(
            HttpClient(MockEngine { respond(xml, HttpStatusCode.OK) }),
            FaxBotEntry("onlyfax", "http://example.com/onlyfax.xml"),
            null,
        )
        assertEquals(1, db.allBots().size)
        assertEquals("onlyfax", db.allBots().first().name)
    }

    @Test
    fun canFaxbot_matchesMonsterInRegisteredBot() {
        val db = FaxBotDatabase.instance
        val bot = FaxBot("onlyfax", 1)
        bot.addMonsters(
            listOf(
                FaxBotMonster("rockfish", "rockfish", "rockfish", "fish", 848),
            ),
        )
        db.registerBotForTest(bot)

        assertTrue(db.canFaxbot(848))
        assertTrue(db.canFaxbot(848, "onlyfax"))
        assertFalse(db.canFaxbot(848, "missing"))
        assertFalse(db.canFaxbot(999))
    }

    @Test
    fun getSortedFaxbots_prefersLastSuccessful() {
        val db = FaxBotDatabase.instance
        val first = FaxBot("easyfax", 1)
        val second = FaxBot("onlyfax", 2)
        db.registerBotForTest(first)
        db.registerBotForTest(second)

        val prefs = Preferences(MapSettings())
        prefs.setString(FaxBotDatabase.PREF_LAST_SUCCESSFUL, "onlyfax")
        val sorted = db.getSortedFaxbots(prefs)

        assertEquals("onlyfax", sorted.first().name)
    }

    @Test
    fun findMatchingCommands_resolvesPartialCommand() {
        val bot = FaxBot("onlyfax", 1)
        bot.addMonsters(
            listOf(
                FaxBotMonster("rockfish", "rockfish", "rockfish", "fish", 848),
            ),
        )
        val matches = bot.findMatchingCommands("rock")
        assertEquals(listOf("rockfish"), matches)
    }
}
