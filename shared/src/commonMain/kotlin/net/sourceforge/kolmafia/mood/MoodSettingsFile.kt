package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap

/** Desktop `{username}_moods.txt` parse/serialize helpers. */
object MoodSettingsFile {

    private const val BUFF_MINIMUM_TURNS = 5

    fun seededLibrary(): Map<String, Mood> = linkedMapOf(
        "apathetic" to Mood("apathetic"),
        "default" to Mood("default"),
    )

    fun parse(text: String): Map<String, Mood> {
        val moods = seededLibrary().toMutableMap()
        var current: Mood? = null

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("[")) {
                val closeBracket = line.indexOf(']')
                if (closeBracket < 0) continue
                val header = line.substring(1, closeBracket).trim()
                val (name, parentNames) = Mood.parseName(header)
                if (name.isEmpty()) continue
                current = Mood(name, emptyList(), parentNames, emptyList())
                moods[name] = current
                continue
            }

            val mood = current ?: continue
            val parsed = parseTriggerLine(line) ?: continue
            current = mood.copy(
                triggers = mood.triggers + parsed.buffTriggers,
                removalTriggers = mood.removalTriggers + parsed.removalTriggers,
            )
            moods[mood.name] = current
        }

        return moods
    }

    fun serialize(moods: Collection<Mood>): String =
        moods
            .filter { it.name.isNotEmpty() }
            .sortedBy { it.displayName() }
            .joinToString("\n") { moodToSettingString(it) }
            .let { if (it.isEmpty()) "" else it + "\n" }

    fun moodToSettingString(mood: Mood): String {
        if (mood.name.isEmpty()) return ""
        val lines = buildList {
            add("[ ${mood.displayName()} ]")
            for (trigger in mood.triggers) {
                add("lose_effect ${trigger.effectName} => cast ${trigger.skillName}")
            }
            val buffCastKeys = mood.triggers.map { buffCastKey(it.effectName, it.skillName) }.toSet()
            for (trigger in mood.removalTriggers) {
                val castSkill = castSkillName(trigger.action)
                if (trigger.type == MoodRemovalTriggerType.LOSE_EFFECT &&
                    castSkill != null &&
                    buffCastKeys.contains(buffCastKey(trigger.effectName, castSkill))
                ) {
                    continue
                }
                add(formatRemovalLine(trigger))
            }
        }
        return lines.joinToString("\n")
    }

    internal fun parseTriggerLine(line: String): ParsedTriggerLine? {
        val removal = MoodRemovalTriggerParser.parseLine(line) ?: return null
        val buffTriggers = deriveBuffTriggers(removal)
        return ParsedTriggerLine(buffTriggers, listOf(removal))
    }

    internal fun deriveBuffTriggers(removal: MoodRemovalTrigger): List<MoodTrigger> {
        if (removal.type != MoodRemovalTriggerType.LOSE_EFFECT) return emptyList()
        val skillName = castSkillName(removal.action) ?: return emptyList()
        val mappedEffect = UneffectSkillEffectMap.skillToEffect(skillName) ?: return emptyList()
        if (!mappedEffect.equals(removal.effectName, ignoreCase = true)) return emptyList()
        val skill = SkillDefinitionDatabase.getByName(skillName) ?: return emptyList()
        val effect = EffectDatabase.getByName(removal.effectName)
            ?: EffectDatabase.getById(removal.effectId)
            ?: return emptyList()
        return listOf(
            MoodTrigger(
                effectId = effect.id,
                effectName = effect.name,
                skillId = skill.id,
                skillName = skill.name,
                minimumTurns = BUFF_MINIMUM_TURNS,
            ),
        )
    }

    private fun formatRemovalLine(trigger: MoodRemovalTrigger): String =
        when (trigger.type) {
            MoodRemovalTriggerType.UNCONDITIONAL ->
                "${trigger.typeWireName()} => ${trigger.action}"
            else ->
                "${trigger.typeWireName()} ${trigger.effectName} => ${trigger.action}"
        }

    private fun castSkillName(action: String): String? {
        var params = action.trim()
        if (!params.startsWith("cast", ignoreCase = true)) return null
        params = params.substring(4).trim()
        if (params.isEmpty()) return null
        if (params.first().isDigit()) {
            val spaceIndex = params.indexOf(' ')
            if (spaceIndex < 0) return null
            params = params.substring(spaceIndex + 1).trim()
        }
        if (params.contains(" ^ ")) {
            params = params.substring(0, params.indexOf(" ^ "))
        }
        return params.trim().ifEmpty { null }
    }

    private fun buffCastKey(effectName: String, skillName: String): String =
        "${effectName.lowercase()}|${skillName.lowercase()}"

    internal data class ParsedTriggerLine(
        val buffTriggers: List<MoodTrigger>,
        val removalTriggers: List<MoodRemovalTrigger>,
    )
}
