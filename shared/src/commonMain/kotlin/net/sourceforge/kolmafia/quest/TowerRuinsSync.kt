package net.sourceforge.kolmafia.quest

/**
 * Desktop [QuestManager.handleTowerRuinsChange] Fernswarthy Tower Ruins EGO NCs.
 */
object TowerRuinsSync {

    const val TOWER_RUINS = 22

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
        if (area != TOWER_RUINS) return false
        if (questDatabase.getProgress(Quest.EGO) == QuestDatabase.FINISHED) return false
        when {
            html.contains("Take a Dusty Look!") ->
                questDatabase.setQuestIfBetter(Quest.EGO, "step6")
            html.contains("Into the Maw of Deepness") ->
                questDatabase.setQuestIfBetter(Quest.EGO, "step5")
            html.contains("Staring into Nothing") ->
                questDatabase.setQuestIfBetter(Quest.EGO, "step4")
            else ->
                questDatabase.setQuestIfBetter(Quest.EGO, "step3")
        }
        return true
    }
}
