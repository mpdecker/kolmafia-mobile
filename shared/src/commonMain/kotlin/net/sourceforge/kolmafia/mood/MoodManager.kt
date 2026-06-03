package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

class MoodManager(
    private val skillManager: SkillManager,
    private val preferences: Preferences,
) {
    var activeMood: Mood? = null

    companion object {
        fun missingTriggers(mood: Mood, effectState: EffectState): List<MoodTrigger> =
            mood.triggers.filter { trigger ->
                val remaining = effectState.effects
                    .firstOrNull { it.id == trigger.effectId }
                    ?.duration ?: 0
                remaining < trigger.minimumTurns
            }
    }
}
