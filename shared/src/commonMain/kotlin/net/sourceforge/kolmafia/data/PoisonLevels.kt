package net.sourceforge.kolmafia.data

/**
 * Maps monster poison effect names to severity levels.
 * Mirrors desktop [EffectDatabase.getPoisonLevel].
 * Lower values are more severe.
 */
object PoisonLevels {

    fun levelForEffectName(text: String): Int {
        val lower = text.lowercase()
        if (lower.contains("toad in the hole")) return 1
        if (!lower.contains("poisoned")) return Int.MAX_VALUE
        return when {
            lower.contains("majorly poisoned") -> 2
            lower.contains("really quite poisoned") -> 3
            lower.contains("somewhat poisoned") -> 4
            lower.contains("a little bit poisoned") -> 5
            lower.contains("hardly poisoned at all") -> 6
            else -> Int.MAX_VALUE
        }
    }
}
