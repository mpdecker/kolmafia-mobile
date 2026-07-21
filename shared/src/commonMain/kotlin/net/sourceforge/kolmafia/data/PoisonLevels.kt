package net.sourceforge.kolmafia.data

/**
 * Maps monster poison effect names to severity levels.
 * Mirrors desktop [EffectDatabase.getPoisonLevel] / [EffectDatabase.getPoisonName].
 * Lower values are more severe.
 */
object PoisonLevels {

    private val EFFECT_NAMES = arrayOf(
        "", // unused index 0
        "Toad In The Hole",
        "Majorly Poisoned",
        "Really Quite Poisoned",
        "Somewhat Poisoned",
        "A Little Bit Poisoned",
        "Hardly Poisoned at All",
    )

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

    /** Desktop MonsterProxy poison EFFECT name; [Int.MAX_VALUE] → `"none"`. */
    fun effectNameForLevel(level: Int): String {
        if (level < 1 || level >= EFFECT_NAMES.size) return "none"
        return EFFECT_NAMES[level]
    }
}
