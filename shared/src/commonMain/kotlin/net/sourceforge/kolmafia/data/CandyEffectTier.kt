package net.sourceforge.kolmafia.data

/** Desktop CandyDatabase.getEffectTier/getEffectModulus for synthesis candy effects. */
object CandyEffectTier {
    fun getEffectTier(effectId: Int): Int = when (effectId) {
        2165, 2166, 2167, 2168, 2169 -> 1 // SYNTHESIS_HOT..GREASY
        2170, 2171, 2172, 2173, 2174 -> 2 // SYNTHESIS_STRONG..ENERGY
        2175, 2176, 2177, 2178, 2179 -> 3 // SYNTHESIS_GREED..STYLE
        else -> 0
    }

    fun getEffectModulus(effectId: Int): Int = when (effectId) {
        2165, 2170, 2175 -> 0 // HOT, STRONG, GREED
        2166, 2171, 2176 -> 1 // COLD, SMART, COLLECTION
        2167, 2172, 2177 -> 2 // PUNGENT, COOL, MOVEMENT
        2168, 2173, 2178 -> 3 // SCARY, HARDY, LEARNING
        2169, 2174, 2179 -> 4 // GREASY, ENERGY, STYLE
        else -> -1
    }

    /** Reverse lookup: tier (1..3) + modulus (0..4) → effect ID, or -1. */
    fun getEffectIdByTierAndModulus(tier: Int, modulus: Int): Int = when (tier) {
        1 -> 2165 + modulus
        2 -> 2170 + modulus
        3 -> 2175 + modulus
        else -> -1
    }
}
