package net.sourceforge.kolmafia.quest

/**
 * Desktop [QuestManager.handleMountainsChange] Melvin shirt quest.
 */
object MelvinShirtSync {

    const val LETTER_FOR_MELVIGN = 7268
    const val PROFESSOR_WHAT_GARMENT = 7269

    fun applyFromVisit(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (questDatabase == null) return false
        val location = url.orEmpty()
        var changed = false
        if (location.contains("action=mts_melvin", ignoreCase = true)) {
            if (html.contains("I saw this awesome T-shirt") ||
                html.contains("haven't you fougnd my T-shirt yet")
            ) {
                questDatabase.setProgress(Quest.SHIRT, QuestDatabase.STARTED)
                consumeItem(LETTER_FOR_MELVIGN, 1)
                changed = true
            } else if (html.contains("I dogn't have a torso.")) {
                questDatabase.setProgress(Quest.SHIRT, QuestDatabase.FINISHED)
                consumeItem(PROFESSOR_WHAT_GARMENT, 1)
                changed = true
            }
        }
        if (html.contains("Melvin's Comic Shop")) {
            questDatabase.setQuestIfBetter(Quest.SHIRT, QuestDatabase.STARTED)
            changed = true
        } else if (html.contains("The Thinknerd Warehouse")) {
            questDatabase.setProgress(Quest.SHIRT, QuestDatabase.FINISHED)
            changed = true
        }
        return changed
    }
}
