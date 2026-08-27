package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [MummeryRequest.parseResponse] for choice 1271.
 */
object MummeryChoiceSync {

    const val CHOICE_ID = 1271

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        familiarRace: String = "",
        familiarHasAttribute: (String) -> Boolean = { false },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (!html.contains("You dress")) return false
        if (familiarRace.isBlank()) return false

        var mods = preferences.getString("_mummeryMods", "")
        if (mods.contains(familiarRace)) {
            mods = mods.split(',')
                .filter { it.isNotBlank() && !it.contains(familiarRace) }
                .joinToString(",")
                .let { if (it.isEmpty()) "" else "$it," }
        } else if (mods.isNotEmpty() && !mods.endsWith(",")) {
            mods += ","
        }

        mods += when (decision) {
            1 -> {
                val mod1 = if (familiarHasAttribute("hashands")) 30 else 15
                "Meat Drop: [$mod1*fam($familiarRace)],"
            }
            2 -> {
                val mod1 = if (familiarHasAttribute("haswings")) 6 else 4
                val mod2 = if (familiarHasAttribute("haswings")) 10 else 5
                "MP Regen Min: [$mod1*fam($familiarRace)], MP Regen Max: [$mod2*fam($familiarRace)],"
            }
            3 -> {
                val mod1 = if (familiarHasAttribute("animal")) 4 else 3
                "Experience (Muscle): [$mod1*fam($familiarRace)],"
            }
            4 -> {
                val mod1 = if (familiarHasAttribute("wearsclothes")) 25 else 15
                "Item Drop: [$mod1*fam($familiarRace)],"
            }
            5 -> {
                val mod1 = if (familiarHasAttribute("haseyes")) 4 else 3
                "Experience (Mysticality): [$mod1*fam($familiarRace)],"
            }
            6 -> {
                val mod1 = if (familiarHasAttribute("technological")) 18 else 8
                val mod2 = if (familiarHasAttribute("technological")) 20 else 10
                "HP Regen Min: [$mod1*fam($familiarRace)], HP Regen Max: [$mod2*fam($familiarRace)],"
            }
            7 -> {
                val mod1 = if (familiarHasAttribute("sleaze")) 4 else 2
                "Experience (Moxie): [$mod1*fam($familiarRace)],"
            }
            else -> return false
        }

        preferences.setString(
            "_mummeryUses",
            preferences.getString("_mummeryUses", "") + "$decision,",
        )
        preferences.setString("_mummeryMods", mods)
        return true
    }
}
