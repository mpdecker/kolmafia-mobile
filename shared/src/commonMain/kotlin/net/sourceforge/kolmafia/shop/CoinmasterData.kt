package net.sourceforge.kolmafia.shop

data class CoinmasterData(
    val masterName: String,
    val nickname: String,
    val token: String?,
    val shopId: String?,
    val buyItems: List<ShopRow>,
    val sellItems: List<ShopRow>
)
