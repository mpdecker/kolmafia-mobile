package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.ash.kolRolloverDayDifference

/** Desktop [HolidayDatabase.getDayDifference] for consequence desc rotation. */
object KoLRolloverCalendar {
    fun getDayDifference(): Long = kolRolloverDayDifference()
}
