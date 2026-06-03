package net.sourceforge.kolmafia.data

enum class BountyType { EASY, HARD, SPECIAL, UNKNOWN }

data class BountyData(
    val name: String,
    val plural: String,
    val type: BountyType,
    val image: String,
    val count: Int,
    val monster: String,
    val bestLocation: String
)
