package net.sourceforge.kolmafia.adventure

/**
 * The request-shaped part of one FightRequest turn.
 *
 * KoL's fight endpoint uses the same URL for all actions; keeping the action
 * typed prevents callers from having to manufacture form fields and makes
 * action logging deterministic on every KMP target.
 */
enum class FightActionKind {
    ATTACK,
    SKILL,
    ITEM,
    RUNAWAY,
    STEAL,
    MACRO,
    ABORT,
}

data class FightAction(
    val kind: FightActionKind,
    val skillId: Int = 0,
    val itemId: Int = 0,
    val itemId2: Int = 0,
    val macroText: String = "",
) {
    fun formFields(): Map<String, String> = when (kind) {
        FightActionKind.ATTACK -> mapOf("action" to "attack")
        FightActionKind.SKILL -> mapOf(
            "action" to "skill",
            "whichskill" to skillId.toString(),
        )
        FightActionKind.ITEM -> buildMap {
            put("action", "useitem")
            put("whichitem", itemId.toString())
            if (itemId2 > 0) put("whichitem2", itemId2.toString())
        }
        FightActionKind.RUNAWAY -> mapOf("action" to "runaway")
        FightActionKind.STEAL -> mapOf("action" to "steal")
        FightActionKind.MACRO -> mapOf(
            "action" to "macro",
            "macrotext" to macroText,
        )
        FightActionKind.ABORT -> mapOf("action" to "abort")
    }

    fun shortName(): String = when (kind) {
        FightActionKind.ATTACK -> "attack"
        FightActionKind.SKILL -> "skill$skillId"
        FightActionKind.ITEM -> if (itemId2 > 0) "$itemId,$itemId2" else itemId.toString()
        FightActionKind.RUNAWAY -> "runaway"
        FightActionKind.STEAL -> "steal"
        FightActionKind.MACRO -> "macro"
        FightActionKind.ABORT -> "abort"
    }

    companion object {
        fun attack() = FightAction(FightActionKind.ATTACK)
        fun skill(id: Int) = FightAction(FightActionKind.SKILL, skillId = id)
        fun item(id: Int, secondId: Int = 0) =
            FightAction(FightActionKind.ITEM, itemId = id, itemId2 = secondId)
        fun runaway() = FightAction(FightActionKind.RUNAWAY)
        fun steal() = FightAction(FightActionKind.STEAL)
        fun macro(text: String) = FightAction(FightActionKind.MACRO, macroText = text)
        fun abort() = FightAction(FightActionKind.ABORT)

        /** Parses the compact action names used by CCS/macros. */
        fun parse(value: String): FightAction? {
            val action = value.trim()
            return when {
                action.equals("attack", ignoreCase = true) -> attack()
                action.equals("runaway", ignoreCase = true) -> runaway()
                action.equals("steal", ignoreCase = true) -> steal()
                action.equals("abort", ignoreCase = true) -> abort()
                action.startsWith("skill", ignoreCase = true) ->
                    action.substring(5).trim().toIntOrNull()?.let(::skill)
                action.startsWith("macro ", ignoreCase = true) ->
                    macro(action.substring(6))
                else -> {
                    val ids = action.split(',').mapNotNull { it.trim().toIntOrNull() }
                    ids.firstOrNull()?.let { item(it, ids.getOrNull(1) ?: 0) }
                }
            }
        }
    }
}
