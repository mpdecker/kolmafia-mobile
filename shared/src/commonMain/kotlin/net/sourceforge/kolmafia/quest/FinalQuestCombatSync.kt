package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [QuestManager.updateQuestData] NS contest crowd + FINAL tower monster combat hooks. */
object FinalQuestCombatSync {

    private val CROWD_ADVENTURES = mapOf(
        "ns_01_crowd1" to "nsContestants1",
        "ns_01_crowd2" to "nsContestants2",
        "ns_01_crowd3" to "nsContestants3",
    )

    private val TOPIARY_MONSTERS = setOf(
        "topiary gopher",
        "topiary chihuahua herd",
        "topiary duck",
        "topiary kiwi",
    )

    private val NS_AVATAR_MONSTERS = setOf(
        "naughty sorceress (3)",
        "the avatar of sneaky pete",
        "the avatar of boris",
        "principal mooney",
        "rene c. corman",
        "the avatar of jarlsberg",
        "the rain king",
        "one thousand source agents",
        "jerry bradford, pokéfam world champion",
        "jerry bradford, pokefam world champion",
        "\"blofeld\"",
        "nautomatic sorceress",
        "%alucard%",
    )

    fun applyCombatWin(
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        adventureId: String,
        monster: String,
        responseText: String,
        won: Boolean,
    ): Boolean {
        if (questDatabase == null || preferences == null || !won) return false
        var changed = applyCrowdWin(adventureId, preferences, questDatabase)
        changed = applyTowerMonsterWin(monster, responseText, questDatabase) || changed
        return changed
    }

    fun applyCrowdWin(
        adventureId: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase,
    ): Boolean {
        val prefs = preferences ?: return false
        val counterPref = CROWD_ADVENTURES[adventureId] ?: return false
        val crowd = prefs.getInt(counterPref, 0)
        if (crowd > 0) {
            prefs.setInt(counterPref, crowd - 1)
        }
        if (prefs.getInt("nsContestants1", 0) == 0 &&
            prefs.getInt("nsContestants2", 0) == 0 &&
            prefs.getInt("nsContestants3", 0) == 0
        ) {
            questDatabase.setQuestIfBetter(Quest.FINAL, "step2")
            return true
        }
        return crowd > 0
    }

    fun applyTowerMonsterWin(
        monster: String,
        responseText: String,
        questDatabase: QuestDatabase,
    ): Boolean {
        if (monster.isBlank()) return false
        val lower = monster.trim().lowercase()
        val step = when {
            lower in TOPIARY_MONSTERS -> "step4"
            lower == "wall of skin" -> "step7"
            lower == "wall of meat" &&
                responseText.contains("the stairs to the next floor are clear", ignoreCase = true) -> "step8"
            lower == "wall of bones" -> "step9"
            lower == "your shadow" || lower == "clancy" -> "step11"
            lower in NS_AVATAR_MONSTERS -> "step13"
            lower == "guy made of bees" &&
                responseText.contains("Thwaitgold bee statuette", ignoreCase = true) -> "step13"
            else -> return false
        }
        return advanceFinal(questDatabase, step)
    }

    private fun advanceFinal(questDatabase: QuestDatabase, step: String): Boolean {
        val current = questDatabase.getProgress(Quest.FINAL)
        if (QuestDatabase.stepOrdinal(step) <= QuestDatabase.stepOrdinal(current)) return false
        questDatabase.setProgress(Quest.FINAL, step)
        return true
    }
}
