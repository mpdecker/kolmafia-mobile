package net.sourceforge.kolmafia.data

data class CafeData(
    val id: Int,
    val name: String,
    val price: Int = 0,
    val type: ConsumableType, // FOOD or DRINK
)
