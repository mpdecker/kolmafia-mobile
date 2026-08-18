package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleCouncilChange] visit glue. Council text matching stays
 * in [net.sourceforge.kolmafia.data.QuestCouncilDatabase.handleCouncilText].
 */
object CouncilVisitSync {

    const val MOSQUITO_LARVA = 275
    const val LARVA_CONSUME =
        "Thanks for the larva, Adventurer. We'll put this to good use."

    fun applyFromVisit(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        level: Int,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        if (url != null && !isCouncilUrl(url)) return false
        preferences.setInt("lastCouncilVisit", level)
        if (html.contains(LARVA_CONSUME)) {
            consumeItem(MOSQUITO_LARVA, 1)
        }
        if (questDatabase.isAtLeast(Quest.MACGUFFIN, QuestDatabase.STARTED)) {
            questDatabase.setQuestIfBetter(Quest.BLACK, QuestDatabase.STARTED)
        }
        return true
    }

    internal fun isCouncilUrl(url: String): Boolean =
        url.contains("council.php", ignoreCase = true) ||
            url.contains("action=expl_council", ignoreCase = true)
}
