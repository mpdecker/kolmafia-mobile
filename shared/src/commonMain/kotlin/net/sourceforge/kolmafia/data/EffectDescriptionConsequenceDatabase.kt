package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Desktop consequences.txt DESC_EFFECT rows — applied to desc_effect.php HTML. */
@OptIn(ExperimentalResourceApi::class)
object EffectDescriptionConsequenceDatabase {

    private val rulesByDescId = mutableMapOf<String, MutableList<ConsequenceRule>>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/consequences.txt").decodeToString()
        rulesByDescId.clear()
        for ((descId, rules) in parse(text)) {
            rulesByDescId[descId] = rules.toMutableList()
        }
        loaded = true
    }

    fun rulesForDescId(descId: String): List<ConsequenceRule> =
        rulesByDescId[descId].orEmpty()

    internal fun parseForTest(text: String): Map<String, List<ConsequenceRule>> = parse(text)

    internal fun injectForTest(parsed: Map<String, List<ConsequenceRule>>) {
        rulesByDescId.clear()
        for ((descId, rules) in parsed) {
            rulesByDescId[descId] = rules.toMutableList()
        }
        loaded = true
    }

    internal fun resetForTest() {
        rulesByDescId.clear()
        loaded = false
    }

    private fun parse(text: String): Map<String, List<ConsequenceRule>> {
        val result = linkedMapOf<String, MutableList<ConsequenceRule>>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue
            val parts = line.split('\t')
            if (parts.size < 4 || parts[0] != "DESC_EFFECT") continue

            val spec = parts[1]
            val regexText = parts[2]
            val actionTexts = parts.drop(3)
            val hasModsAction = actionTexts.any { it.substringAfter('=').trim() == "mods" }
            if (regexText.isEmpty() && !hasModsAction) continue

            val actions = actionTexts.mapNotNull { parseEffectAction(it) }
            if (actions.isEmpty()) continue

            val effect = EffectDatabase.getByName(spec) ?: continue
            val rule = ConsequenceRule(
                spec = spec,
                pattern = if (regexText.isEmpty()) Regex("") else Regex(regexText),
                actions = actions,
            )
            result.getOrPut(effect.descId) { mutableListOf() }.add(rule)
        }
        return result
    }

    private fun parseEffectAction(action: String): ConsequenceAction? {
        val eq = action.indexOf('=')
        if (eq <= 0) return null
        val key = action.substring(0, eq).trim()
        val value = action.substring(eq + 1).trim()
        if (value == "mods") return ConsequenceAction.SetEffectMods(key)
        return ConsequenceActionParser.parseAction(action)
    }
}
