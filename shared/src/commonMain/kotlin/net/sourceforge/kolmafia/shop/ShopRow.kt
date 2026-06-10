package net.sourceforge.kolmafia.shop

data class ShopRow(
    val rowId: Int,
    val item: ItemStack,
    val costs: List<ItemStack> = emptyList(),
    /** Token/meat price for old-style buy/sell rows when [costs] is empty. */
    val price: Int = 0,
) {
    val isMeatPurchase: Boolean
        get() = costs.size == 1 && costs[0].isMeat
}
