package net.sourceforge.kolmafia.mall

enum class MallListingSource {
    MALL,
    NPC,
    COINMASTER,
}

data class MallListing(
    val shopId: Int,
    val shopName: String,
    val itemId: Int,
    val price: Long,
    val quantity: Int,
    val limit: Int = quantity,
    val canPurchase: Boolean = true,
    val timestampSeconds: Long = currentEpochSeconds(),
    val source: MallListingSource = MallListingSource.MALL,
)
