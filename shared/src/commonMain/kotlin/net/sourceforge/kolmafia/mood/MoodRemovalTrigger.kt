package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.data.EffectDatabase

/** Desktop mood trigger for effect removal/re-acquisition (`gain_effect` / `lose_effect`). */
data class MoodRemovalTrigger(
    val type: MoodRemovalTriggerType,
    val effectId: Int,
    val effectName: String,
    val action: String,
) {
    fun typeWireName(): String = type.wireName

    fun matches(type: String, name: String): Boolean =
        typeWireName().equals(type, ignoreCase = true) &&
            (type == "unconditional" || effectName.equals(name, ignoreCase = true))
}

enum class MoodRemovalTriggerType(val wireName: String) {
    GAIN_EFFECT("gain_effect"),
    LOSE_EFFECT("lose_effect"),
    UNCONDITIONAL("unconditional"),
}

object MoodRemovalTriggerParser {

    fun parseLine(line: String): MoodRemovalTrigger? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        val pieces = trimmed.split(" => ", limit = 2)
        if (pieces.size != 2) return null

        val type = when {
            pieces[0].startsWith("gain_effect") -> MoodRemovalTriggerType.GAIN_EFFECT
            pieces[0].startsWith("lose_effect") -> MoodRemovalTriggerType.LOSE_EFFECT
            pieces[0].startsWith("unconditional") -> MoodRemovalTriggerType.UNCONDITIONAL
            else -> return null
        }

        val action = pieces[1].trim()
        if (type == MoodRemovalTriggerType.UNCONDITIONAL) {
            return MoodRemovalTrigger(type, 0, "", action)
        }

        val spaceIndex = pieces[0].indexOf(' ')
        if (spaceIndex < 0) return null
        val effectName = pieces[0].substring(spaceIndex + 1).trim()
        val effect = EffectDatabase.getByName(effectName) ?: return null
        return MoodRemovalTrigger(type, effect.id, effect.name, action)
    }
}
