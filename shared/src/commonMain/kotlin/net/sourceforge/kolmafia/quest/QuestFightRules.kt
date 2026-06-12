package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Quest step bumps from combat win/loss and item drops. */
object QuestFightRules {

    const val VOLCANO_MAP_ID = 3291
    const val BURNOUTS_DEFEATED_PREF = "burnoutsDefeated"
    private const val BURNOUTS_GOAL = 30

    private val VOLCANIC_CAVE_MARKER = "(volcanic cave)"

    fun applyFightStarted(questDatabase: QuestDatabase, monster: String): Boolean {
        if (monster.isBlank()) return false
        var advanced = false
        if (monster.contains(VOLCANIC_CAVE_MARKER, ignoreCase = true)) {
            advanced = advance(questDatabase, Quest.NEMESIS, "step28") || advanced
        }
        if (monster.equals("cake lord", ignoreCase = true)) {
            advanced = advance(questDatabase, Quest.ARMORER, "step2") || advanced
        }
        return advanced
    }

    fun applyCombat(
        questDatabase: QuestDatabase,
        monster: String,
        won: Boolean,
        itemsGained: List<String> = emptyList(),
        itemIdsGained: List<Int> = emptyList(),
        preferences: Preferences? = null,
    ): Boolean {
        var advanced = false
        if (monster.isNotBlank()) {
            nemesisStep(monster, won)?.let {
                if (advance(questDatabase, Quest.NEMESIS, it)) advanced = true
            }
            citadelStep(monster, won, preferences)?.let {
                if (advance(questDatabase, Quest.CITADEL, it)) advanced = true
            }
            armorerStep(monster, won)?.let {
                if (advance(questDatabase, Quest.ARMORER, it)) advanced = true
            }
        }
        if (itemsGained.any { it.contains("volcano map", ignoreCase = true) } ||
            VOLCANO_MAP_ID in itemIdsGained
        ) {
            if (advance(questDatabase, Quest.NEMESIS, "step25")) advanced = true
        }
        return advanced
    }

    private fun nemesisStep(monster: String, won: Boolean): String? {
        val lower = monster.lowercase()
        if (won) {
            if (lower.contains(VOLCANIC_CAVE_MARKER)) return "step29"
            if (lower.startsWith("the unknown ")) return "step2"
            return when (lower) {
                "clownlord beelzebozo" -> "step6"
                "menacing thug" -> "step19"
                "mob penguin hitman" -> "step21"
                "hunting seal", "turtle trapper", "evil spaghetti cult assassin",
                "béarnaise zombie", "flock of seagulls", "mariachi bandolero" -> "step23"
                else -> null
            }
        }
        return when (lower) {
            "menacing thug" -> "step18"
            "mob penguin hitman" -> "step20"
            "hunting seal", "turtle trapper", "evil spaghetti cult assassin",
            "béarnaise zombie", "flock of seagulls", "mariachi bandolero" -> "step22"
            "argarggagarg the dire hellseal", "safari jack, small-game hunter",
            "yakisoba the executioner", "heimandatz, nacho golem", "jocko homo",
            "the mariachi with no name" -> "step24"
            else -> null
        }
    }

    private fun citadelStep(
        monster: String,
        won: Boolean,
        preferences: Preferences?,
    ): String? {
        if (!won) return null
        val lower = monster.lowercase()
        if (lower == "pair of burnouts") {
            val prefs = preferences ?: return "step4"
            val next = (prefs.getInt(BURNOUTS_DEFEATED_PREF, 0) + 1).coerceAtMost(BURNOUTS_GOAL)
            prefs.setInt(BURNOUTS_DEFEATED_PREF, next)
            return if (next >= BURNOUTS_GOAL) "step4" else null
        }
        return when (lower) {
            "biclops" -> "step5"
            "surprised and annoyed witch", "extremely annoyed witch" -> "step7"
            "elpízo & crosybdis", "elpizo & crosybdis" -> "step10"
            else -> null
        }
    }

    private fun armorerStep(monster: String, won: Boolean): String? {
        if (!won) return null
        return if (monster.equals("cake lord", ignoreCase = true)) "step3" else null
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
