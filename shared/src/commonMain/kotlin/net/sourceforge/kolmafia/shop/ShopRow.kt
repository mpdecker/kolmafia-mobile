package net.sourceforge.kolmafia.shop

data class ShopRow(
    val rowId: Int,
    val item: ItemStack,
    val costs: List<ItemStack>
) {
    val isMeatPurchase: Boolean
        get() = costs.size == 1 && costs[0].isMeat
}
