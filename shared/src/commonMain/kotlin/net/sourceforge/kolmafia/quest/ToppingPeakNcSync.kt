package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleABooPeakChange] / [QuestManager.handleOilPeakChange]
 * adventure.php lighting NC titles.
 */
object ToppingPeakNcSync {

    const val ABOO_PEAK = 296
    const val OIL_PEAK = 298

    const val ABOO_TITLE = "Come On Ghosty, Light My Pyre"
    const val OIL_TITLE = "Unimpressed with Pressure"

    fun applyFromAdventure(
        url: String?,
        html: String,
        preferences: Preferences?,
        adventureId: String? = null,
    ): Boolean {
        if (preferences == null) return false
        val area = adventureId?.toIntOrNull()
            ?: Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(url.orEmpty())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        return when (area) {
            ABOO_PEAK -> {
                if (!html.contains(ABOO_TITLE)) return false
                preferences.setBoolean("booPeakLit", true)
                preferences.setInt("booPeakProgress", 0)
                true
            }
            OIL_PEAK -> {
                if (!html.contains(OIL_TITLE)) return false
                preferences.setBoolean("oilPeakLit", true)
                preferences.setString("oilPeakProgress", "0")
                true
            }
            else -> false
        }
    }
}
