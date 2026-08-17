package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleHiddenCityChange] place visit hooks for Hidden City zone prefs.
 */
object HiddenCityVisitSync {

    private val ZONE_UNLOCKS = listOf(
        341 to "hiddenApartmentProgress",
        342 to "hiddenHospitalProgress",
        343 to "hiddenOfficeProgress",
        344 to "hiddenBowlingAlleyProgress",
    )

    fun applyFromVisit(
        url: String?,
        html: String,
        preferences: Preferences?,
        ascensionNumber: Int = 0,
    ): Boolean {
        if (preferences == null) return false
        val location = url.orEmpty()
        if (!location.contains("whichplace=hiddencity", ignoreCase = true) &&
            !html.contains("snarfblat=341") &&
            !html.contains("snarfblat=342") &&
            !html.contains("snarfblat=343") &&
            !html.contains("snarfblat=344") &&
            !html.contains("whichshop=hiddentavern")
        ) {
            if (!location.contains("hiddencity", ignoreCase = true) &&
                !location.contains("hiddentavern", ignoreCase = true)
            ) {
                return false
            }
        }
        var changed = false
        for ((snarf, pref) in ZONE_UNLOCKS) {
            if (html.contains("snarfblat=$snarf") && preferences.getInt(pref, 0) == 0) {
                preferences.setInt(pref, 1)
                changed = true
            }
        }
        if (html.contains("whichshop=hiddentavern") ||
            location.contains("whichshop=hiddentavern", ignoreCase = true)
        ) {
            if (preferences.getInt("hiddenTavernUnlock", -1) != ascensionNumber) {
                preferences.setInt("hiddenTavernUnlock", ascensionNumber)
                changed = true
            }
        }
        return changed
    }
}
