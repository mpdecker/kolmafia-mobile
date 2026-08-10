package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ItemDatabase

/** Parses desktop mood predefined uneffect action strings into [UneffectAction]. */
object MoodUneffectActionParser {

    fun parse(action: String, ctx: UneffectActionContext): UneffectAction? {
        val trimmed = action.trim()
        if (trimmed.isEmpty()) return null

        when {
            trimmed.equals("hottub", ignoreCase = true) -> {
                return if (UneffectActionResolver.canUseHotTub(ctx)) {
                    UneffectAction.HotTub
                } else {
                    null
                }
            }
            trimmed.startsWith("cast ", ignoreCase = true) ||
                trimmed.startsWith("skill ", ignoreCase = true) -> {
                val skillName = parseSkillFromCastAction(trimmed)
                if (skillName.isNotEmpty() && ctx.canCastSkill(skillName)) {
                    return UneffectAction.CastSkill(skillName)
                }
                return null
            }
            trimmed.startsWith("use ", ignoreCase = true) -> {
                val itemId = parseItemFromUseAction(trimmed) ?: return null
                return UneffectAction.UseItem(itemId)
            }
        }
        return null
    }

    internal fun parseSkillFromCastAction(action: String): String {
        val prefixLen = when {
            action.startsWith("cast ", ignoreCase = true) -> 5
            action.startsWith("buff ", ignoreCase = true) -> 5
            else -> 6
        }
        var params = action.substring(prefixLen).trim()
        if (params.isEmpty()) return ""

        if (params[0].isDigit()) {
            val spaceIndex = params.indexOf(' ')
            if (spaceIndex >= 0) {
                params = params.substring(spaceIndex + 1).trim()
            }
        }

        val caretIndex = params.indexOf(" ^ ")
        if (caretIndex >= 0) {
            params = params.substring(0, caretIndex).trim()
        }
        return params
    }

    /** Desktop mood trigger cast prefix count (`cast 2 Skill`). */
    fun parseCastCount(action: String): Int {
        val prefixLen = when {
            action.startsWith("cast ", ignoreCase = true) -> 5
            action.startsWith("buff ", ignoreCase = true) -> 5
            action.startsWith("skill ", ignoreCase = true) -> 6
            else -> return 1
        }
        val params = action.substring(prefixLen).trim()
        if (params.isEmpty() || !params[0].isDigit()) return 1
        val spaceIndex = params.indexOf(' ')
        if (spaceIndex < 0) return 1
        return params.substring(0, spaceIndex).trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
    }

    /** Desktop mood trigger use prefix count (`use 2 item`). */
    fun parseUseCount(action: String): Int {
        if (!action.startsWith("use ", ignoreCase = true)) return 1
        var params = action.substring(4).trim()
        if (params.startsWith('*')) {
            val spaceIndex = params.indexOf(' ')
            if (spaceIndex >= 0) {
                params = params.substring(spaceIndex + 1).trim()
            }
        }
        if (params.isEmpty() || !params[0].isDigit()) return 1
        val spaceIndex = params.indexOf(' ')
        if (spaceIndex < 0) return 1
        return params.substring(0, spaceIndex).trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
    }

    internal fun parseItemFromUseAction(action: String): Int? {
        Regex("""\[(\d+)\]""").find(action)?.let { match ->
            return match.groupValues[1].toIntOrNull()
        }
        var params = action.substring(4).trim()
        if (params.startsWith('*')) {
            val spaceIndex = params.indexOf(' ')
            if (spaceIndex >= 0) {
                params = params.substring(spaceIndex + 1).trim()
            }
        }
        if (params.isNotEmpty() && params[0].isDigit()) {
            val spaceIndex = params.indexOf(' ')
            if (spaceIndex >= 0) {
                params = params.substring(spaceIndex + 1).trim()
            }
        }
        if (params.isEmpty()) return null
        return ItemDatabase.getById(params.toIntOrNull() ?: -1)?.id
            ?: ItemDatabase.getByName(params)?.id
    }
}
