package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class ConsumableDatabaseBracketLookupTest {

    @BeforeTest
    fun setUp() = runTest {
        ConsumableDatabase.load()
    }

    @AfterTest
    fun tearDown() {
        ConsumableDatabase.resetForTest()
    }

    @Test
    fun getInebrietyByName_drink() {
        assertEquals(6, ConsumableDatabase.getInebrietyByName("Lucky Lindy"))
    }

    @Test
    fun getInebrietyByName_nonDrink_returnsZero() {
        assertEquals(0, ConsumableDatabase.getInebrietyByName("acceptable bagel"))
    }

    @Test
    fun getSpleenByName_candy() {
        assertEquals(1, ConsumableDatabase.getSpleenByName("a bug's lymph"))
    }

    @Test
    fun getQualityName_food() {
        assertEquals("good", ConsumableDatabase.getQualityName("acceptable bagel"))
    }

    @Test
    fun getQualityName_nonfilling_returnsEmpty() {
        assertEquals("", ConsumableDatabase.getQualityName("battery (AAA)"))
    }

    @Test
    fun getAdventureRange_rangeFormat() {
        assertEquals("8-9", ConsumableDatabase.getAdventureRange("acceptable bagel"))
        assertEquals("16-20", ConsumableDatabase.getAdventureRange("Lucky Lindy"))
    }

    @Test
    fun getAdventureRange_nonConsumable_returnsZero() {
        assertEquals("0", ConsumableDatabase.getAdventureRange("seal tooth"))
    }

    @Test
    fun getMuscleRange_substatRange() {
        assertEquals("2-3", ConsumableDatabase.getMuscleRange("acceptable bagel"))
    }

    @Test
    fun formatRange_singleValue() {
        assertEquals("8", ConsumableDatabase.formatRange(8, 8))
    }

    @Test
    fun formatRange_zeroPair() {
        assertEquals("0", ConsumableDatabase.formatRange(0, 0))
    }

    @Test
    fun getLevelReqByName_nonfilling() {
        assertEquals(0, ConsumableDatabase.getLevelReqByName("battery (AAA)"))
        assertEquals(13, ConsumableDatabase.getLevelReqByName("mime army challenge coin"))
    }
}
