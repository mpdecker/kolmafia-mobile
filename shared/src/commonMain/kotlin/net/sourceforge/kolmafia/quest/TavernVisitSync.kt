package net.sourceforge.kolmafia.quest

/**
 * Desktop [TavernManager.handleTavernChange] + [TavernRequest.parseResponse] barkeep
 * RAT finish / step1 writers.
 */
object TavernVisitSync {

    private val FINISH_STRINGS = listOf(
        "have a few drinks on the house",
        "something that wasn't booze",
        "a round on the house",
    )

    private const val BARKEEP_SWILL =
        "grab some mugs and pour yourself some tavern swill"

    fun applyFromVisit(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
    ): Boolean {
        if (questDatabase == null) return false
        if (url != null && !url.contains("tavern.php", ignoreCase = true)) return false
        if (FINISH_STRINGS.any { html.contains(it) }) {
            questDatabase.setProgress(Quest.RAT, QuestDatabase.FINISHED)
            return true
        }
        if (url != null && url.contains("place=barkeep", ignoreCase = true)) {
            if (html.contains(BARKEEP_SWILL)) {
                questDatabase.setProgress(Quest.RAT, QuestDatabase.FINISHED)
            } else {
                questDatabase.setQuestIfBetter(Quest.RAT, "step1")
            }
            return true
        }
        return false
    }
}
