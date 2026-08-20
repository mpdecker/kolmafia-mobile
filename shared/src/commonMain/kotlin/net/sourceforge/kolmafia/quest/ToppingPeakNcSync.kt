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

    fun applyFromChoice(
        decision: Int,
        html: String,
        optionLabel: String?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null || decision != 1) return false
        val delta = if (html.contains("That's all the horror you can take")) {
            2
        } else {
            2 * findBooPeakLevel(optionLabel)
        }
        val next = (preferences.getInt("booPeakProgress", 0) - delta).coerceAtLeast(0)
        preferences.setInt("booPeakProgress", next)
        return true
    }

    fun findBooPeakLevel(decisionText: String?): Int = when (decisionText) {
        "Ask the Question",
        "Talk to the Ghosts",
        "I Wanna Know What Love Is",
        "Tap Him on the Back",
        "Avert Your Eyes",
        "Approach a Raider",
        "Approach the Argument",
        "Approach the Ghost",
        "Approach the Accountant Ghost",
        "Ask if He's Lost",
        -> 1
        "Enter the Crypt",
        "Try to Talk Some Sense into Them",
        "Put Your Two Cents In",
        "Talk to the Ghost",
        "Tell Them What Werewolves Are",
        "Scream in Terror",
        "Check out the Duel",
        "Watch the Fight",
        "Approach and Reproach",
        "Talk Back to the Robot",
        -> 2
        "Go down the Steps",
        "Make a Suggestion",
        "Tell Them About True Love",
        "Scold the Ghost",
        "Examine the Pipe",
        "Say What?",
        "Listen to the Lesson",
        "Listen in on the Discussion",
        "Point out the Malefactors",
        "Ask for Information",
        -> 3
        "Hurl Some Spells of Your Own",
        "Take Command",
        "Lose Your Patience",
        "Fail to Stifle a Sneeze",
        "Ask for Help",
        "Ask How Duskwalker Basketball Is Played, Against Your Better Judgment",
        "Knights in White Armor, Never Reaching an End",
        "Own up to It",
        "Approach the Poor Waifs",
        "Look Behind You",
        -> 4
        "Read the Book",
        "Join the Conversation",
        "Speak of the Pompatus of Love",
        "Ask What's Going On",
        "Interrupt the Rally",
        "Ask What She's Doing Up There",
        "Point Out an Unfortunate Fact",
        "Try to Talk Sense",
        "Ask for Directional Guidance",
        "What?",
        -> 5
        else -> 0
    }
}
