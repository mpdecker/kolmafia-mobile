package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Desktop consequences.txt QUEST_LOG row — applied to questlog.php?which=3. */
data class QuestLogConsequenceRule(
    val spec: String,
    val pattern: Regex,
    val actions: List<QuestLogConsequenceAction>,
)

sealed class QuestLogConsequenceAction {
    data class SetString(val key: String, val groupIndex: Int) : QuestLogConsequenceAction()
    data class SetLiteral(val key: String, val value: String) : QuestLogConsequenceAction()
    data class SetBoolean(val key: String, val value: Boolean) : QuestLogConsequenceAction()
    data class SetAscensions(val key: String) : QuestLogConsequenceAction()
}

@OptIn(ExperimentalResourceApi::class)
object QuestLogConsequenceDatabase {

    private val rules = mutableListOf<QuestLogConsequenceRule>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/consequences.txt").decodeToString()
        rules.clear()
        rules.addAll(parse(text))
        loaded = true
    }

    fun rules(): List<QuestLogConsequenceRule> = rules.toList()

    /** Test-only: parse text without file I/O. */
    internal fun parseForTest(text: String): List<QuestLogConsequenceRule> = parse(text)

    /** Test-only: inject parsed rules without file I/O. */
    internal fun injectForTest(parsed: List<QuestLogConsequenceRule>) {
        rules.clear()
        rules.addAll(parsed)
        loaded = true
    }

    private fun parse(text: String): List<QuestLogConsequenceRule> {
        val result = mutableListOf<QuestLogConsequenceRule>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue
            val parts = line.split('\t')
            if (parts.size < 4 || parts[0] != "QUEST_LOG") continue
            val spec = parts[1]
            val regexText = parts[2]
            val actionTexts = parts.drop(3)
            if (actionTexts.any { it.contains('[') }) continue
            val actions = actionTexts.mapNotNull { parseAction(it) }
            if (actions.isEmpty()) continue
            result.add(
                QuestLogConsequenceRule(
                    spec = spec,
                    pattern = Regex(regexText),
                    actions = actions,
                ),
            )
        }
        return result
    }

    private fun parseAction(action: String): QuestLogConsequenceAction? {
        val eq = action.indexOf('=')
        if (eq <= 0) return null
        val key = action.substring(0, eq).trim()
        val value = action.substring(eq + 1).trim()
        if (value == "ascensions") return QuestLogConsequenceAction.SetAscensions(key)
        if (value == "true") return QuestLogConsequenceAction.SetBoolean(key, true)
        if (value == "false") return QuestLogConsequenceAction.SetBoolean(key, false)
        val groupMatch = Regex("""^\$(\d+)$""").matchEntire(value) ?: return QuestLogConsequenceAction.SetLiteral(key, value)
        return QuestLogConsequenceAction.SetString(key, groupMatch.groupValues[1].toInt())
    }
}
