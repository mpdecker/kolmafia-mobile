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

    suspend fun executeActiveMood(
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
    ) {
        val mood = activeMood ?: return
        if (!preferences.getBoolean(Preferences.AUTO_BUFF, true)) return
        for (trigger in missingTriggers(mood, effectState)) {
            val skill = skillState.skills.firstOrNull { it.id == trigger.skillId } ?: continue
            if (skill.mpCost > charState.currentMp) continue
            if (skill.dailyLimit > 0 && skill.timesCast >= skill.dailyLimit) continue
            skillManager.cast(skill)
        }
    }
}
