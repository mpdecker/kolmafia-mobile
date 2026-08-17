package net.sourceforge.kolmafia.quest

/**
 * Desktop [QuestManager.handleCell37] Subject 37 Escape quest.
 */
object Cell37EscapeSync {

    const val SUBJECT_37_FILE = 4961
    const val GOTO = 4948
    const val WEREMOOSE_SPIT = 4949
    const val ABOMINABLE_BLUBBER = 4950

    fun applyFromVisit(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        itemCount: (Int) -> Int = { 0 },
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (questDatabase == null) return false
        if (url != null &&
            !url.contains("action=cell37", ignoreCase = true) &&
            !url.contains("cell37", ignoreCase = true)
        ) {
            return false
        }
        var changed = false
        if (html.contains("scientists should have a file on me") ||
            html.contains("Did you find that file yet")
        ) {
            questDatabase.setProgress(Quest.ESCAPE, QuestDatabase.STARTED)
            changed = true
        }
        if (html.contains("pass the folder through")) {
            consumeItem(SUBJECT_37_FILE, 1)
            questDatabase.setProgress(
                Quest.ESCAPE,
                if (itemCount(GOTO) > 0) "step3" else "step2",
            )
            changed = true
        }
        if (html.contains("pass the GOTO through")) {
            consumeItem(GOTO, 1)
            questDatabase.setProgress(
                Quest.ESCAPE,
                if (itemCount(WEREMOOSE_SPIT) > 0) "step5" else "step4",
            )
            changed = true
        }
        if (html.contains("pass the little vial")) {
            consumeItem(WEREMOOSE_SPIT, 1)
            questDatabase.setProgress(
                Quest.ESCAPE,
                if (itemCount(ABOMINABLE_BLUBBER) > 0) "step7" else "step6",
            )
            changed = true
        }
        if (html.contains("hand Subject 37 the glob")) {
            consumeItem(ABOMINABLE_BLUBBER, 1)
            questDatabase.setProgress(Quest.ESCAPE, QuestDatabase.FINISHED)
            changed = true
        }
        return changed
    }
}
