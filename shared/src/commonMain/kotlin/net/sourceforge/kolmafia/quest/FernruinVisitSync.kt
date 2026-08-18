package net.sourceforge.kolmafia.quest

/**
 * Desktop [QuestManager] fernruin.php — unconditional EGO step3 via setQuestIfBetter.
 * Key-gated place=fern remains in [QuestLogSync.applyFernTowerUnlock].
 */
object FernruinVisitSync {

    fun applyFromVisit(
        url: String?,
        questDatabase: QuestDatabase?,
    ): Boolean {
        if (questDatabase == null) return false
        if (url != null && !url.contains("fernruin", ignoreCase = true)) return false
        questDatabase.setQuestIfBetter(Quest.EGO, "step3")
        return true
    }
}
