package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [PantogramRequest.parseResponse] for choice 1270 — writes `_pantogramModifier`.
 */
object PantogramChoiceSync {

    const val CHOICE_ID = 1270
    const val PANTOGRAM_PANTS_ID = 9574

    private val URL_PATTERN = Regex("""m=(\d)&e=(\d)&s1=(.*?)&s2=(.*?)&s3=(.*)""")

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
        gainItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (!choiceUrl.contains("whichchoice=1270", ignoreCase = true) &&
            !choiceUrl.contains("m=")
        ) {
            // Allow tests to pass only the m/e/s fields
            if (!URL_PATTERN.containsMatchIn(choiceUrl)) return false
        }
        if (!html.contains("You acquire an item")) return false

        gainItem(PANTOGRAM_PANTS_ID, 1)
        val matcher = URL_PATTERN.find(choiceUrl) ?: return true
        val mods = mutableListOf<String>()

        when (matcher.groupValues[1]) {
            "1" -> mods += "Muscle: +10"
            "2" -> mods += "Mysticality: +10"
            "3" -> mods += "Moxie: +10"
        }
        when (matcher.groupValues[2]) {
            "1" -> mods += "Hot Resistance: +2"
            "2" -> mods += "Cold Resistance: +2"
            "3" -> mods += "Spooky Resistance: +2"
            "4" -> mods += "Sleaze Resistance: +2"
            "5" -> mods += "Stench Resistance: +2"
        }

        parseSlot1(matcher.groupValues[3], mods, consumeItem)
        parseSlot2(matcher.groupValues[4], mods, consumeItem)
        parseSlot3(matcher.groupValues[5], mods, consumeItem)

        mods += "Lasts Until Rollover"
        mods += "Last Available: \"2017-11\""
        preferences.setString("_pantogramModifier", mods.joinToString(", "))
        return true
    }

    private fun parseSlot1(slot: String, mods: MutableList<String>, consume: (Int, Int) -> Unit) {
        when {
            slot.startsWith("-1") -> mods += "Maximum HP: +40"
            slot.startsWith("-2") -> mods += "Maximum MP: +20"
            slot.startsWith("464") -> {
                mods += "HP Regen Min: 5"
                mods += "HP Regen Max: 10"
                consume(464, 1)
            }
            slot.startsWith("830") -> {
                mods += "HP Regen Min: 5"
                mods += "HP Regen Max: 15"
                consume(830, 1)
            }
            slot.startsWith("2438") -> {
                mods += "HP Regen Min: 10"
                mods += "HP Regen Max: 20"
                consume(2438, 1)
            }
            slot.startsWith("1658") -> {
                mods += "MP Regen Min: 5"
                mods += "MP Regen Max: 10"
                consume(1658, 1)
            }
            slot.startsWith("5789") -> {
                mods += "MP Regen Min: 5"
                mods += "MP Regen Max: 15"
                consume(5789, 1)
            }
            slot.startsWith("8455") -> {
                mods += "MP Regen Min: 10"
                mods += "MP Regen Max: 20"
                consume(8455, 1)
            }
            slot.startsWith("705") -> {
                mods += "Mana Cost: -3"
                consume(705, 1)
            }
        }
    }

    private fun parseSlot2(slot: String, mods: MutableList<String>, consume: (Int, Int) -> Unit) {
        when {
            slot.startsWith("-1") -> mods += "Weapon Damage: +20"
            slot.startsWith("-2") -> mods += "Spell Damage Percent: +20"
            slot.startsWith("173") -> {
                mods += "Meat Drop: +30"
                consume(173, 1)
            }
            slot.startsWith("706") -> {
                mods += "Meat Drop: +60"
                consume(706, 1)
            }
            slot.startsWith("80") -> {
                mods += "Item Drop: +15"
                consume(80, 1)
            }
            slot.startsWith("7338") -> {
                mods += "Item Drop: +30"
                consume(7338, 1)
            }
            slot.startsWith("747") -> {
                mods += "Experience (Muscle): +3"
                consume(747, 3)
            }
            slot.startsWith("559") -> {
                mods += "Experience (Mysticality): +3"
                consume(559, 3)
            }
            slot.startsWith("27") -> {
                mods += "Experience (Moxie): +3"
                consume(27, 3)
            }
            slot.startsWith("7327") -> {
                mods += "Experience Percent (Muscle): +25"
                consume(7327, 5)
            }
            slot.startsWith("7324") -> {
                mods += "Experience Percent (Mysticality): +25"
                consume(7324, 5)
            }
            slot.startsWith("7330") -> {
                mods += "Experience Percent (Moxie): +25"
                consume(7330, 5)
            }
        }
    }

    private fun parseSlot3(slot: String, mods: MutableList<String>, consume: (Int, Int) -> Unit) {
        when {
            slot.startsWith("-1") -> mods += "Combat Rate: -5"
            slot.startsWith("-2") -> mods += "Combat Rate: +5"
            slot.startsWith("70%") -> {
                mods += "Initiative: +50"
                consume(70, 1)
            }
            slot.startsWith("704") -> {
                mods += "Critical Hit Percent: +10"
                consume(704, 1)
            }
            slot.startsWith("865") -> {
                mods += "Familiar Weight: +10"
                consume(865, 11)
            }
            slot.startsWith("6851") -> {
                mods += "Candy Drop: +100"
                consume(6851, 1)
            }
            slot.startsWith("3495") -> {
                mods += "Initiative Penalty: [20*env(underwater)]"
                mods += "Item Drop Penalty: [20*env(underwater)]"
                mods += "Meat Drop Penalty: [20*env(underwater)]"
                consume(3495, 11)
            }
            slot.startsWith("9008") -> {
                mods += "Fishing Skill: +5"
                consume(9008, 1)
            }
            slot.startsWith("1907") -> {
                mods += "Pool Skill: +5"
                consume(1907, 15)
            }
            slot.startsWith("14") -> consume(14, 99)
            slot.startsWith("24") -> {
                mods += "Drops Items"
                consume(24, 1)
            }
        }
    }
}
