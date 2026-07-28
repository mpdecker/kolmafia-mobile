package net.sourceforge.kolmafia.shop

/** Desktop [FlowerTradeinRequest.accessible] rose/tulip inventory gate. */
object FlowerTradeinAccessibility {

    const val ROSE = 8668
    const val WHITE_TULIP = 8669
    const val RED_TULIP = 8670
    const val BLUE_TULIP = 8671

    private val FLOWER_IDS = intArrayOf(ROSE, WHITE_TULIP, RED_TULIP, BLUE_TULIP)

    fun hasTradeFlower(accessibleCount: (Int) -> Int): Boolean =
        FLOWER_IDS.any { accessibleCount(it) > 0 }
}
