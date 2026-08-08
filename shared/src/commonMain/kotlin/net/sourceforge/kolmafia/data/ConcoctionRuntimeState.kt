package net.sourceforge.kolmafia.data

/** Desktop Concoction.initial/creatable/total — mutable refresh snapshot per concoction result. */
data class ConcoctionRuntimeState(
    val initial: Int = 0,
    val creatable: Int = 0,
    val total: Int = 0,
    val visibleTotal: Int = 0,
    val freeTotal: Int = 0,
    val price: Int = 0,
    val skipCalculate: Boolean = false,
    val queued: Int = 0,
    val queuedPulls: Int = 0,
    val pullable: Int = 0,
    val wasPossible: Boolean = false,
)
