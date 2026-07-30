package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConsumableDatabaseNonFillingTest {

    @AfterTest
    fun tearDown() {
        ConsumableDatabase.resetForTest()
    }

    @Test
    fun loadsBatteryAaaWithLevelReqZero() = runBlocking {
        ConsumableDatabase.load()
        val entry = ConsumableDatabase.getNonFilling("battery (AAA)")
        assertNotNull(entry)
        assertEquals(0, entry.levelReq)
        assertEquals(ConsumableType.NONFILLING, entry.type)
        assertEquals(0, entry.amount)
        assertEquals(ConsumableQuality.NONE, entry.quality)
    }

    @Test
    fun loadsBrickoElephantWithLevelReqThree() = runBlocking {
        ConsumableDatabase.load()
        val entry = ConsumableDatabase.getNonFilling("BRICKO elephant")
        assertNotNull(entry)
        assertEquals(3, entry.levelReq)
    }

    @Test
    fun preservesUsageNotes() = runBlocking {
        ConsumableDatabase.load()
        val entry = ConsumableDatabase.getNonFilling("battery (AAA)")
        assertNotNull(entry)
        assertTrue(entry.notes.contains("Energy"))
    }

    @Test
    fun getFullnessByName_returnsZeroForNonFillingItem() = runBlocking {
        ConsumableDatabase.load()
        assertEquals(0, ConsumableDatabase.getFullnessByName("battery (AAA)"))
    }

    @Test
    fun getLevelReqByName_findsNonFillingItemNotInFullnessTxt() = runBlocking {
        ConsumableDatabase.load()
        assertNull(ConsumableDatabase.getFood("mime army challenge coin"))
        assertEquals(13, ConsumableDatabase.getLevelReqByName("mime army challenge coin"))
    }

    @Test
    fun applyNonFillingParse_skipsCommentsAndVersionLine() {
        val fixture = """
            1
            # Item	Level req	Usage note (optional)
            battery (AAA)	0	Grants 15 Energy
            #IocaineBot	792443	http://example.com
        """.trimIndent()
        ConsumableDatabase.applyNonFillingParse(fixture)
        assertNotNull(ConsumableDatabase.getNonFilling("battery (AAA)"))
        assertNull(ConsumableDatabase.getNonFilling("IocaineBot"))
    }
}
