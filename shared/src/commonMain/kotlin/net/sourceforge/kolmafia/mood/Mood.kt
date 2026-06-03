package net.sourceforge.kolmafia.mood

/** A named set of [MoodTrigger] entries that the mood system evaluates each turn. */
data class Mood(
    val name: String,
    val triggers: List<MoodTrigger>,
) {
    companion object {
        val EMPTY = Mood(name = "default", triggers = emptyList())
    }
}
