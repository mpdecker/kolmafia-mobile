package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.preferences.Preferences

/** Parse and apply desktop Maximizer filterType sets (~548–573 in Maximizer.java). */
object MaximizerFilters {

    private const val PREF_LAST_FILTERS = "maximizerLastFilters"

    fun allEnabled(): Set<MaximizerFilterType> = MaximizerFilterType.entries.toSet()

    fun fromPreferences(preferences: Preferences?): Set<MaximizerFilterType> {
        val raw = preferences?.getString(PREF_LAST_FILTERS, "").orEmpty()
        return if (raw.isBlank()) allEnabled() else parseFromString(raw)
    }

    /** Substring match on lowercase filter names (desktop RuntimeLibrary maximize 5-arg). */
    fun parseFromString(raw: String): Set<MaximizerFilterType> {
        val lower = raw.lowercase()
        return buildSet {
            for (filter in MaximizerFilterType.entries) {
                if (lower.contains(filter.name.lowercase())) {
                    add(filter)
                }
            }
        }
    }

    fun baseCommand(source: String): String {
        val trimmed = source.trim()
        return if (trimmed.contains(' ')) trimmed.substringBefore(' ') else trimmed
    }

    fun allowsSource(source: String, filters: Set<MaximizerFilterType>): Boolean {
        if (filters.isEmpty()) return false
        return when (baseCommand(source).lowercase()) {
            "cast" -> MaximizerFilterType.CAST in filters
            "synthesize", "chew" -> MaximizerFilterType.SPLEEN in filters
            "drink" -> MaximizerFilterType.BOOZE in filters
            "eat" -> MaximizerFilterType.FOOD in filters
            "use" -> MaximizerFilterType.USABLE in filters
            "genie", "monkeypaw" -> MaximizerFilterType.WISH in filters
            else -> MaximizerFilterType.OTHER in filters
        }
    }

    fun isEquipOnly(filters: Set<MaximizerFilterType>): Boolean =
        filters.size == 1 && MaximizerFilterType.EQUIP in filters
}
