package net.sourceforge.kolmafia.quest

/**
 * Desktop [QuestManager.handleBatholeChange] — BAT steps from `bathole_N.gif`.
 */
object BatholeSync {

    private val batholePattern = Regex("""bathole_(\d)\.gif""", RegexOption.IGNORE_CASE)

    fun applyFromVisit(url: String?, html: String, questDatabase: QuestDatabase?): Boolean {
        if (questDatabase == null) return false
        if (url != null &&
            !url.contains("whichplace=bathole", ignoreCase = true) &&
            !url.contains("bathole", ignoreCase = true) &&
            !html.contains("bathole_")
        ) {
            return false
        }
        val image = batholePattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        val status = when (image) {
            1 -> QuestDatabase.STARTED
            2 -> "step1"
            3 -> "step2"
            4 -> "step3"
            5 -> "step4"
            else -> return false
        }
        questDatabase.setQuestIfBetter(Quest.BAT, status)
        return true
    }
}
