package net.sourceforge.kolmafia.quest

/** Quest step bumps from combat win/loss and item drops. */
object QuestFightRules {

    const val VOLCANO_MAP_ID = 3291

    fun applyCombat(
        questDatabase: QuestDatabase,
        monster: String,
        won: Boolean,
        itemsGained: List<String> = emptyList(),
    ): Boolean {
        var advanced = false
        if (monster.isNotBlank()) {
            val step = if (won) winStep(monster) else lossStep(monster)
            if (step != null && advance(questDatabase, Quest.NEMESIS, step)) advanced = true
        }
        if (itemsGained.any { it.contains("volcano map", ignoreCase = true) }) {
            if (advance(questDatabase, Quest.NEMESIS, "step25")) advanced = true
        }
        return advanced
    }

    private fun winStep(monster: String): String? = when (monster.lowercase()) {
        "menacing thug" -> "step19"
        "mob penguin hitman" -> "step21"
        "hunting seal", "turtle trapper", "evil spaghetti cult assassin",
        "béarnaise zombie", "flock of seagulls", "mariachi bandolero" -> "step23"
        else -> null
    }

    private fun lossStep(monster: String): String? = when (monster.lowercase()) {
        "menacing thug" -> "step18"
        "mob penguin hitman" -> "step20"
        "hunting seal", "turtle trapper", "evil spaghetti cult assassin",
        "béarnaise zombie", "flock of seagulls", "mariachi bandolero" -> "step22"
        "argarggagarg the dire hellseal", "safari jack, small-game hunter",
        "yakisoba the executioner", "heimandatz, nacho golem", "jocko homo",
        "the mariachi with no name" -> "step24"
        else -> null
    }

    private fun advance(questDatabase: QuestDatabase, quest: Quest, step: String): Boolean {
        val current = questDatabase.getProgress(quest)
        if (QuestDatabase.stepOrdinal(step) > QuestDatabase.stepOrdinal(current)) {
            questDatabase.setProgress(quest, step)
            return true
        }
        return false
    }
}
