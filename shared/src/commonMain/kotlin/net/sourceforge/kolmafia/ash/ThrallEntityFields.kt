package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.PastaThrall

/**
 * Resolves `$thrall[field]` bracket access. Mirrors desktop [ThrallProxy].
 */
internal object ThrallEntityFields {

    fun resolve(
        thrallName: String,
        fieldName: String,
        preferences: Preferences?,
        gameDatabase: GameDatabase?,
    ): AshValue {
        val canonical = PastaThrall.TYPES.firstOrNull { it.equals(thrallName, ignoreCase = true) } ?: thrallName
        return when (fieldName.lowercase()) {
            "id" -> {
                val index = PastaThrall.TYPES.indexOfFirst { it.equals(canonical, ignoreCase = true) }
                AshValue.of(if (index >= 0) (index + 1).toLong() else 0L)
            }
            "name" -> AshValue.of(canonical)
            "level" -> AshValue.of(
                (preferences?.let { PastaThrall.thrallLevel(it, canonical) } ?: 1).toLong(),
            )
            "skill" -> {
                val skillId = PastaThrall.bindSkillId(canonical) ?: 0
                val skillName = gameDatabase?.skill(skillId)?.name ?: ""
                AshValue(AshType.SKILL, skillName)
            }
            "current_modifiers" -> AshValue.of(ModifierDatabase.getThrall(canonical)?.modifiers ?: "")
            "image", "tinyimage" -> AshValue.EMPTY_STRING
            else -> throw ScriptException("thrall has no field '$fieldName'")
        }
    }
}
