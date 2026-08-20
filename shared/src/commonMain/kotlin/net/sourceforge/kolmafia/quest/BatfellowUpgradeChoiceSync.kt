package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [BatManager] Bat-Suit / Bat-Sedan / Bat-Cavern upgrade choices 1137–1139.
 */
object BatfellowUpgradeChoiceSync {

    const val SUIT = 1137
    const val SEDAN = 1138
    const val CAVERN = 1139

    const val UPGRADES_PREF = "batmanUpgrades"
    const val FUNDS_PREF = "batmanFundsAvailable"

    private data class Upgrade(val option: Int, val name: String)

    private val SUIT_UPGRADES = listOf(
        Upgrade(1, "Hardened Knuckles"),
        Upgrade(2, "Steel-Toed Bat-Boots"),
        Upgrade(3, "Extra-Swishy Cloak"),
        Upgrade(4, "Pec-Guards"),
        Upgrade(5, "Kevlar Undergarments"),
        Upgrade(6, "Improved Cowl Optics"),
        Upgrade(7, "Asbestos Lining"),
        Upgrade(8, "Utility Belt First Aid Kit"),
    )

    private val SEDAN_UPGRADES = listOf(
        Upgrade(1, "Rocket Booster"),
        Upgrade(2, "Glove Compartment First-Aid Kit"),
        Upgrade(3, "Street Sweeper"),
        Upgrade(4, "Advanced Air Filter"),
        Upgrade(5, "Orphan Scoop"),
        Upgrade(6, "Spotlight"),
        Upgrade(7, "Bat-Freshener"),
        Upgrade(8, "Loose Bearings"),
    )

    private val CAVERN_UPGRADES = listOf(
        Upgrade(1, "Really Long Winch"),
        Upgrade(2, "Improved 3-D Bat-Printer"),
        Upgrade(3, "Transfusion Satellite"),
        Upgrade(4, "Surveillance Network"),
        Upgrade(5, "Blueprints Database"),
        Upgrade(7, "Snugglybear Nightlight"),
        Upgrade(8, "Glue Factory"),
    )

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        val upgrades = when (choiceId) {
            SUIT -> SUIT_UPGRADES
            SEDAN -> SEDAN_UPGRADES
            CAVERN -> CAVERN_UPGRADES
            else -> return false
        }
        val upgrade = upgrades.firstOrNull { it.option == decision } ?: return false
        return addUpgrade(upgrade.name, preferences)
    }

    private fun addUpgrade(name: String, preferences: Preferences): Boolean {
        val existing = preferences.getString(UPGRADES_PREF, "")
            .split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()
        if (existing.any { it.equals(name, ignoreCase = true) }) return false
        existing.add(name)
        preferences.setString(UPGRADES_PREF, existing.joinToString(";"))
        val funds = preferences.getInt(FUNDS_PREF, 0)
        if (funds > 0) {
            preferences.setInt(FUNDS_PREF, funds - 1)
        }
        return true
    }
}
