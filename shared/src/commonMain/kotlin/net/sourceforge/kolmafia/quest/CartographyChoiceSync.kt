package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] cartography visit stamps (1425, 1427–1434, 1436).
 */
object CartographyChoiceSync {

    private val PREF_BY_CHOICE = mapOf(
        1425 to "lastCartographyFratHouse",
        1427 to "lastCartographyGuanoJunction",
        1428 to "lastCartographyDarkNeck",
        1429 to "lastCartographyDefiledNook",
        1430 to "lastCartographyBooPeak",
        1431 to "lastCartographyCastleTop",
        1432 to "lastCartographyZeppelinProtesters",
        1433 to "lastCartographyFratHouseVerge",
        1434 to "lastCartographyHippyCampVerge",
        1436 to "lastCartographyHauntedBilliards",
    )

    val CHOICE_IDS: Set<Int> = PREF_BY_CHOICE.keys

    fun applyVisit(
        choiceId: Int,
        preferences: Preferences?,
        ascensionNumber: Int,
    ): Boolean {
        if (preferences == null) return false
        val pref = PREF_BY_CHOICE[choiceId] ?: return false
        preferences.setInt(pref, ascensionNumber)
        return true
    }
}
