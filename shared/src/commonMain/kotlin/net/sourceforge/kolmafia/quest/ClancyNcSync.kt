package net.sourceforge.kolmafia.quest

/**
 * Desktop [QuestManager] Clancy NC writers: Barroom Brawl / Knob Shaft / Icy Peak.
 */
object ClancyNcSync {

    const val BARROOM_BRAWL = 233
    const val KNOB_SHAFT = 101
    const val ICY_PEAK = 110

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
            BARROOM_BRAWL -> {
                if (html.contains("Jackin' the Jukebox")) {
                    questDatabase.setProgress(Quest.CLANCY, "step1")
                    true
                } else false
            }
            KNOB_SHAFT -> {
                if (html.contains("A Miner Variation")) {
                    questDatabase.setProgress(Quest.CLANCY, "step3")
                    true
                } else false
            }
            ICY_PEAK -> {
                if (html.contains("Mercury Rising")) {
                    questDatabase.setProgress(Quest.CLANCY, "step7")
                    true
                } else false
            }
            else -> false
        }
    }

    /**
     * Desktop [ChoiceControl] minstrel choices 571–577.
     */
    fun applyFromChoice(choiceId: Int, questDatabase: QuestDatabase?): Boolean {
        if (questDatabase == null) return false
        val step = when (choiceId) {
            571 -> QuestDatabase.STARTED
            572 -> "step2"
            573 -> "step4"
            576 -> "step6"
            577 -> "step8"
            else -> return false
        }
        questDatabase.setProgress(Quest.CLANCY, step)
        return true
    }
}
