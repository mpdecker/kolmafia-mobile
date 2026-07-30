package net.sourceforge.kolmafia.buffbot

import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuffBotDatabaseTest {

    @AfterTest
    fun tearDown() {
        BuffBotDatabase.instance.clearRegistryForTest()
    }

    @Test
    fun loadsTestudinataAndSevenLances() {
        runBlocking {
            BuffBotDatabase.load()
            assertTrue(BuffBotDatabase.instance.isKnownBot("Testudinata"))
            assertTrue(BuffBotDatabase.instance.isKnownBot("SevenLances_com"))
            assertEquals("537858", BuffBotDatabase.instance.findBot("Testudinata")?.playerId)
        }
    }

    @Test
    fun skipsCommentedBots() {
        runBlocking {
            BuffBotDatabase.load()
            assertFalse(BuffBotDatabase.instance.isKnownBot("IocaineBot"))
        }
    }

    @Test
    fun isOptedOut_falseForActiveBots() {
        runBlocking {
            BuffBotDatabase.load()
            assertFalse(BuffBotDatabase.instance.isOptedOut("Testudinata"))
        }
    }

    @Test
    fun isOptedOut_trueWhenXmlUrlIsOptOutConstant() {
        val db = BuffBotDatabase.forTest(
            bots = listOf(
                BuffBotEntry("OptOutBot", "123", BuffBotDatabase.OPTOUT_URL),
            ),
        )
        assertTrue(db.isOptedOut("OptOutBot"))
    }

    @Test
    fun findBot_isCaseInsensitive() {
        runBlocking {
            BuffBotDatabase.load()
            assertNotNull(BuffBotDatabase.instance.findBot("testudinata"))
            assertNotNull(BuffBotDatabase.instance.findBot("TESTUDINATA"))
        }
    }

    @Test
    fun applyRegistryParse_skipsVersionAndComments() {
        val db = BuffBotDatabase.forTest()
        db.applyRegistryParse(
            """
            1
            # Bot	Player ID	XML File URL
            OakBot	123456	http://example.com/oak.xml
            #IocaineBot	792443	http://example.com/iocaine.xml
            """.trimIndent(),
        )
        assertNotNull(db.findBot("OakBot"))
        assertNull(db.findBot("IocaineBot"))
    }
}
