package net.sourceforge.kolmafia.shop

data class ItemStack(
    val itemId: Int,
    val count: Int,
    val isMeat: Boolean = false
)
