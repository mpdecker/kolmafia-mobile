package net.sourceforge.kolmafia.data

data class PackageData(
    val name: String,
    val containedItemId: Int,
    val count: Int,
    val meatValue: Int
)
