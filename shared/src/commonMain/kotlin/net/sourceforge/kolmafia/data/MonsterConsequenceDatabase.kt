package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Desktop consequences.txt MONSTER rows — disambiguate ambiguous boss names from fight HTML. */
@OptIn(ExperimentalResourceApi::class)
object MonsterConsequenceDatabase {

    private val rulesByMonsterName = linkedMapOf<String, MutableList<ConsequenceRule>>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/consequences.txt").decodeToString()
        rulesByMonsterName.clear()
        for ((monsterName, rules) in parse(text)) {
            rulesByMonsterName[monsterName] = rules.toMutableList()
        }
        loaded = true
    }

    fun rulesForMonster(monsterName: String): List<ConsequenceRule> {
        rulesByMonsterName[monsterName]?.let { return it.toList() }
        val lower = monsterName.lowercase()
        return rulesByMonsterName.entries
            .firstOrNull { it.key.equals(lower, ignoreCase = true) }
            ?.value
            .orEmpty()
    }

    internal fun parseForTest(text: String): Map<String, List<ConsequenceRule>> = parse(text)

    internal fun injectForTest(parsed: Map<String, List<ConsequenceRule>>) {
        rulesByMonsterName.clear()
        for ((monsterName, rules) in parsed) {
            rulesByMonsterName[monsterName] = rules.toMutableList()
        }
        loaded = true
    }

    internal fun resetForTest() {
        rulesByMonsterName.clear()
        loaded = false
    }

    private fun parse(text: String): Map<String, List<ConsequenceRule>> {
        val result = linkedMapOf<String, MutableList<ConsequenceRule>>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue
            val parts = line.split('\t')
            if (parts.size < 4 || parts[0] != "MONSTER") continue

            val spec = parts[1]
            val regexText = parts[2]
            val actionTexts = parts.drop(3)
            if (regexText.isEmpty()) continue

            val actions = actionTexts.mapNotNull { ConsequenceActionParser.parseAction(it) }
            if (actions.none { it is ConsequenceAction.ReturnReplacement }) continue

            val rule = ConsequenceRule(
                spec = spec,
                pattern = Regex(regexText),
                actions = actions,
            )
            result.getOrPut(spec) { mutableListOf() }.add(rule)
        }
        return result
    }
}
