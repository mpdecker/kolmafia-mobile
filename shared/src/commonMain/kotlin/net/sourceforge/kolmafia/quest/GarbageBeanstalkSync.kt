package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleBeanstalkChange] airship / castle Garbage quest writers.
 */
object GarbageBeanstalkSync {

    const val AIRSHIP = 81
    const val CASTLE_BASEMENT = 322
    const val CASTLE_GROUND = 323
    const val CASTLE_TOP = 324

    fun applyFromAdventure(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        ascensionNumber: Int = 0,
        adventureId: String? = null,
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        val area = adventureId?.toIntOrNull()
            ?: Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(url.orEmpty())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        return when (area) {
            AIRSHIP -> {
                questDatabase.setQuestIfBetter(Quest.GARBAGE, "step1")
                if (html.contains("we're looking for the Four Immateria")) {
                    questDatabase.setQuestIfBetter(Quest.GARBAGE, "step2")
                }
                true
            }
            CASTLE_BASEMENT -> {
                questDatabase.setQuestIfBetter(Quest.GARBAGE, "step7")
                if (html.contains("New Area Unlocked") && html.contains("The Ground Floor")) {
                    preferences.setInt("lastCastleGroundUnlock", ascensionNumber)
                    questDatabase.setProgress(Quest.GARBAGE, "step8")
                }
                true
            }
            CASTLE_GROUND -> {
                questDatabase.setQuestIfBetter(Quest.GARBAGE, "step8")
                if (html.contains("New Area Unlocked") && html.contains("The Top Floor")) {
                    preferences.setInt("lastCastleTopUnlock", ascensionNumber)
                    questDatabase.setProgress(Quest.GARBAGE, "step9")
                }
                true
            }
            CASTLE_TOP -> {
                if (!html.contains("You have to learn to walk") &&
                    !html.contains("You'll have to figure out some other way")
                ) {
                    questDatabase.setQuestIfBetter(Quest.GARBAGE, "step9")
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    /** Desktop place=beanstalk beanstalk.gif → GARBAGE step1. */
    fun applyFromPlace(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
    ): Boolean {
        if (questDatabase == null) return false
        if (url != null &&
            !url.contains("whichplace=beanstalk", ignoreCase = true) &&
            !url.contains("place=beanstalk", ignoreCase = true)
        ) {
            return false
        }
        if (!html.contains("otherimages/stalktop/beanstalk.gif")) return false
        questDatabase.setQuestIfBetter(Quest.GARBAGE, "step1")
        return true
    }
}
