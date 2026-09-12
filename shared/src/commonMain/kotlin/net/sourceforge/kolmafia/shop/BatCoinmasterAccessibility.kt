package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.session.BatManager

/**
 * Desktop Bat-Fabricator / ChemiCorp / Orphanage / PD downtown accessibility
 * (Phases 1351–1360 subset; XLVIII Track C wires shop accessible()).
 */
object BatCoinmasterAccessibility {

    const val FABRICATOR = "batman_cave"
    const val CHEMICORP = "batman_chemicorp"
    const val ORPHANAGE = "batman_orphanage"
    const val PD = "batman_pd"

    /** Fabricator requires batman mode and Bat-Cavern zone. */
    fun fabricatorAccessible(limitMode: String): Boolean =
        fabricatorInaccessibleReason(limitMode) == null

    /** Downtown shops require Downtown zone. */
    fun downtownShopAccessible(limitMode: String, shopId: String): Boolean {
        if (!limitMode.equals("batman", ignoreCase = true)) return false
        if (shopId !in setOf(CHEMICORP, ORPHANAGE, PD)) return false
        return isDowntown()
    }

    /**
     * Desktop ChemiCorp / Orphanage / PD [accessible] messages.
     * Returns null when accessible.
     */
    fun downtownInaccessibleReason(limitMode: String, shopId: String): String? {
        val (onlyBatfellow, onlyDowntown) = when (shopId) {
            CHEMICORP ->
                "Only Batfellow can go to ChemiCorp." to
                    "Batfellow can only visit ChemiCorp while Downtown."
            ORPHANAGE ->
                "Only Batfellow can go to the Gotpork Orphanage." to
                    "Batfellow can only visit the Gotpork Orphanage while Downtown."
            PD ->
                "Only Batfellow can go to the Gotpork P. D." to
                    "Batfellow can only visit the Gotpork Police Department while Downtown."
            else -> return null
        }
        if (!limitMode.equals("batman", ignoreCase = true)) return onlyBatfellow
        if (!isDowntown()) return onlyDowntown
        return null
    }

    /**
     * Desktop Bat-Fabricator [accessible] messages.
     * Returns null when accessible.
     */
    fun fabricatorInaccessibleReason(limitMode: String): String? {
        if (!limitMode.equals("batman", ignoreCase = true)) {
            return "Only Batfellow can use the Bat-Fabricator."
        }
        if (!isBatCavern()) {
            return "Batfellow can only use the Bat-Fabricator in the BatCavern."
        }
        return null
    }

    /** Improved printer halves some fabricator costs (desktop price 2 vs 3). */
    fun fabricatorTokenCost(baseCost: Int): Int =
        if (BatManager.hasImprovedPrinter()) (baseCost * 2 / 3).coerceAtLeast(1) else baseCost

    private fun isDowntown(): Boolean =
        BatManager.currentBatZone() == BatManager.DOWNTOWN ||
            BatManager.currentBatZone().contains("Downtown", ignoreCase = true)

    private fun isBatCavern(): Boolean =
        BatManager.currentBatZone() == BatManager.BAT_CAVERN ||
            BatManager.currentBatZone().contains("Bat-Cavern", ignoreCase = true) ||
            BatManager.currentBatZone().contains("BatCavern", ignoreCase = true)
}
