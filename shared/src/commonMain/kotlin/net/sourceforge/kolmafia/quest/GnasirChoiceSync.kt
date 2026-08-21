package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] A Sietch in Time choice 805 (Gnasir turn-ins).
 * Option numbers reorder; progress is response-text driven bitflags.
 */
object GnasirChoiceSync {

    const val CHOICE_ID = 805

    const val STONE_ROSE = 2326
    const val BLACK_PAINT = 2327
    const val KILLING_JAR = 6847
    const val WORM_RIDING_MANUAL_PAGE = 2320

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        var progress = preferences.getInt("gnasirProgress", 0)
        var changed = false
        when {
            html.contains("give the stone rose to Gnasir") -> {
                consumeItem(STONE_ROSE, 1)
                progress = progress or 1
                changed = true
            }
            html.contains("hold up the bucket of black paint") -> {
                consumeItem(BLACK_PAINT, 1)
                progress = progress or 2
                changed = true
            }
            html.contains("hand Gnasir the glass jar") -> {
                consumeItem(KILLING_JAR, 1)
                progress = progress or 4
                changed = true
            }
            html.contains("hand him the pages") -> {
                consumeItem(WORM_RIDING_MANUAL_PAGE, 15)
                progress = progress or 8
                changed = true
            }
        }
        if (changed) {
            preferences.setInt("gnasirProgress", progress)
        }
        return changed
    }
}
