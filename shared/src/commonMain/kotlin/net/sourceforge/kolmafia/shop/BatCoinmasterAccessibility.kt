package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.session.BatManager

/**
 * Desktop Bat-Fabricator / ChemiCorp / Orphanage / PD downtown accessibility
 * (Phases 1351–1360 subset).
 */
object BatCoinmasterAccessibility {

    const val FABRICATOR = "batman_cave"
    const val CHEMICORP = "batman_chemicorp"
    const val ORPHANAGE = "batman_orphanage"
    const val PD = "batman_pd"

    /** Fabricator is always available in Bat-Cavern during batman mode. */
    fun fabricatorAccessible(limitMode: String): Boolean =
        limitMode.equals("batman", ignoreCase = true)

    /** Downtown shops require Downtown zone. */
    fun downtownShopAccessible(limitMode: String, shopId: String): Boolean {
        if (!limitMode.equals("batman", ignoreCase = true)) return false
        if (shopId !in setOf(CHEMICORP, ORPHANAGE, PD)) return false
        return BatManager.currentBatZone() == BatManager.DOWNTOWN ||
            BatManager.currentBatZone().contains("Downtown", ignoreCase = true)
    }

    /** Improved printer halves some fabricator costs (desktop price 2 vs 3). */
    fun fabricatorTokenCost(baseCost: Int): Int =
        if (BatManager.hasImprovedPrinter()) (baseCost * 2 / 3).coerceAtLeast(1) else baseCost
}
