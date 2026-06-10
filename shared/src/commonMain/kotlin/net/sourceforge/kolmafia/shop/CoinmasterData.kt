package net.sourceforge.kolmafia.shop

data class CoinmasterData(
    val masterName: String,
    val nickname: String,
    val nicknames: List<String> = emptyList(),
    val token: String?,
    val shopId: String?,
    val buyItems: List<ShopRow>,
    val sellItems: List<ShopRow>,
    val useItemField: Boolean = false,
    val buyUrl: String? = null,
    val buyAction: String = "buy",
    val sellUrl: String? = null,
    val sellAction: String = "sell",
) {
    val allNicknames: List<String>
        get() = (listOf(nickname) + nicknames).distinct()

    fun buyRowFor(itemId: Int): ShopRow? =
        buyItems.firstOrNull { it.item.itemId == itemId }

    fun sellRowFor(itemId: Int): ShopRow? =
        sellItems.firstOrNull { it.item.itemId == itemId }

    fun isAccessible(): Boolean = shopId != null || buyUrl != null || sellUrl != null

    fun inaccessibleReason(): String =
        if (isAccessible()) "" else "Shop not available"
}
