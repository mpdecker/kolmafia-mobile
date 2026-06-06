package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

class ManaBurnManager(
    private val skillManager: SkillManager,
    private val preferences: Preferences,
) {
    companion object {
        /**
         * Returns true when mana burn should fire: enabled preference is set AND
         * current MP is at or above [MANA_BURN_BELOW_PCT] percent of max MP.
         */
        fun shouldBurn(charState: CharacterState, prefs: Preferences): Boolean {
            if (!prefs.getBoolean(Preferences.MANA_BURN_ENABLED, false)) return false
            if (charState.maxMp <= 0) return false
            val belowPct = prefs.getInt(Preferences.MANA_BURN_BELOW_PCT, 90)
            return charState.currentMp * 100 / charState.maxMp >= belowPct
        }

        /**
         * From the active [mood]'s trigger list, returns the [SkillData] whose effect has
         * the fewest remaining turns (i.e., the effect most in need of extension), subject
         * to: the skill must cost MP > 0, must be castable at current MP, and must not be
         * at its daily limit. Returns null if no mood or no eligible skill.
         */
        fun pickSkillToBurn(
            mood: Mood?,
            effectState: EffectState,
            skillState: SkillState,
            charState: CharacterState,
        ): SkillData? {
            if (mood == null) return null
            return mood.triggers
                .sortedBy { trigger ->
                    effectState.effects.firstOrNull { it.id == trigger.effectId }?.duration ?: 0
                }
                .firstNotNullOfOrNull { trigger ->
                    skillState.skills.firstOrNull { skill ->
                        skill.id == trigger.skillId
                            && skill.mpCost > 0
                            && skill.mpCost <= charState.currentMp
                            && (skill.dailyLimit == 0 || skill.timesCast < skill.dailyLimit)
                    }
                }
        }
    }

    /**
     * If mana burn is enabled and MP is above the threshold, casts one skill
     * (the one extending the shortest-duration active effect).
     * Returns true if a skill was cast, false otherwise.
     */
    suspend fun burnIfEnabled(
        mood: Mood?,
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
    ): Boolean {
        if (!shouldBurn(charState, preferences)) return false
        val skill = pickSkillToBurn(mood, effectState, skillState, charState) ?: return false
        skillManager.cast(skill)
        return true
    }
}
