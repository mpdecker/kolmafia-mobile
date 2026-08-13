package net.sourceforge.kolmafia.data

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

class HolidayNamesTest {

    @AfterTest
    fun tearDown() {
        HolidayNames.clearOverride()
    }

    @Test
    fun override_appendedToHolidayString() {
        HolidayNames.setHoliday("Bill 1")
        assertTrue(HolidayNames.getHoliday().contains("Bill 1"))
    }
}
