package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Rummaging through the Garbage choice 1275.
 * Defers equipment unequip / inventory remove of tote parts.
 */
object GarbageToteChoiceSync {

    const val CHOICE_ID = 1275

    private val DECEASED_TREE_PATTERN = Regex("""Looks like it has (.*?) needle""")
    private val BROKEN_CHAMPAGNE_PATTERN = Regex("""Looks like it has (\d+) ounce""")
    private val GARBAGE_SHIRT_PATTERN = Regex("""Looks like you can read roughly (\d+) scrap""")

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        var changed = false
        if (decision in 1..5) {
            if (!preferences.getBoolean("_garbageItemChanged", false)) {
                preferences.setInt("garbageTreeCharge", 1000)
                preferences.setInt("garbageChampagneCharge", 11)
                preferences.setInt("garbageShirtCharge", 37)
            }
            preferences.setBoolean("_garbageItemChanged", true)
            changed = true
        }
        DECEASED_TREE_PATTERN.find(html)?.groupValues?.getOrNull(1)
            ?.replace(",", "")
            ?.toIntOrNull()
            ?.let {
                preferences.setInt("garbageTreeCharge", it)
                changed = true
            }
        BROKEN_CHAMPAGNE_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            preferences.setInt("garbageChampagneCharge", it)
            changed = true
        }
        GARBAGE_SHIRT_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            preferences.setInt("garbageShirtCharge", it)
            changed = true
        }
        return changed
    }
}
