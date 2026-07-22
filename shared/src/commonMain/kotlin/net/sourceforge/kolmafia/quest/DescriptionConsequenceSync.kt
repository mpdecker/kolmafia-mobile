package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.DescriptionConsequenceRegistry
import net.sourceforge.kolmafia.data.KoLRolloverCalendar

/** Desktop [ConsequenceManager.updateOneDesc] desc rotation prefetch. */
object DescriptionConsequenceSync {

    fun pathForToday(dayDifference: Long = KoLRolloverCalendar.getDayDifference()): String? =
        DescriptionConsequenceRegistry.urlForDay(dayDifference)
}
