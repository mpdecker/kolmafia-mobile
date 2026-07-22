package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object QuestLogConsequenceDatabase {

    private val rules = mutableListOf<ConsequenceRule>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/consequences.txt").decodeToString()
        rules.clear()
        rules.addAll(parse(text))
        loaded = true
    }

    fun rules(): List<ConsequenceRule> = rules.toList()

    /** Test-only: parse text without file I/O. */
    internal fun parseForTest(text: String): List<ConsequenceRule> = parse(text)

    /** Test-only: inject parsed rules without file I/O. */
    internal fun injectForTest(parsed: List<ConsequenceRule>) {
        rules.clear()
        rules.addAll(parsed)
        loaded = true
    }

    private fun parse(text: String): List<ConsequenceRule> {
        val result = mutableListOf<ConsequenceRule>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue
            val parts = line.split('\t')
            if (parts.size < 4 || parts[0] != "QUEST_LOG") continue
            val spec = parts[1]
            val regexText = parts[2]
            val actionTexts = parts.drop(3)
            val actions = actionTexts.mapNotNull { ConsequenceActionParser.parseAction(it) }
            if (actions.isEmpty()) continue
            result.add(
                ConsequenceRule(
                    spec = spec,
                    pattern = Regex(regexText),
                    actions = actions,
                ),
            )
        }
        return result
    }
}
