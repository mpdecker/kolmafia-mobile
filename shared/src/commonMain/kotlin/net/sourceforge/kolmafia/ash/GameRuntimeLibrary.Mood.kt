package net.sourceforge.kolmafia.ash

// MoodManager (T3 implementation) tracks only a single activeMood; there is no mood
// library / named-mood list available yet.  Both get_moods() and mood_list() return an
// empty string[int] aggregate until a mood library is added to MoodManager.
internal fun GameRuntimeLibrary.registerMoodQueries(scope: AshScope) {

    val stringIntType = AggregateType(AshType.INT, AshType.STRING)

    // get_moods() → string[int]  (stub — no mood library in MoodManager yet)
    regFn(scope, "get_moods", stringIntType, emptyList()) { _, _ ->
        AggregateValue(stringIntType)
    }

    // mood_list() → string[int]  (alias for get_moods)
    regFn(scope, "mood_list", stringIntType, emptyList()) { _, _ ->
        AggregateValue(stringIntType)
    }
}
