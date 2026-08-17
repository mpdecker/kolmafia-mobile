package net.sourceforge.kolmafia.quest

/**
 * Desktop [QuestManager.handlePlainsChange] — bean plant + palinlink unlock.
 */
object PlainsVisitSync {

    const val ENCHANTED_BEAN = 186

    fun applyFromVisit(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (questDatabase == null) return false
        if (url != null && !url.contains("whichplace=plains", ignoreCase = true) &&
            !url.contains("plains", ignoreCase = true)
        ) {
            return false
        }
        var changed = false
        if (html.contains("immediately grows into an enormous beanstalk")) {
            consumeItem(ENCHANTED_BEAN, 1)
            questDatabase.setProgress(Quest.GARBAGE, "step1")
            changed = true
        }
        if (html.contains("palinlink.gif")) {
            questDatabase.setQuestIfBetter(Quest.PALINDOME, QuestDatabase.STARTED)
            changed = true
        }
        return changed
    }
}
