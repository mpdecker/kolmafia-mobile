package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Shen Copperhead choices 851–854 + charm/talisman ResultProcessor glue.
 */
object ShenSync {

    const val CHOICE_NIGHTCLUB = 851
    const val CHOICE_JERK = 852
    const val CHOICE_HUGE_JERK = 853
    const val CHOICE_WORLDS_BIGGEST = 854

    const val COPPERHEAD_CHARM = 7178
    const val COPPERHEAD_CHARM_RAMPANT = 7186
    const val TALISMAN = 486
    const val FIRST_PIZZA = 7179
    const val LACROSSE_STICK = 7180
    const val EYE_OF_THE_STARS = 7181
    const val STANKARA_STONE = 7182
    const val MURPHYS_FLAG = 7183
    const val SHIELD_OF_BROOK = 7184

    private val SHEN_PATTERN =
        Regex(
            """(?:Bring me|artifact known only as) <b>(.*?)</b>, hidden away for centuries""",
            RegexOption.IGNORE_CASE,
        )

    fun parseQuestItem(html: String): String? =
        SHEN_PATTERN.find(html)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }

    fun applyVisitChoice(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null || choiceId != CHOICE_NIGHTCLUB) return false
        val item = parseQuestItem(html) ?: return false
        preferences.setString("shenQuestItem", item)
        return true
    }

    fun applyPostChoice(
        choiceId: Int,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        dayCount: Int = 0,
        consumeItem: (Int) -> Unit = {},
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        return when (choiceId) {
            CHOICE_NIGHTCLUB -> {
                questDatabase.setProgress(Quest.SHEN, "step1")
                preferences.setInt("shenInitiationDay", dayCount)
                if (preferences.getString("shenQuestItem", "").isEmpty()) {
                    parseQuestItem(html)?.let { preferences.setString("shenQuestItem", it) }
                }
                true
            }
            CHOICE_JERK, CHOICE_HUGE_JERK, CHOICE_WORLDS_BIGGEST -> {
                questDatabase.advanceQuest(Quest.SHEN)
                // Shen has three delivery steps then finished (desktop quest-log table).
                if (choiceId == CHOICE_WORLDS_BIGGEST ||
                    questDatabase.getProgress(Quest.SHEN) == "step4"
                ) {
                    questDatabase.setProgress(Quest.SHEN, QuestDatabase.FINISHED)
                    preferences.setString("shenQuestItem", "")
                } else {
                    parseQuestItem(html)?.let { preferences.setString("shenQuestItem", it) }
                }
                listOf(
                    FIRST_PIZZA,
                    LACROSSE_STICK,
                    EYE_OF_THE_STARS,
                    STANKARA_STONE,
                    MURPHYS_FLAG,
                    SHIELD_OF_BROOK,
                ).forEach(consumeItem)
                true
            }
            else -> false
        }
    }

    fun applyItemAcquire(
        itemId: Int,
        questDatabase: QuestDatabase?,
        hasItemId: (Int) -> Boolean,
        autoCreateTalisman: () -> Unit = {},
    ): Boolean {
        if (questDatabase == null) return false
        var changed = false
        when (itemId) {
            COPPERHEAD_CHARM -> {
                if (hasItemId(COPPERHEAD_CHARM)) {
                    questDatabase.setProgress(Quest.SHEN, QuestDatabase.FINISHED)
                    changed = true
                }
            }
            COPPERHEAD_CHARM_RAMPANT -> {
                if (hasItemId(COPPERHEAD_CHARM_RAMPANT)) {
                    questDatabase.setProgress(Quest.RON, QuestDatabase.FINISHED)
                    changed = true
                }
            }
            TALISMAN -> {
                questDatabase.setQuestIfBetter(Quest.PALINDOME, QuestDatabase.STARTED)
                changed = true
            }
        }
        if (hasItemId(COPPERHEAD_CHARM) && hasItemId(COPPERHEAD_CHARM_RAMPANT)) {
            autoCreateTalisman()
        }
        return changed
    }
}
