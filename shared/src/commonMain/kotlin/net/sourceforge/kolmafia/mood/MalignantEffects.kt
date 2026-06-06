package net.sourceforge.kolmafia.mood

/**
 * Effect names auto-removed by [MoodManager.removeMalignantEffects] when
 * [net.sourceforge.kolmafia.preferences.Preferences.REMOVE_MALIGNANT_EFFECTS] is true.
 *
 * Matches desktop MoodManager.AUTO_CLEAR (EffectPool entries):
 *   BEATEN_UP, TETANUS, AMNESIA, CUNCTATITIS, and the five poison variants.
 */
object MalignantEffects {
    val NAMES: Set<String> = setOf(
        "Beaten Up",
        "Tetanus",
        "Amnesia",
        "Cunctatitis",
        "Hardly Poisoned at All",
        "A Little Bit Poisoned",
        "Somewhat Poisoned",
        "Really Quite Poisoned",
        "Majorly Poisoned",
    )
}
