package net.sourceforge.kolmafia.data

/** Desktop CandyDatabase.getEffectTier(int effectId) for synthesis candy effects. */
object CandyEffectTier {
    fun getEffectTier(effectId: Int): Int = when (effectId) {
        2165, 2166, 2167, 2168, 2169 -> 1 // SYNTHESIS_HOT..GREASY
        2170, 2171, 2172, 2173, 2174 -> 2 // SYNTHESIS_STRONG..ENERGY
        2175, 2176, 2177, 2178, 2179 -> 3 // SYNTHESIS_GREED..STYLE
        else -> 0
    }
}
