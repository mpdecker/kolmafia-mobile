package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Desktop consequences.txt DESC_SKILL rows — applied to desc_skill.php HTML. */
@OptIn(ExperimentalResourceApi::class)
object SkillDescriptionConsequenceDatabase {

    private val rulesBySkillId = mutableMapOf<Int, MutableList<ConsequenceRule>>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/consequences.txt").decodeToString()
        rulesBySkillId.clear()
        for ((skillId, rules) in parse(text)) {
            rulesBySkillId[skillId] = rules.toMutableList()
        }
        loaded = true
    }

    fun rulesForSkillId(skillId: Int): List<ConsequenceRule> =
        rulesBySkillId[skillId].orEmpty()

    internal fun parseForTest(text: String): Map<Int, List<ConsequenceRule>> = parse(text)

    internal fun injectForTest(parsed: Map<Int, List<ConsequenceRule>>) {
        rulesBySkillId.clear()
        for ((skillId, rules) in parsed) {
            rulesBySkillId[skillId] = rules.toMutableList()
        }
        loaded = true
    }

    internal fun resetForTest() {
        rulesBySkillId.clear()
        loaded = false
    }

    private fun parse(text: String): Map<Int, List<ConsequenceRule>> {
        val result = linkedMapOf<Int, MutableList<ConsequenceRule>>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue
            val parts = line.split('\t')
            if (parts.size < 4 || parts[0] != "DESC_SKILL") continue

            val spec = parts[1]
            val regexText = parts[2]
            val actionTexts = parts.drop(3)
            if (regexText.isEmpty()) continue

            val actions = actionTexts.mapNotNull { ConsequenceActionParser.parseAction(it) }
            if (actions.isEmpty()) continue

            val skill = SkillDefinitionDatabase.getByName(spec) ?: continue
            val rule = ConsequenceRule(
                spec = spec,
                pattern = Regex(regexText),
                actions = actions,
            )
            result.getOrPut(skill.id) { mutableListOf() }.add(rule)
        }
        return result
    }
}
