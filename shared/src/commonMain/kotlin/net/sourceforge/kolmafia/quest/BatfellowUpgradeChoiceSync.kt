package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BatManager

/**
 * Desktop [BatManager] Bat-Suit / Bat-Sedan / Bat-Cavern upgrade choices 1137–1139.
 * Routes through BatManager so BatStats multipliers apply.
 */
object BatfellowUpgradeChoiceSync {

    const val SUIT = 1137
    const val SEDAN = 1138
    const val CAVERN = 1139

    const val UPGRADES_PREF = "batmanUpgrades"
    const val FUNDS_PREF = "batmanFundsAvailable"

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        BatManager.restoreUpgradesFromPref(preferences)
        return when (choiceId) {
            SUIT -> BatManager.batSuitUpgrade(decision, preferences)
            SEDAN -> BatManager.batSedanUpgrade(decision, preferences)
            CAVERN -> BatManager.batCavernUpgrade(decision, preferences)
            else -> false
        }
    }
}
