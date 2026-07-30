package net.sourceforge.kolmafia.faxbot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import kotlinx.coroutines.runBlocking

class FaxBotXmlParserTest {

    private val sampleXml = """
        <faxbot>
            <botdata>
                <name>onlyfax</name>
                <playerid>12345</playerid>
            </botdata>
            <monsterdata>
                <name>rockfish</name>
                <actual_name>rockfish</actual_name>
                <command>rockfish</command>
                <category>fish</category>
            </monsterdata>
            <monsterdata>
                <name>none</name>
                <actual_name>none</actual_name>
                <command>none</command>
                <category>none</category>
            </monsterdata>
            <monsterdata>
                <name>id monster</name>
                <actual_name>rockfish</actual_name>
                <command>rockfish [848]</command>
                <category>fish</category>
            </monsterdata>
        </faxbot>
    """.trimIndent()

    @Test
    fun parse_botAndMonsters() = runBlocking {
        val db = GameDatabase()
        db.load()
        val parsed = FaxBotXmlParser.parse(sampleXml, db)

        assertEquals(1, parsed.bots.size)
        assertEquals("onlyfax", parsed.bots[0].name)
        assertEquals(12345, parsed.bots[0].playerId)
        assertTrue(parsed.monsters.size >= 2)
        assertEquals(848, parsed.bots[0].getMonsterByCommand("rockfish")?.monsterId)
    }
}
