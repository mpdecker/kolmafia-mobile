package net.sourceforge.kolmafia.shop

/**
 * Desktop Spaaace isotope shop [accessible] gates
 * (HTTP Residual LII Track A: Isotope Smithery / Dollhawker / Lunar Lunch).
 *
 * Desktop [SpaaaceRequest.accessible]: generator quest finished + Transpondent
 * effect or transporter transponder in inventory.
 */
object SpaaaceShopAccessibility {

    const val TRANSPONDER = 5170
    const val TRANSPONDENT_EFFECT = 846

    fun inaccessible(
        generatorFinished: Boolean,
        accessibleCount: (Int) -> Int,
        hasEffect: (Int) -> Boolean,
    ): String? {
        if (!generatorFinished) {
            return "You need to repair the Elves' Shield Generator to go there."
        }
        if (hasEffect(TRANSPONDENT_EFFECT) || accessibleCount(TRANSPONDER) > 0) {
            return null
        }
        return "You need a transporter transponder to go there."
    }
}
