package net.sourceforge.kolmafia.inventory

data class InventoryState(
    val items: Map<Int, InventoryItem> = emptyMap(),
    val equipped: Map<String, InventoryItem> = emptyMap(),
    val fullness: Int = 0, val inebriety: Int = 0,
    val spleenUsed: Int = 0, val isStale: Boolean = false
)
