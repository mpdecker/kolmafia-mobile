package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] woods demon grove choices 560–569 (562 is a fight).
 */
object WoodsDemonChoiceSync {

    const val VANITY_STONE = 5449
    const val JEALOUSY_STONE = 5451
    const val GLUTTONOUS_STONE = 5453

    const val THORAX = "The Thorax"
    const val BAT_IN_THE_SPATS = "The Bat in the Spats"
    const val TERRIBLE_PINCH = "The Terrible Pinch"
    const val THUGS = "Thug 1 and Thug 2"
    const val MAMMON = "Mammon the Elephant"
    const val SNITCH = "The Large-Bellied Snitch"

    fun apply(
        choiceId: Int,
        decision: Int,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        itemCount: (Int) -> Int = { 0 },
    ): Boolean {
        if (questDatabase == null) return false
        return when (choiceId) {
            560 -> {
                questDatabase.setProgress(
                    Quest.CLUMSINESS,
                    if (decision == 1) QuestDatabase.STARTED else QuestDatabase.UNSTARTED,
                )
                true
            }
            561 -> {
                questDatabase.setProgress(Quest.CLUMSINESS, "step1")
                preferences?.setString(
                    "clumsinessGroveBoss",
                    if (decision == 1) THORAX else BAT_IN_THE_SPATS,
                )
                true
            }
            563 -> {
                if (decision != 1) return false
                preferences?.setString(
                    "clumsinessGroveBoss",
                    if (itemCount(VANITY_STONE) > 0) THORAX else BAT_IN_THE_SPATS,
                )
                questDatabase.setProgress(Quest.CLUMSINESS, "step3")
                true
            }
            564 -> {
                questDatabase.setProgress(
                    Quest.MAELSTROM,
                    if (decision == 1) QuestDatabase.STARTED else QuestDatabase.UNSTARTED,
                )
                true
            }
            565 -> {
                questDatabase.setProgress(Quest.MAELSTROM, "step1")
                preferences?.setString(
                    "maelstromOfLoversBoss",
                    if (decision == 1) TERRIBLE_PINCH else THUGS,
                )
                true
            }
            566 -> {
                if (decision != 1) return false
                preferences?.setString(
                    "maelstromOfLoversBoss",
                    if (itemCount(JEALOUSY_STONE) > 0) TERRIBLE_PINCH else THUGS,
                )
                questDatabase.setProgress(Quest.MAELSTROM, "step3")
                true
            }
            567 -> {
                questDatabase.setProgress(
                    Quest.GLACIER,
                    if (decision == 1) QuestDatabase.STARTED else QuestDatabase.UNSTARTED,
                )
                true
            }
            568 -> {
                questDatabase.setProgress(Quest.GLACIER, "step1")
                preferences?.setString(
                    "glacierOfJerksBoss",
                    if (decision == 1) MAMMON else SNITCH,
                )
                true
            }
            569 -> {
                if (decision != 1) return false
                preferences?.setString(
                    "glacierOfJerksBoss",
                    if (itemCount(GLUTTONOUS_STONE) > 0) MAMMON else SNITCH,
                )
                questDatabase.setProgress(Quest.GLACIER, "step3")
                true
            }
            else -> false
        }
    }
}
