package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.skill.BuffToolDuration
import net.sourceforge.kolmafia.skill.BuffTools
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState
import net.sourceforge.kolmafia.skill.SkillType

data class ManaBurnPick(val skill: SkillData, val quantity: Int = 1)

sealed class ManaBurnAction {
    data class Cast(val pick: ManaBurnPick) : ManaBurnAction()
    data class Cli(val command: String) : ManaBurnAction()
}

class ManaBurnManager(
    private val skillManager: SkillManager,
    private val preferences: Preferences,
) {
    /** Desktop `KoLmafiaCLI.executeLine` fallback for [considerLastChanceBurn]. */
    var cliExecutor: (suspend (String) -> Unit)? = null

    /** Full physical accessible count for BuffTool/wizard-hat duration in [pickFromActiveEffects]. */
    var accessibleCountProvider: (suspend (Int) -> Int)? = null

    var gameDatabase: GameDatabase? = null

    /** Desktop `KoLCharacter.getManaCostAdjustment(false)` for libram MP precompute. */
    var manaCostAdjustmentProvider: (() -> Int)? = null

    companion object {
        fun shouldBurn(charState: CharacterState, prefs: Preferences): Boolean {
            if (!prefs.getBoolean(Preferences.MANA_BURN_ENABLED, false)) return false
            if (charState.maxMp <= 0) return false
            val belowPct = prefs.getInt(Preferences.MANA_BURN_MIN_MP_PCT, 90)
            return charState.currentMp * 100 / charState.maxMp >= belowPct
        }

        private fun mpPercent(charState: CharacterState): Int =
            if (charState.maxMp <= 0) 0 else charState.currentMp * 100 / charState.maxMp

        private fun burnReserveMp(charState: CharacterState, prefs: Preferences): Int {
            val pct = prefs.getInt(Preferences.MANA_BURN_MIN_MP_PCT, 90)
            return charState.maxMp * pct / 100
        }

        private fun availableBurnMp(charState: CharacterState, prefs: Preferences): Long {
            val reserve = burnReserveMp(charState, prefs).toLong()
            return (charState.currentMp - reserve).coerceAtLeast(0)
        }

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
            baseDurationLimit: Int = prefs.getInt(Preferences.MAX_MANA_BURN, 1000) + charState.adventuresLeft,
        ): Int {
            val priority = skillBurnPriority(skillId, prefs)
            return baseDurationLimit * minOf(100, priority) / 100
        }

        /** Desktop [ManaBurnManager.considerBreakfastSkill]. */
        internal fun considerBreakfastSkill(
            skillState: SkillState,
            charState: CharacterState,
            prefs: Preferences,
            manaCostAdjustment: Int = 0,
        ): ManaBurnAction? {
            val availableMp = availableBurnMp(charState, prefs)
            val skills = skillState.skills
            for (name in BreakfastBurnSkills.breakfastSkills) {
                val skill = BreakfastBurnSkills.findSkill(skillState, name) ?: continue
                if (!BreakfastBurnSkills.canCastBreakfastSkill(name, charState, skills)) continue
                if (skill.mpCost <= 0) continue
                val maximumCast = BreakfastBurnSkills.maximumCastRemaining(skill)
                if (maximumCast == 0L) continue
                val castCount = minOf(
                    maximumCast,
                    availableMp / skill.mpCost.toLong(),
                )
                if (castCount > 0) {
                    return ManaBurnAction.Cast(ManaBurnPick(skill, castCount.toInt()))
                }
            }
            return considerLibramSummon(skillState, charState, prefs, manaCostAdjustment)
        }

        /** Desktop [ManaBurnManager.considerLibramSummon]. */
        internal fun considerLibramSummon(
            skillState: SkillState,
            charState: CharacterState,
            prefs: Preferences,
            manaCostAdjustment: Int = 0,
        ): ManaBurnAction? {
            val availableMp = availableBurnMp(charState, prefs)
            val castable = BreakfastBurnSkills.getBreakfastLibramSkills(prefs, skillState, charState)
            if (castable.isEmpty()) return null

            val libramSummons = prefs.getInt(Preferences.LIBRAM_SUMMONS, 0)
            val totalCasts = LibramSkillCasts.libramSkillCasts(libramSummons, availableMp, manaCostAdjustment)
            if (totalCasts <= 0) return null

            val nextCast = libramSummons
            if (castable.size == 1) {
                val batch = LibramSkillCasts.firstLibramBatch(totalCasts, castable.size, nextCast) ?: return null
                val skill = BreakfastBurnSkills.findSkill(skillState, castable[batch.first]) ?: return null
                return ManaBurnAction.Cast(ManaBurnPick(skill, batch.second))
            }

            val command = LibramSkillCasts.buildLibramSummonCommand(totalCasts, castable, nextCast)
                ?: return null
            return ManaBurnAction.Cli(command)
        }

        /** Desktop active-effect scan in [ManaBurnManager.getNextBurnCast]. */
        internal suspend fun pickFromActiveEffects(
            mood: Mood?,
            effectState: EffectState,
            skillState: SkillState,
            charState: CharacterState,
            moodLibrary: Map<String, Mood>,
            prefs: Preferences,
            accessibleCount: suspend (Int) -> Int = { 0 },
            gameDatabase: GameDatabase? = null,
            manaCostAdjustment: Int = 0,
        ): ManaBurnAction? {
            val onlyMood = !prefs.getBoolean(Preferences.ALLOW_NON_MOOD_BURNING, false)
            val summonThreshold = prefs.getInt(Preferences.MANA_BURN_SUMMON_THRESHOLD, 10)
            var breakfastAction: ManaBurnAction? = null
            if (prefs.getBoolean(Preferences.ALLOW_SUMMON_BURNING, false)) {
                breakfastAction = considerBreakfastSkill(skillState, charState, prefs, manaCostAdjustment)
            }

            var baseDurationLimit = prefs.getInt(Preferences.MAX_MANA_BURN, 1000) + charState.adventuresLeft
            var chosen: ManaBurn? = null
            val burns = mutableListOf<ManaBurn>()
            val availableMp = availableBurnMp(charState, prefs)

            for (effect in effectState.effects.sortedBy { it.duration }) {
                if (EffectGainGate.cannotGainEffect(effect.id, charState, effectState, prefs)) continue

                val skillName = UneffectSkillEffectMap.effectToSkill(effect.name) ?: continue
                val skill = skillState.skills.firstOrNull {
                    it.name.equals(skillName, ignoreCase = true)
                } ?: continue
                if (skill.mpCost <= 0) continue
                if (skill.dailyLimit != 0 && skill.timesCast >= skill.dailyLimit) continue

                val priority = skillBurnPriority(skill.id, prefs)
                if (priority <= 0) continue

                val durationLimit = durationLimitForSkill(skill.id, charState, prefs, baseDurationLimit)
                if (effect.duration >= durationLimit) continue

                if (breakfastAction != null && effect.duration >= summonThreshold) {
                    return breakfastAction
                }

                if (onlyMood && !MoodManager.effectInMood(effect.id, mood, moodLibrary)) continue

                if (skill.mpCost.toLong() > availableMp) {
                    baseDurationLimit = maxOf(10, minOf(effect.duration * 2, baseDurationLimit))
                    continue
                }

                val cachedAccessibleCount = prefetchAccessibleCounts(skill.id, accessibleCount)
                val burn = ManaBurn(
                    skillId = skill.id,
                    skillName = skill.name,
                    effectName = effect.name,
                    duration = effect.duration,
                    limit = durationLimit,
                    mpCost = skill.mpCost,
                    effectDurationPerCast = SkillDefinitionProxy.getEffectDuration(
                        skill.id, skillState, charState, effectState,
                        accessibleCount = cachedAccessibleCount,
                        gameDatabase = gameDatabase,
                    ),
                )
                if (chosen == null) {
                    chosen = burn
                }
                burns.add(burn)
                breakfastAction = null
            }

            if (chosen == null) {
                return breakfastAction
            }

            ManaBurn.simulateBalancedCasts(burns, availableMp)
            val skill = skillState.skills.firstOrNull { it.id == chosen.skillId } ?: return null
            return ManaBurnAction.Cast(ManaBurnPick(skill, chosen.count.coerceAtLeast(1)))
        }

        suspend fun pickBurnPick(
            mood: Mood?,
            effectState: EffectState,
            skillState: SkillState,
            charState: CharacterState,
            moodLibrary: Map<String, Mood> = emptyMap(),
            prefs: Preferences? = null,
            accessibleCount: suspend (Int) -> Int = { 0 },
            gameDatabase: GameDatabase? = null,
            includeActiveEffects: Boolean = true,
        ): ManaBurnPick? {
            if (prefs != null && includeActiveEffects) {
                (pickFromActiveEffects(
                    mood, effectState, skillState, charState, moodLibrary, prefs,
                    accessibleCount = accessibleCount,
                    gameDatabase = gameDatabase,
                ) as? ManaBurnAction.Cast)?.pick?.let { return it }
            }

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
                if (moodSkill != null) return ManaBurnPick(moodSkill)
            }

            if (prefs == null) return null

            val priorityNames = prefs.getString(Preferences.MANA_BURN_SKILLS, "")
                .split("|", ",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            for (name in priorityNames) {
                val skill = skillState.skills.firstOrNull {
                    it.name.equals(name, ignoreCase = true) && isEligible(it, charState)
                }
                if (skill != null) return ManaBurnPick(skill)
            }

            val summonThreshold = prefs.getInt(Preferences.MANA_BURN_SUMMON_THRESHOLD, 0)
            if (summonThreshold > 0 && mpPercent(charState) >= summonThreshold) {
                val summon = skillState.skills
                    .filter { it.type == SkillType.SUMMON && isEligible(it, charState) }
                    .minByOrNull { it.timesCast }
                if (summon != null) return ManaBurnPick(summon)
            }

            if (prefs.getBoolean(Preferences.ALLOW_NON_MOOD_BURNING, false)) {
                val skill = skillState.skills
                    .filter {
                        (it.type == SkillType.BUFF || it.type == SkillType.NONCOMBAT)
                            && isEligible(it, charState)
                    }
                    .minByOrNull { candidate ->
                        effectState.effects
                            .filter { effect ->
                                effect.name.contains(candidate.name, ignoreCase = true)
                                    || candidate.name.contains(effect.name, ignoreCase = true)
                            }
                            .minOfOrNull { it.duration } ?: Int.MAX_VALUE
                    }
                if (skill != null) return ManaBurnPick(skill)
            }

            return null
        }

        /** Desktop last-chance tail in [ManaBurnManager.getNextBurnCast]. */
        internal fun considerLastChanceBurn(
            charState: CharacterState,
            prefs: Preferences,
        ): String? {
            val availableMp = availableBurnMp(charState, prefs)
            val threshold = prefs.getInt(Preferences.LAST_CHANCE_THRESHOLD, 100)
            if (availableMp < threshold) return null
            val cmd = prefs.getString(Preferences.LAST_CHANCE_BURN, "").trim()
            if (cmd.isEmpty() || cmd.startsWith("burn ", ignoreCase = true)) return null
            return cmd.replace("#", availableMp.toString())
        }

        internal suspend fun resolveBurnAction(
            mood: Mood?,
            effectState: EffectState,
            skillState: SkillState,
            charState: CharacterState,
            moodLibrary: Map<String, Mood> = emptyMap(),
            prefs: Preferences? = null,
            accessibleCount: suspend (Int) -> Int = { 0 },
            gameDatabase: GameDatabase? = null,
            manaCostAdjustment: Int = 0,
        ): ManaBurnAction? {
            if (prefs == null) return null
            pickFromActiveEffects(
                mood, effectState, skillState, charState, moodLibrary, prefs,
                accessibleCount = accessibleCount,
                gameDatabase = gameDatabase,
                manaCostAdjustment = manaCostAdjustment,
            )?.let { return it }
            pickBurnPick(
                mood, effectState, skillState, charState, moodLibrary, prefs,
                accessibleCount = accessibleCount,
                gameDatabase = gameDatabase,
                includeActiveEffects = false,
            )?.let { return ManaBurnAction.Cast(it) }
            considerLastChanceBurn(charState, prefs)
                ?.let { return ManaBurnAction.Cli(it) }
            return null
        }

        suspend fun pickSkillToBurn(
            mood: Mood?,
            effectState: EffectState,
            skillState: SkillState,
            charState: CharacterState,
            moodLibrary: Map<String, Mood> = emptyMap(),
            prefs: Preferences? = null,
        ): SkillData? = pickBurnPick(mood, effectState, skillState, charState, moodLibrary, prefs)?.skill

        private suspend fun prefetchAccessibleCounts(
            skillId: Int,
            accessibleCount: suspend (Int) -> Int,
        ): (Int) -> Int {
            val cache = mutableMapOf<Int, Int>()
            val ids = buildSet {
                add(BuffToolDuration.JEWEL_EYED_WIZARD_HAT)
                add(BuffToolDuration.REPLICA_JEWEL_EYED_WIZARD_HAT)
                BuffTools.toolsForSkill(skillId)?.forEach { add(it.itemId) }
            }
            for (id in ids) {
                cache[id] = accessibleCount(id)
            }
            return { id -> cache[id] ?: 0 }
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
        when (val action = resolveBurnAction(
            mood, effectState, skillState, charState, moodLibrary, preferences,
            accessibleCount = accessibleCountProvider ?: { 0 },
            gameDatabase = gameDatabase,
            manaCostAdjustment = manaCostAdjustmentProvider?.invoke() ?: 0,
        )) {
            is ManaBurnAction.Cast -> {
                val result = skillManager.cast(action.pick.skill, action.pick.quantity)
                return result.isSuccess
            }
            is ManaBurnAction.Cli -> {
                val executor = cliExecutor ?: return false
                executor(action.command)
                return true
            }
            null -> return false
        }
    }

    /**
     * Desktop [ManaBurnManager.burnExtraMana] with `isManualInvocation=true`.
     * Skips [shouldBurn] auto-threshold; zombiecore and recovery-limited modes no-op.
     */
    suspend fun burnExtraMana(
        mood: Mood?,
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
        moodLibrary: Map<String, Mood> = emptyMap(),
        currentCharState: () -> CharacterState = { charState },
    ) {
        if (charState.inZombiecore) return
        if (LimitModeGates.limitRecovery(charState.limitMode)) return
        burnUntilStable(
            mood, effectState, skillState, currentCharState, moodLibrary,
            minimumMp = null,
        )
    }

    /**
     * Desktop [ManaBurnManager.burnMana] — keep at least [minimumMp] MP in reserve.
     */
    suspend fun burnMana(
        minimumMp: Long,
        mood: Mood?,
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
        moodLibrary: Map<String, Mood> = emptyMap(),
        currentCharState: () -> CharacterState = { charState },
    ) {
        if (charState.inZombiecore) return
        burnUntilStable(
            mood, effectState, skillState, currentCharState, moodLibrary,
            minimumMp = minimumMp.coerceAtLeast(0),
        )
    }

    private suspend fun burnUntilStable(
        mood: Mood?,
        effectState: EffectState,
        skillState: SkillState,
        currentCharState: () -> CharacterState,
        moodLibrary: Map<String, Mood>,
        minimumMp: Long?,
    ) {
        var lastMp = -1
        while (true) {
            val live = currentCharState()
            if (live.currentMp == lastMp) return
            lastMp = live.currentMp
            if (minimumMp != null && live.currentMp <= minimumMp) return
            val action = resolveBurnAction(
                mood, effectState, skillState, live, moodLibrary, preferences,
                accessibleCount = accessibleCountProvider ?: { 0 },
                gameDatabase = gameDatabase,
                manaCostAdjustment = manaCostAdjustmentProvider?.invoke() ?: 0,
            ) ?: return
            when (action) {
                is ManaBurnAction.Cast -> {
                    skillManager.cast(action.pick.skill, action.pick.quantity)
                }
                is ManaBurnAction.Cli -> {
                    val executor = cliExecutor ?: return
                    executor(action.command)
                }
            }
        }
    }
}
