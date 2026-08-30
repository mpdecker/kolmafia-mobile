package net.sourceforge.kolmafia.mall

data class MallListing(
    val shopId: Int,
    val shopName: String,
    val itemId: Int,
    val price: Long,
    val quantity: Int,
    val limit: Int = quantity,
    val canPurchase: Boolean = true,
    val timestampSeconds: Long = currentEpochSeconds(),
)
