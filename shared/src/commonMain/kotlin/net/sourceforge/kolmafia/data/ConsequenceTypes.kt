package net.sourceforge.kolmafia.data

/** Shared consequences.txt rule — used by QUEST_LOG and DESC_ITEM loaders. */
data class ConsequenceRule(
    val spec: String,
    val pattern: Regex,
    val actions: List<ConsequenceAction>,
)

sealed class ConsequenceAction {
    data class SetString(val key: String, val groupIndex: Int) : ConsequenceAction()
    data class SetLiteral(val key: String, val value: String) : ConsequenceAction()
    data class SetBoolean(val key: String, val value: Boolean) : ConsequenceAction()
    data class SetAscensions(val key: String) : ConsequenceAction()
    data class SetExpressionValue(val key: String, val rawValue: String) : ConsequenceAction()
    data class SetItemMods(val key: String) : ConsequenceAction()
    data class SetEffectMods(val key: String) : ConsequenceAction()
    data class ReturnReplacement(val template: String) : ConsequenceAction()
}

typealias QuestLogConsequenceRule = ConsequenceRule
typealias QuestLogConsequenceAction = ConsequenceAction

object ConsequenceActionParser {

    fun parseAction(action: String): ConsequenceAction? {
        if (action.startsWith("\"")) {
            val end = if (action.endsWith("\"")) action.length - 1 else action.length
            return ConsequenceAction.ReturnReplacement(action.substring(1, end))
        }
        val eq = action.indexOf('=')
        if (eq <= 0) return null
        val key = action.substring(0, eq).trim()
        val value = action.substring(eq + 1).trim()
        if (value == "ascensions") return ConsequenceAction.SetAscensions(key)
        if (value == "mods") return ConsequenceAction.SetItemMods(key)
        if (value == "true") return ConsequenceAction.SetBoolean(key, true)
        if (value == "false") return ConsequenceAction.SetBoolean(key, false)
        if (value.contains('[')) return ConsequenceAction.SetExpressionValue(key, value)
        val groupMatch = Regex("""^\$(\d+)$""").matchEntire(value)
            ?: return ConsequenceAction.SetLiteral(key, value)
        return ConsequenceAction.SetString(key, groupMatch.groupValues[1].toInt())
    }
}
