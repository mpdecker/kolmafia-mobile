package net.sourceforge.kolmafia.mood

/** A named set of mood triggers; may inherit triggers from parent moods via [parentNames]. */
data class Mood(
    val name: String,
    val triggers: List<MoodTrigger> = emptyList(),
    val parentNames: List<String> = emptyList(),
) {
    /** Desktop-compatible display string, e.g. `"run extends default"`. */
    fun displayName(): String {
        if (parentNames.isEmpty()) return name
        val parents = parentNames.joinToString(", ")
        return if (name.isEmpty()) parents else "$name extends $parents"
    }

    /**
     * Merged trigger list: parent moods (in order) then local triggers.
     * Child local triggers override parent triggers targeting the same effect.
     */
    fun effectiveTriggers(
        library: Map<String, Mood>,
        visiting: MutableSet<String> = mutableSetOf(),
    ): List<MoodTrigger> {
        if (name.isNotEmpty() && name in visiting) return emptyList()
        val nextVisiting = visiting.toMutableSet()
        if (name.isNotEmpty()) nextVisiting += name

        val merged = mutableListOf<MoodTrigger>()
        for (parentName in parentNames) {
            if (parentName in nextVisiting) continue
            val parentMood = library[parentName]
            val parentTriggers = parentMood?.effectiveTriggers(library, nextVisiting)
                ?: emptyList()
            removeTriggersForEffects(merged, parentTriggers)
            merged.addAll(parentTriggers)
        }
        removeTriggersForEffects(merged, triggers)
        merged.addAll(triggers)
        return merged
    }

    private fun removeTriggersForEffects(
        merged: MutableList<MoodTrigger>,
        replacements: List<MoodTrigger>,
    ) {
        val effectIds = replacements.map { it.effectId }.toSet()
        merged.removeAll { it.effectId in effectIds }
    }

    companion object {
        val EMPTY = Mood(name = "default", triggers = emptyList())

        /**
         * Parses a mood name string per desktop [Mood.java]: `"foo extends default"`,
         * comma-only parent lists, and canonical name normalization.
         */
        fun parseName(raw: String): Pair<String, List<String>> {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return "" to emptyList()

            val extendsIndex = trimmed.indexOf(" extends ", ignoreCase = true)
            if (extendsIndex != -1) {
                val name = normalizeName(trimmed.substring(0, extendsIndex))
                val parentString = trimmed.substring(extendsIndex + 9)
                val parents = splitParentNames(parentString)
                return name to parents
            }

            if (trimmed.contains(',')) {
                return "" to splitParentNames(trimmed)
            }

            return normalizeName(trimmed) to emptyList()
        }

        /** Resolves a CLI or display name to the canonical library lookup key. */
        fun canonicalName(raw: String): String = parseName(raw).first

        private fun splitParentNames(raw: String): List<String> =
            raw.split(',')
                .map { normalizeName(it.trim()) }
                .filter { it.isNotEmpty() }

        private fun normalizeName(moodName: String): String {
            val trimmed = moodName.trim()
            if (trimmed.isEmpty() ||
                trimmed.equals("clear", ignoreCase = true) ||
                trimmed.equals("autofill", ignoreCase = true) ||
                trimmed.startsWith("exec", ignoreCase = true) ||
                trimmed.startsWith("repeat", ignoreCase = true)
            ) {
                return "default"
            }
            return Regex("[\\s,]+").replace(trimmed, "").lowercase()
        }
    }
}
