package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleZeppelinMobChange] / [QuestManager.handleZeppelinChange]
 * + lighter protestor increments.
 */
object ZeppelinRonSync {

    const val ZEPPELIN_PROTESTORS = 384
    const val RED_ZEPPELIN = 385

    private val LIGHTER_PATTERN =
        Regex("""group of (\d+) nearby protesters do the same""", RegexOption.IGNORE_CASE)

    fun applyFromAdventure(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        adventureId: String? = null,
        won: Boolean = true,
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        val area = adventureId?.toIntOrNull()
            ?: Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(url.orEmpty())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        return when (area) {
            ZEPPELIN_PROTESTORS -> applyProtestors(html, questDatabase, preferences, won)
            RED_ZEPPELIN -> applyRedZeppelin(html, questDatabase)
            else -> false
        }
    }

    fun applyProtestors(
        html: String,
        questDatabase: QuestDatabase,
        preferences: Preferences,
        won: Boolean,
    ): Boolean {
        if (html.contains("mob has cleared out")) {
            questDatabase.setProgress(Quest.RON, "step2")
            return true
        }
        questDatabase.setQuestIfBetter(Quest.RON, "step1")
        if (won) {
            val lighterMatch = LIGHTER_PATTERN.find(html)
            if (lighterMatch != null) {
                val flaming = lighterMatch.groupValues[1].toIntOrNull() ?: 0
                preferences.setInt(
                    "zeppelinProtestors",
                    preferences.getInt("zeppelinProtestors", 0) + flaming + 1,
                )
            } else {
                preferences.setInt(
                    "zeppelinProtestors",
                    preferences.getInt("zeppelinProtestors", 0) + 1,
                )
            }
        }
        return true
    }

    fun applyRedZeppelin(html: String, questDatabase: QuestDatabase): Boolean {
        if (html.contains("sneak aboard the Zeppelin")) {
            questDatabase.setProgress(Quest.RON, "step3")
            return true
        }
        questDatabase.setQuestIfBetter(Quest.RON, "step2")
        return true
    }
}
