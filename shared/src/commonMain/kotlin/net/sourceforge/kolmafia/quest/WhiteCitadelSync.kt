package net.sourceforge.kolmafia.quest

/**
 * Desktop [QuestManager.handleWhiteysGroveChange] / [QuestManager.handleWhiteCitadelChange]
 * adventure NC writers for Whitey's Grove (100) and Road to the White Citadel (413).
 */
object WhiteCitadelSync {

    const val WHITEYS_GROVE = 100
    const val ROAD_TO_WHITE_CITADEL = 413

    fun applyFromAdventure(
        adventureId: String?,
        html: String,
        questDatabase: QuestDatabase?,
        url: String? = null,
    ): Boolean {
        if (questDatabase == null) return false
        val area = adventureId?.toIntOrNull()
            ?: Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(url.orEmpty())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        return when (area) {
            WHITEYS_GROVE -> {
                if (html.contains("It's A Sign!")) {
                    questDatabase.setQuestIfBetter(Quest.CITADEL, "step1")
                    true
                } else {
                    false
                }
            }
            ROAD_TO_WHITE_CITADEL -> {
                if (html.contains("I Guess They Were the Existential Blues Brothers")) {
                    questDatabase.setProgress(Quest.CITADEL, "step3")
                } else {
                    questDatabase.setQuestIfBetter(Quest.CITADEL, "step2")
                }
                true
            }
            else -> false
        }
    }
}
