package net.sourceforge.kolmafia.data

/** Resolved cafe menu row for purchase HTTP. */
data class CafeMenuEntry(
    val name: String,
    val cafeId: String,
    val whichItem: Int,
    val price: Int,
    val type: ConsumableType,
)
