package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState
import net.sourceforge.kolmafia.skill.SkillType

class ManaBurnManager(
    private val skillManager: SkillManager,
    private val preferences: Preferences,
) {
    companion object {
        fun shouldBurn(charState: CharacterState, prefs: Preferences): Boolean {
            if (!prefs.getBoolean(Preferences.MANA_BURN_ENABLED, false)) return false
            if (charState.maxMp <= 0) return false
            val belowPct = prefs.getInt(Preferences.MANA_BURN_MIN_MP_PCT, 90)
            return charState.currentMp * 100 / charState.maxMp >= belowPct
        }

        private fun mpPercent(charState: CharacterState): Int =
            if (charState.maxMp <= 0) 0 else charState.currentMp * 100 / charState.maxMp

        private fun isEligible(skill: SkillData, charState: CharacterState): Boolean =
            skill.mpCost > 0
                && skill.mpCost <= charState.currentMp
                && (skill.dailyLimit == 0 || skill.timesCast < skill.dailyLimit)

        /** Desktop `Preferences.getInteger("skillBurn" + skillId) + 100`. */
        internal fun skillBurnPriority(skillId: Int, prefs: Preferences): Int =
            prefs.getInt(Preferences.skillBurnPrefKey(skillId), 100) + 100

        internal fun durationLimitForSkill(
            skillId: Int,
            charState: CharacterState,
            prefs: Preferences,
        ): Int {
            val baseLimit = prefs.getInt(Preferences.MAX_MANA_BURN, 1000) + charState.adventuresLeft
            val priority = skillBurnPriority(skillId, prefs)
            return baseLimit * minOf(100, priority) / 100
        }

        /** Desktop active-effect scan in [ManaBurnManager.getNextBurnCast]. */
        internal fun pickFromActiveEffects(
            mood: Mood?,
            effectState: EffectState,
            skillState: SkillState,
            charState: CharacterState,
            moodLibrary: Map<String, Mood>,
            prefs: Preferences,
        ): SkillData? {
            val onlyMood = !prefs.getBoolean(Preferences.ALLOW_NON_MOOD_BURNING, false)
            for (effect in effectState.effects.sortedBy { it.duration }) {
                if (EffectGainGate.cannotGainEffect(effect.id)) continue
                if (onlyMood && !MoodManager.effectInMood(effect.id, mood, moodLibrary)) continue

                val skillName = UneffectSkillEffectMap.effectToSkill(effect.name) ?: continue
                val skill = skillState.skills.firstOrNull {
                    it.name.equals(skillName, ignoreCase = true)
                } ?: continue
                if (!isEligible(skill, charState)) continue

                val priority = skillBurnPriority(skill.id, prefs)
                if (priority <= 0) continue

                val durationLimit = durationLimitForSkill(skill.id, charState, prefs)
                if (effect.duration >= durationLimit) continue

                return skill
            }
            return null
        }

        fun pickSkillToBurn(
            mood: Mood?,
            effectState: EffectState,
            skillState: SkillState,
            charState: CharacterState,
            moodLibrary: Map<String, Mood> = emptyMap(),
            prefs: Preferences? = null,
        ): SkillData? {
            if (prefs != null) {
                pickFromActiveEffects(
                    mood, effectState, skillState, charState, moodLibrary, prefs,
                )?.let { return it }
            }

            // Mood-trigger fallback for unmapped synthetic/test effects
            if (mood != null) {
                val moodSkill = mood.effectiveTriggers(moodLibrary)
                    .sortedBy { trigger ->
                        effectState.effects.firstOrNull { it.id == trigger.effectId }?.duration ?: 0
                    }
                    .firstNotNullOfOrNull { trigger ->
                        skillState.skills.firstOrNull { skill ->
                            skill.id == trigger.skillId && isEligible(skill, charState)
                        }
                    }
                if (moodSkill != null) return moodSkill
            }

            if (prefs == null) return null

            // Explicit priority list from pref
            val priorityNames = prefs.getString(Preferences.MANA_BURN_SKILLS, "")
                .split("|", ",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            for (name in priorityNames) {
                val skill = skillState.skills.firstOrNull {
                    it.name.equals(name, ignoreCase = true) && isEligible(it, charState)
                }
                if (skill != null) return skill
            }

            // Summon skills when MP% meets summon threshold
            val summonThreshold = prefs.getInt(Preferences.MANA_BURN_SUMMON_THRESHOLD, 0)
            if (summonThreshold > 0 && mpPercent(charState) >= summonThreshold) {
                val summon = skillState.skills
                    .filter { it.type == SkillType.SUMMON && isEligible(it, charState) }
                    .minByOrNull { it.timesCast }
                if (summon != null) return summon
            }

            // Non-mood buff burning by name match
            if (prefs.getBoolean(Preferences.ALLOW_NON_MOOD_BURNING, false)) {
                return skillState.skills
                    .filter {
                        (it.type == SkillType.BUFF || it.type == SkillType.NONCOMBAT)
                            && isEligible(it, charState)
                    }
                    .minByOrNull { skill ->
                        effectState.effects
                            .filter { effect ->
                                effect.name.contains(skill.name, ignoreCase = true)
                                    || skill.name.contains(effect.name, ignoreCase = true)
                            }
                            .minOfOrNull { it.duration } ?: Int.MAX_VALUE
                    }
            }

            return null
        }
    }

    suspend fun burnIfEnabled(
        mood: Mood?,
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
        moodLibrary: Map<String, Mood> = emptyMap(),
    ): Boolean {
        if (!shouldBurn(charState, preferences)) return false
        val skill = pickSkillToBurn(
            mood, effectState, skillState, charState, moodLibrary, preferences,
        ) ?: return false
        skillManager.cast(skill)
        return true
    }
}
