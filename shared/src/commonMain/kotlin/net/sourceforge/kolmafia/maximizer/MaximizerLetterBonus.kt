package net.sourceforge.kolmafia.maximizer

/**
 * Desktop LetterBonus — name-based bonus helpers for Maximizer `letter` / `number` goals.
 */
object MaximizerLetterBonus {
    private val numberPattern = Regex("[0-9]")

    fun letterBonus(name: String?): Double {
        if (name.isNullOrBlank()) return 0.0
        return name.length.toDouble()
    }

    fun letterBonus(name: String?, letter: String): Double {
        if (name.isNullOrBlank() || letter.isBlank()) return 0.0
        val pattern = Regex(Regex.escape(letter), RegexOption.IGNORE_CASE)
        return pattern.findAll(name).count().toDouble()
    }

    fun numberBonus(name: String?): Double {
        if (name.isNullOrBlank()) return 0.0
        return numberPattern.findAll(name).count().toDouble()
    }
}
