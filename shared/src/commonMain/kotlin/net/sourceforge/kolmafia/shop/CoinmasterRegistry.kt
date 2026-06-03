package net.sourceforge.kolmafia.shop

object CoinmasterRegistry {

    val all: List<CoinmasterData> by lazy { buildRegistry() }

    fun findByNickname(nickname: String): CoinmasterData? =
        all.firstOrNull { it.nickname.equals(nickname, ignoreCase = true) }

    fun findByShopId(shopId: String): CoinmasterData? =
        all.firstOrNull { it.shopId == shopId }

    private fun buildRegistry(): List<CoinmasterData> = listOf(
        CoinmasterData(
            masterName = "Dimemaster",
            nickname = "dmt",
            token = "Mr. Accessory",
            shopId = "dmt",
            buyItems = listOf(
                ShopRow(
                    rowId = 1,
                    item = ItemStack(itemId = 2764, count = 1),   // ten-percent bonus
                    costs = listOf(ItemStack(itemId = 46, count = 5)) // Mr. Accessory
                )
            ),
            sellItems = emptyList()
        ),
        CoinmasterData(
            masterName = "The Shore, Inc. Gift Shop",
            nickname = "shore",
            token = "Shore Inc. Ship Trip Scrip",
            shopId = "shore",
            buyItems = listOf(
                ShopRow(
                    rowId = 1,
                    item = ItemStack(itemId = 266, count = 1),    // stuffed cocoabo
                    costs = listOf(ItemStack(itemId = 667, count = 1)) // Ship Trip Scrip
                )
            ),
            sellItems = emptyList()
        ),
        CoinmasterData(
            masterName = "The Bounty Hunter Hunter",
            nickname = "bhh",
            token = "lucre",
            shopId = "bhh",
            buyItems = listOf(
                ShopRow(
                    rowId = 1,
                    item = ItemStack(itemId = 3485, count = 1),   // bounty-hunting rifle
                    costs = listOf(ItemStack(itemId = 3476, count = 20)) // lucre
                )
            ),
            sellItems = emptyList()
        )
    )
}
