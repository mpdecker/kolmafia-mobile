package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Parses Naughty Sorceress tower HTML and syncs [Quest.FINAL] progress.
 * Mirrors desktop [SorceressLairManager.parseTower].
 */
object TowerSync {

    private val towerMarkers = listOf(
        "crowd1.gif" to "step1",
        "crowd2.gif" to "step1",
        "crowd3.gif" to "step1",
        "nstower_regdesk.gif" to QuestDatabase.STARTED,
        "nstower_courtyard.gif" to "step3",
        "nstower_hedgemaze.gif" to "step4",
        "nstower_towerdoor.gif" to "step5",
        "nstower_tower1.gif" to "step6",
        "nstower_tower2.gif" to "step7",
        "nstower_tower3.gif" to "step8",
        "nstower_tower4.gif" to "step9",
        "nstower_tower5.gif" to "step10",
        "chamberlabel.gif" to "step11",
        "kingprism" to "step12",
        "gash.gif" to QuestDatabase.FINISHED,
    )

    fun containsTowerMarker(html: String): Boolean =
        towerMarkers.any { (marker, _) -> html.contains(marker, ignoreCase = true) }

    fun parseTower(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase == null) return false
        var matchedStep: String? = null
        for ((marker, step) in towerMarkers) {
            if (!html.contains(marker, ignoreCase = true)) continue
            matchedStep = step
            break
        }
        val step = matchedStep ?: return false
        val advanced = advanceIfBetter(questDatabase, Quest.FINAL, step)
        if (questDatabase.isQuestLaterThan(Quest.FINAL, "step1")) {
            preferences?.setInt("nsContestants1", 0)
            preferences?.setInt("nsContestants2", 0)
            preferences?.setInt("nsContestants3", 0)
        }
        return advanced
    }

    private fun advanceIfBetter(questDatabase: QuestDatabase, quest: Quest, step: String): Boolean {
        val current = questDatabase.getProgress(quest)
        if (QuestDatabase.stepOrdinal(step) <= QuestDatabase.stepOrdinal(current)) return false
        questDatabase.setProgress(quest, step)
        return true
    }
}
