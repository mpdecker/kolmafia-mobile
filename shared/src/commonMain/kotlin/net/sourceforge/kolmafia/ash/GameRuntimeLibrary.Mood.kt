package net.sourceforge.kolmafia.ash

// MoodManager (T3 implementation) tracks a single activeMood and a moodLibrary.
// Both get_moods() and mood_list() return a string[int] aggregate of mood names
// from the mood library, sorted alphabetically by name.
internal fun GameRuntimeLibrary.registerMoodQueries(scope: AshScope) {

    val stringIntType = AggregateType(AshType.INT, AshType.STRING)

    fun buildMoodList(): AggregateValue {
        val agg = AggregateValue(stringIntType)
        moodManager?.moodLibrary?.values
            ?.sortedBy { it.displayName() }
            ?.forEachIndexed { i, mood ->
                agg[AshValue.of(i.toLong())] = AshValue.of(mood.displayName())
            }
        return agg
    }

    // get_moods() → string[int]  (returns mood names from library, sorted alphabetically)
    regFn(scope, "get_moods", stringIntType, emptyList()) { _, _ ->
        buildMoodList()
    }

    // mood_list() → string[int]  (alias for get_moods)
    regFn(scope, "mood_list", stringIntType, emptyList()) { _, _ ->
        buildMoodList()
    }
}
