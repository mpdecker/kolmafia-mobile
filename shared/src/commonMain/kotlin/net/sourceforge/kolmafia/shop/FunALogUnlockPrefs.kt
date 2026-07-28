package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop FunALogRequest.ITEM_TO_UNLOCK_PREF shared by visit sync + validate gates. */
object FunALogUnlockPrefs {

    const val PIRATE_REALM_FUN_LOG = 10225

    /** itemId → unlock pref (multiple rhum variants share pirateRealmUnlockedRhum). */
    val itemToUnlockPref: Map<Int, String> = mapOf(
        10199 to "pirateRealmUnlockedCrabsicle",
        10201 to "pirateRealmUnlockedRhum",
        10202 to "pirateRealmUnlockedRhum",
        10203 to "pirateRealmUnlockedRhum",
        10204 to "pirateRealmUnlockedShavingCream",
        10205 to "pirateRealmUnlockedBreastplate",
        10210 to "pirateRealmUnlockedRadioRing",
        10211 to "pirateRealmUnlockedTikiSkillbook",
        10224 to "pirateRealmUnlockedTattoo",
        10226 to "pirateRealmUnlockedPlushie",
        10227 to "pirateRealmUnlockedFork",
        10228 to "pirateRealmUnlockedScurvySkillbook",
        10229 to "pirateRealmUnlockedGoldRing",
        10230 to "pirateRealmUnlockedBlunderbuss",
    )

    fun isItemAvailable(itemId: Int, prefs: Preferences?): Boolean {
        val prefKey = itemToUnlockPref[itemId] ?: return true
        return prefs?.getBoolean(prefKey, false) == true
    }

    fun syncFromShopHtml(html: String, prefs: Preferences) {
        val presentIds = ITEM_ROW_PATTERN.findAll(html)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .toSet()
        for ((itemId, prefKey) in itemToUnlockPref) {
            prefs.setBoolean(prefKey, itemId in presentIds)
        }
        FUN_POINTS_PATTERN.find(html)?.let { match ->
            val points = match.groupValues[1].replace(",", "").toIntOrNull() ?: return
            prefs.setInt("availableFunPoints", points)
        }
    }

    private val ITEM_ROW_PATTERN = Regex("""<tr rel="(\d+)"""")
    private val FUN_POINTS_PATTERN = Regex("""<b>You have ([\d,]+) FunPoints?\.</b>""")
}
