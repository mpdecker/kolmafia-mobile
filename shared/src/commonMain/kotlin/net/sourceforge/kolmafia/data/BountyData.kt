package net.sourceforge.kolmafia.data

enum class BountyType { EASY, HARD, SPECIAL, UNKNOWN }

data class BountyData(
    val name: String,
    val plural: String,
    val type: BountyType,
    val image: String,
    val count: Int,
    val monster: String,
    val bestLocation: String,
) {
    fun typeString(): String = when (type) {
        BountyType.EASY -> "easy"
        BountyType.HARD -> "hard"
        BountyType.SPECIAL -> "special"
        BountyType.UNKNOWN -> ""
    }

    fun kolInternalType(): String = when (type) {
        BountyType.EASY -> "low"
        BountyType.HARD -> "high"
        BountyType.SPECIAL -> "special"
        BountyType.UNKNOWN -> ""
    }
}
