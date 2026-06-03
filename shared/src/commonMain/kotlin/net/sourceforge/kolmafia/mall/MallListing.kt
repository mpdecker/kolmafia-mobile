package net.sourceforge.kolmafia.mall

data class MallListing(
    val shopId: Int,
    val shopName: String,
    val itemId: Int,
    val price: Long,
    val quantity: Int
)
