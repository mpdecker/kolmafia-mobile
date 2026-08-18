package net.sourceforge.kolmafia.quest

/**
 * Desktop [QuestManager] pandamonium.php — Azazel quest starts on visit.
 */
object PandamoniumVisitSync {

    fun applyFromVisit(
        url: String?,
        questDatabase: QuestDatabase?,
    ): Boolean {
        if (questDatabase == null) return false
        if (url != null && !url.contains("pandamonium.php", ignoreCase = true)) return false
        questDatabase.setQuestIfBetter(Quest.AZAZEL, QuestDatabase.STARTED)
        return true
    }
}
