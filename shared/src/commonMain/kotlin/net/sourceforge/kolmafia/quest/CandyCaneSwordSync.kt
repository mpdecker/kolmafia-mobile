package net.sourceforge.kolmafia.quest

/**
 * Desktop [ChoiceControl] non-HC candy-cane-sword flags (postChoice1 + Lyle visit).
 * Hidden City 780/785/788 stay in [HiddenCityChoiceSync].
 */
object CandyCaneSwordSync {

    const val CANDY_CANE_SWORD = 11363

    fun applyFromChoice(
        choiceId: Int,
        decision: Int,
        preferences: net.sourceforge.kolmafia.preferences.Preferences?,
        html: String = "",
        hasCandyCaneSwordEquipped: Boolean = false,
    ): Boolean {
        if (preferences == null) return false
        val pref = postChoicePref(choiceId, decision)
            ?: lylePref(choiceId, hasCandyCaneSwordEquipped)
            ?: return false
        preferences.setBoolean(pref, true)
        return true
    }

    private fun postChoicePref(choiceId: Int, decision: Int): String? = when {
        choiceId == 4 && decision == 4 -> "_candyCaneSwordSouthOfTheBorder"
        choiceId == 109 && decision == 4 -> "_candyCaneSwordBackAlley"
        choiceId == 127 && decision == 4 -> "_candyCaneSwordPalindome"
        choiceId == 139 && decision == 4 -> "candyCaneSwordWarHippyBait"
        choiceId == 140 && decision == 4 -> "candyCaneSwordWarHippyLine"
        choiceId == 143 && decision == 4 -> "candyCaneSwordWarFratZetas"
        choiceId == 144 && decision == 4 -> "candyCaneSwordWarFratRoom"
        choiceId == 151 && decision == 3 -> "candyCaneSwordFunHouse"
        choiceId == 502 && decision == 5 -> "_candyCaneSwordSpookyForest"
        choiceId == 523 && decision == 5 -> "candyCaneSwordDefiledCranny"
        choiceId == 691 && decision == 4 -> "candyCaneSwordDailyDungeon"
        choiceId == 793 && decision == 5 -> "candyCaneSwordShore"
        choiceId == 876 && decision == 4 -> "_candyCaneSwordHauntedBedroom"
        choiceId == 888 && decision == 4 -> "_candyCaneSwordHauntedLibrary"
        choiceId == 923 && decision == 5 -> "candyCaneSwordBlackForest"
        choiceId == 1062 && decision == 6 -> "_candyCaneSwordOvergrownLot"
        choiceId == 1080 && decision == 2 -> "_candyCaneSwordMadnessBakery"
        choiceId == 855 && decision == 5 -> "candyCaneSwordCopperheadClub"
        else -> null
    }

    private fun lylePref(choiceId: Int, equipped: Boolean): String? =
        if (choiceId == 1309 && equipped) "_candyCaneSwordLyle" else null
}
