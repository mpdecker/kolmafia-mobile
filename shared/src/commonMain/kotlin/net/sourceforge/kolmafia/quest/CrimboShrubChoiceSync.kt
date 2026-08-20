package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Shrubberatin' choice 999 —
 * `_shrubDecorated` + shrubTopper/Lights/Garland/Gifts from URL fields.
 */
object CrimboShrubChoiceSync {

    const val CHOICE_ID = 999

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        choiceUrl: String = "",
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null || decision != 1) return false
        preferences.setBoolean("_shrubDecorated", true)
        fieldValue(choiceUrl, "topper")?.let { n ->
            when (n) {
                1 -> preferences.setString("shrubTopper", "Muscle")
                2 -> preferences.setString("shrubTopper", "Mysticality")
                3 -> preferences.setString("shrubTopper", "Moxie")
            }
        }
        fieldValue(choiceUrl, "lights")?.let { n ->
            when (n) {
                1 -> preferences.setString("shrubLights", "prismatic")
                2 -> preferences.setString("shrubLights", "Hot")
                3 -> preferences.setString("shrubLights", "Cold")
                4 -> preferences.setString("shrubLights", "Stench")
                5 -> preferences.setString("shrubLights", "Spooky")
                6 -> preferences.setString("shrubLights", "Sleaze")
            }
        }
        fieldValue(choiceUrl, "garland")?.let { n ->
            when (n) {
                1 -> preferences.setString("shrubGarland", "HP")
                2 -> preferences.setString("shrubGarland", "PvP")
                3 -> preferences.setString("shrubGarland", "blocking")
            }
        }
        fieldValue(choiceUrl, "gift")?.let { n ->
            when (n) {
                1 -> preferences.setString("shrubGifts", "yellow")
                2 -> preferences.setString("shrubGifts", "meat")
                3 -> preferences.setString("shrubGifts", "gifts")
            }
        }
        return true
    }

    private fun fieldValue(url: String, name: String): Int? =
        Regex("""$name=(\d)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
}
