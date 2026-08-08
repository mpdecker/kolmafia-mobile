package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [BarrelShrineRequest.availableBarrelItem] + choice 1100 option mapping. */
object BarrelChoiceMapper {
    const val CHOICE_ID = 1100

    const val OPTION_PROTECTION = 1
    const val OPTION_GLAMOUR = 2
    const val OPTION_VIGOR = 3
    const val OPTION_BUFF = 4

    data class Prayer(val name: String, val reward: String, val id: Int)

    val PRAYERS: List<Prayer> = listOf(
        Prayer("protection", "barrel lid", OPTION_PROTECTION),
        Prayer("glamour", "barrel hoop earring", OPTION_GLAMOUR),
        Prayer("vigor", "bankruptcy barrel", OPTION_VIGOR),
        Prayer("buff", "class buff", OPTION_BUFF),
    )

    fun findPrayer(parameters: String): Int {
        val trimmed = parameters.trim()
        if (trimmed.isEmpty()) return 0
        for (prayer in PRAYERS) {
            if (trimmed.equals(prayer.name, ignoreCase = true) ||
                trimmed.equals(prayer.reward, ignoreCase = true)
            ) {
                return prayer.id
            }
        }
        return 0
    }

    fun resultNameForOption(option: Int): String? = when (option) {
        OPTION_PROTECTION -> "barrel lid"
        OPTION_GLAMOUR -> "barrel hoop earring"
        OPTION_VIGOR -> "bankruptcy barrel"
        else -> null
    }

    fun optionFor(resultName: String): Int? = when (resultName) {
        "barrel lid" -> OPTION_PROTECTION
        "barrel hoop earring" -> OPTION_GLAMOUR
        "bankruptcy barrel" -> OPTION_VIGOR
        else -> null
    }

    fun availableBarrelItem(resultName: String, prefs: Preferences?): Boolean {
        if (prefs == null) return false
        if (!prefs.getBoolean("barrelShrineUnlocked", false)) return false
        if (prefs.getBoolean("_barrelPrayer", false)) return false
        return when (resultName) {
            "barrel lid" -> !prefs.getBoolean("prayedForProtection", false)
            "barrel hoop earring" -> !prefs.getBoolean("prayedForGlamour", false)
            "bankruptcy barrel" -> !prefs.getBoolean("prayedForVigor", false)
            else -> false
        }
    }

    fun applySuccessPrefs(resultName: String, prefs: Preferences) {
        prefs.setBoolean("_barrelPrayer", true)
        when (resultName) {
            "barrel lid" -> prefs.setBoolean("prayedForProtection", true)
            "barrel hoop earring" -> prefs.setBoolean("prayedForGlamour", true)
            "bankruptcy barrel" -> prefs.setBoolean("prayedForVigor", true)
        }
    }

    fun applyPrayerSuccess(option: Int, prefs: Preferences) {
        val resultName = resultNameForOption(option)
        if (resultName != null) {
            applySuccessPrefs(resultName, prefs)
        } else if (option == OPTION_BUFF) {
            prefs.setBoolean("_barrelPrayer", true)
        }
    }
}
