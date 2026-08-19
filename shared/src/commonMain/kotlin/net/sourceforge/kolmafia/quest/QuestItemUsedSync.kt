package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.CryptManager

/**
 * Desktop [QuestManager.updateQuestItemUsed] clippers + Dinsey refreshments.
 */
object QuestItemUsedSync {

    const val FINGERNAIL_CLIPPERS = 7831
    const val DINSEY_REFRESHMENTS = 8243
    const val SPIDER_WEB = 27
    const val CAN_LID = 559
    const val KNOB_FIRECRACKER = 747
    const val CA_BASE_PAIR = 4011
    const val CG_BASE_PAIR = 4012
    const val CT_BASE_PAIR = 4013
    const val AG_BASE_PAIR = 4014
    const val AT_BASE_PAIR = 4015
    const val GT_BASE_PAIR = 4016
    const val DAILY_DUNGEON_MALWARE = 9024
    const val BOMB_OF_UNKNOWN_ORIGIN = 9188
    const val COSMIC_BOWLING_BALL = 10891

    private val TOURIST_PATTERN = Regex("""and the (\d+) tourists in front""")

    private val CYRUS_ADJECTIVES = mapOf(
        CA_BASE_PAIR to "stronger",
        CG_BASE_PAIR to "smarter",
        CT_BASE_PAIR to "more attractive",
        AG_BASE_PAIR to "faster",
        AT_BASE_PAIR to "more aggressive",
        GT_BASE_PAIR to "more resilient",
    )

    fun apply(
        itemId: Int,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
        count: Int = 1,
    ): Boolean = when (itemId) {
        FINGERNAIL_CLIPPERS -> applyClippers(html, questDatabase, preferences)
        DINSEY_REFRESHMENTS -> applyDinseyRefreshments(html, questDatabase, preferences)
        CryptManager.EVILOMETER -> CryptManager.examineEvilometer(html, preferences, consumeItem)
        CryptManager.EVIL_EYE -> CryptManager.applyEvilEye(html, count, preferences)
        SPIDER_WEB -> applyVillainLairItem(html, "Three other minions", "_villainLairWebUsed", preferences)
        KNOB_FIRECRACKER -> applyVillainLairItem(html, "three other minions", "_villainLairFirecrackerUsed", preferences)
        CAN_LID -> applyVillainLairItem(html, "three other minions", "_villainLairCanLidUsed", preferences)
        BOMB_OF_UNKNOWN_ORIGIN -> applyZeppelinBomb(html, preferences)
        DAILY_DUNGEON_MALWARE -> applyDailyDungeonMalware(html, preferences)
        COSMIC_BOWLING_BALL -> applyCosmicBowlingBall(html, preferences)
        in CYRUS_ADJECTIVES.keys -> applyCyrusBasePair(itemId, preferences)
        else -> false
    }

    private fun applyClippers(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        if (!html.contains("little sliver of something fingernail-like")) return false
        val next = preferences.getInt("fingernailsClipped", 0) + 1
        preferences.setInt("fingernailsClipped", next)
        if (next >= 23) {
            questDatabase?.setProgress(Quest.CLIPPER, "step1")
        }
        return true
    }

    private fun applyDinseyRefreshments(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        if (html.contains("realize that the box of refreshments is empty") ||
            html.contains("box of snacks is empty")
        ) {
            questDatabase?.setProgress(Quest.WORK_WITH_FOOD, "step1")
            preferences.setInt("dinseyTouristsFed", 30)
            return true
        }
        if (!html.contains("hand out snacks to your opponent")) return false
        var count = 1
        if (html.contains("and the tourist in front")) {
            count++
        } else {
            TOURIST_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { extra ->
                count += extra
            }
        }
        val next = (preferences.getInt("dinseyTouristsFed", 0) + count).coerceAtMost(30)
        preferences.setInt("dinseyTouristsFed", next)
        return true
    }

    private fun applyVillainLairItem(
        html: String,
        phrase: String,
        usedPref: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        if (!html.contains(phrase)) return false
        preferences.setInt(
            "_villainLairProgress",
            preferences.getInt("_villainLairProgress", 0) + 3,
        )
        preferences.setBoolean(usedPref, true)
        return true
    }

    private fun applyZeppelinBomb(html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        if (!html.contains("decide to find something else to protest")) return false
        preferences.setInt(
            "zeppelinProtestors",
            preferences.getInt("zeppelinProtestors", 0) + 10,
        )
        return true
    }

    private fun applyDailyDungeonMalware(html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        if (!html.contains("It's a UNIX system") &&
            !html.contains("You attempt to hack the monster")
        ) {
            return false
        }
        preferences.setBoolean("_dailyDungeonMalwareUsed", true)
        return true
    }

    private fun applyCosmicBowlingBall(html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        preferences.setInt("cosmicBowlingBallReturnCombats", 0)
        if (html.contains("you hurl it down the ancient lanes")) {
            preferences.setInt(
                "hiddenBowlingAlleyProgress",
                preferences.getInt("hiddenBowlingAlleyProgress", 0) + 1,
            )
        }
        return true
    }

    private fun applyCyrusBasePair(itemId: Int, preferences: Preferences?): Boolean {
        val adjective = CYRUS_ADJECTIVES[itemId] ?: return false
        if (preferences == null) return false
        QuestSpecialSync.appendCyrusAdjective(preferences, adjective)
        return true
    }
}
