package net.sourceforge.kolmafia.mood

/**
 * Instructs the mood system to cast [skillId] whenever [effectId] has fewer
 * than [minimumTurns] remaining.  [effectName] and [skillName] are display-only
 * labels used in UI and logging.
 */
data class MoodTrigger(
    val effectId: Int,
    val effectName: String,
    val skillId: Int,
    val skillName: String,
    val minimumTurns: Int = 1,
)
