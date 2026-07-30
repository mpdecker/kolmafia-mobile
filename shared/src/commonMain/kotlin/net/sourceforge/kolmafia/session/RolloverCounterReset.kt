package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.data.DefaultsDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Partial port of desktop [net.sourceforge.kolmafia.KoLmafia.resetCounters] for mobile login rollover.
 */
object RolloverCounterReset {

    const val LAST_COUNTER_DAY = "lastCounterDay"
    const val CAT_BURGLAR_BANK_HEISTS = "catBurglarBankHeists"
    const val CAT_BURGLAR_CHARGE = "_catBurglarCharge"
    const val CAT_BURGLAR_HEISTS_COMPLETE = "_catBurglarHeistsComplete"
    const val KOLHS_TOTAL = "kolhsTotalSchoolSpirited"
    const val KOLHS_YESTERDAY = "_kolhsSchoolSpirited"

    data class ResetSummary(
        val kolhsReset: Boolean = false,
        val catBurglarBankDelta: Int = 0,
        val perRolloverPrefsReset: Int = 0,
        val dailyPrefsReset: Int = 0,
        val wanderingCountersStopped: Int = 0,
    )

    /** Desktop: rollover - lastCounterDay > 3600 */
    fun shouldResetCounters(rolloverTimestamp: Long, lastCounterDay: Long): Boolean =
        rolloverTimestamp > 0 &&
            (lastCounterDay < 0 || rolloverTimestamp - lastCounterDay > 3600)

    fun resetCounters(preferences: Preferences, rolloverTimestamp: Long): ResetSummary {
        val kolhsReset = resetKolhsTotalSchoolSpirited(preferences)
        val catBurglarBankDelta = carryOverCatBurglarBankHeists(preferences)
        val perRolloverPrefsReset = DefaultsDatabase.resetPerRolloverPrefs(preferences)
        val dailyPrefsReset = DefaultsDatabase.resetDailies(preferences)
        val wanderingCountersStopped = TurnCounter.stopWanderingMonsterWindows(preferences)
        preferences.setLong(LAST_COUNTER_DAY, rolloverTimestamp)
        return ResetSummary(
            kolhsReset = kolhsReset,
            catBurglarBankDelta = catBurglarBankDelta,
            perRolloverPrefsReset = perRolloverPrefsReset,
            dailyPrefsReset = dailyPrefsReset,
            wanderingCountersStopped = wanderingCountersStopped,
        )
    }

    internal fun resetKolhsTotalSchoolSpirited(preferences: Preferences): Boolean {
        if (preferences.getBoolean(KOLHS_YESTERDAY, false)) return false
        preferences.setInt(KOLHS_TOTAL, 0)
        return true
    }

    internal fun carryOverCatBurglarBankHeists(preferences: Preferences): Int {
        var charge = preferences.getInt(CAT_BURGLAR_CHARGE, 0)
        var minChargeCost = 10
        var totalHeists = 0
        while (charge >= minChargeCost) {
            totalHeists++
            charge -= minChargeCost
            minChargeCost *= 2
        }
        val heistsComplete = preferences.getInt(CAT_BURGLAR_HEISTS_COMPLETE, 0)
        val bankHeists = preferences.getInt(CAT_BURGLAR_BANK_HEISTS, 0)
        val delta = totalHeists - heistsComplete
        if (delta != 0) {
            preferences.setInt(CAT_BURGLAR_BANK_HEISTS, bankHeists + delta)
        }
        return delta
    }

    internal fun unusedHeistsFromCharge(charge: Int): Int {
        var remaining = charge
        var minChargeCost = 10
        var totalHeists = 0
        while (remaining >= minChargeCost) {
            totalHeists++
            remaining -= minChargeCost
            minChargeCost *= 2
        }
        return totalHeists
    }
}
