package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.campground.CampgroundItemSync

/** Desktop ConcoctionDatabase.canQueueFood/canQueueBooze/isMayo special consumption queue routing. */
object ConcoctionMayoQueue {

    const val MAYO_CLINIC = 8260
    const val MAYONEX = 8261
    const val MAYODIOL = 8262
    const val MAYOSTAT = 8263
    const val MAYOZAPINE = 8264
    const val MAYOFLEX = 8265

    private const val MUNCHIES_PILL = 1619
    private const val QUANTUM_TACO = 4412
    private const val SCHRODINGERS_THERMOS = 4413
    private const val MAGICAL_SAUSAGE = 10060
    private const val WHETSTONE = 11107

    fun isMayo(itemId: Int): Boolean = when (itemId) {
        MAYONEX, MAYODIOL, MAYOSTAT, MAYOZAPINE, MAYOFLEX -> true
        else -> false
    }

    fun canQueueFood(itemId: Int, context: ConcoctionQueueContext): Boolean {
        when (itemId) {
            QUANTUM_TACO, MUNCHIES_PILL, WHETSTONE, MAGICAL_SAUSAGE -> return true
        }
        if (!isMayo(itemId)) return false
        if (ConcoctionQueueBudget.lastQueuedMayo != 0) return false
        val mayoInMouth = context.getStringPref("mayoInMouth")
        if (context.foodQueueDepth() == 0 && mayoInMouth.isNotEmpty()) return false
        return CampgroundItemSync.hasWorkshedItem(context.preferences, MAYO_CLINIC)
    }

    fun canQueueBooze(itemId: Int): Boolean = itemId == SCHRODINGERS_THERMOS
}
