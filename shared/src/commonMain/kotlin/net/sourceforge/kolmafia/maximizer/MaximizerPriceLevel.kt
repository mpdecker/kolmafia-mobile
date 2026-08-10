package net.sourceforge.kolmafia.maximizer

/** Desktop [net.sourceforge.kolmafia.maximizer.PriceLevel] for mall price validation. */
enum class MaximizerPriceLevel {
    DONT_CHECK,
    BUYABLE_ONLY,
    ALL;

    companion object {
        fun byIndex(index: Int): MaximizerPriceLevel = when (index) {
            2 -> ALL
            1 -> BUYABLE_ONLY
            else -> DONT_CHECK
        }
    }
}
