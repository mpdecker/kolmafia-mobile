package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.updateQuestItemUsed] clippers + Dinsey refreshments.
 */
object QuestItemUsedSync {

    const val FINGERNAIL_CLIPPERS = 7831
    const val DINSEY_REFRESHMENTS = 8243

    private val TOURIST_PATTERN = Regex("""and the (\d+) tourists in front""")

    fun apply(
        itemId: Int,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean = when (itemId) {
        FINGERNAIL_CLIPPERS -> applyClippers(html, questDatabase, preferences)
        DINSEY_REFRESHMENTS -> applyDinseyRefreshments(html, questDatabase, preferences)
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
}
