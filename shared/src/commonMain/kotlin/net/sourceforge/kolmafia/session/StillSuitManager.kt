package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [net.sourceforge.kolmafia.session.StillSuitManager] — familiar-sweat accrual from
 * fight HTML, choice-1476 dram/mod parse, and distillate-drink clear.
 */
object StillSuitManager {
    const val PREF_SWEAT = "familiarSweat"
    const val PREF_NEXT_MODS = "nextDistillateMods"
    const val STILLSUIT_GIF = "stillsuit.gif"

    private val DRAMS = Regex("""<b>(\d+)</b> drams|Looks like there are (\d+) drams""")
    private val EFFECTS_BLOCK = Regex("""<div.*?>(.*?)</div>""", RegexOption.IGNORE_CASE)

    fun clearSweat(preferences: Preferences?) {
        setSweat(preferences, 0)
    }

    fun handleSweat(
        html: String,
        preferences: Preferences?,
        familiarHasStillSuit: Boolean,
        anyOwnedFamiliarHasStillSuit: Boolean,
    ): Boolean {
        if (!html.contains(STILLSUIT_GIF) || preferences == null) return false
        val drams = when {
            familiarHasStillSuit -> 3
            anyOwnedFamiliarHasStillSuit -> 1
            else -> return false
        }
        preferences.setInt(PREF_SWEAT, preferences.getInt(PREF_SWEAT, 0) + drams)
        return true
    }

    fun parseChoice(html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        var changed = false
        DRAMS.find(html)?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }?.toIntOrNull()?.let {
            setSweat(preferences, it)
            changed = true
        }
        val mods = EFFECTS_BLOCK.findAll(html)
            .map { it.groupValues[1].replace(Regex("<[^>]+>"), "").trim() }
            .filter { it.isNotEmpty() && (it.startsWith("+") || it.startsWith("-") || it.contains(":")) }
            .joinToString(",")
        if (mods.isNotEmpty()) {
            preferences.setString(PREF_NEXT_MODS, mods)
            changed = true
        }
        return changed
    }

    fun handleDrink(html: String, preferences: Preferences?): Boolean {
        if (!html.contains("You put your lips to the nozzle")) return false
        clearSweat(preferences)
        preferences?.setString(PREF_NEXT_MODS, "")
        return true
    }

    fun hasStillSuit(itemName: String?): Boolean =
        itemName?.contains("stillsuit", ignoreCase = true) == true

    fun itemId(): Int = ItemPool.STILLSUIT

    private fun setSweat(preferences: Preferences?, drams: Int) {
        preferences?.setInt(PREF_SWEAT, drams)
    }
}
