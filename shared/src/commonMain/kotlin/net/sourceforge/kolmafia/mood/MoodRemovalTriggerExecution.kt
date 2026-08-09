package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.MoodUneffectActionParser
import net.sourceforge.kolmafia.request.UneffectAction
import net.sourceforge.kolmafia.request.UneffectActionContext
import net.sourceforge.kolmafia.request.UneffectRequest
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

/** Desktop [MoodTrigger.shouldExecute] / [MoodTrigger.execute] for removal triggers. */
object MoodRemovalTriggerExecution {

    /** Desktop `Math.max(count, count * multiplicity)` for mood cast/use counts. */
    fun scaledCount(baseCount: Int, multiplicity: Int): Int =
        maxOf(baseCount, baseCount * multiplicity)

    fun shouldExecute(
        trigger: MoodRemovalTrigger,
        effectState: EffectState,
        multiplicity: Int = 0,
    ): Boolean {
        if (trigger.effectId > 0 && EffectGainGate.cannotGainEffect(trigger.effectId)) {
            return false
        }
        when (trigger.type) {
            MoodRemovalTriggerType.UNCONDITIONAL -> return true
            MoodRemovalTriggerType.GAIN_EFFECT ->
                return effectState.effects.any { it.id == trigger.effectId }
            MoodRemovalTriggerType.LOSE_EFFECT -> {
                if (multiplicity > 0) return true
                val active = effectState.effects.firstOrNull { it.id == trigger.effectId }
                if (unstackableAction(trigger.action)) {
                    return active == null
                }
                val activeCount = active?.duration ?: 0
                return activeCount <= 5
            }
        }
    }

    /** Desktop [MoodManager.unstackableAction]. */
    fun unstackableAction(action: String): Boolean {
        val lower = action.lowercase()
        return lower.contains("absinthe") ||
            lower.contains("astral mushroom") ||
            lower.contains("oasis") ||
            lower.contains("turtle pheromones") ||
            lower.contains("gong")
    }

    /** Desktop trigger ordering: unconditional, gain_effect, lose_effect. */
    fun sortedForExecution(triggers: List<MoodRemovalTrigger>): List<MoodRemovalTrigger> =
        triggers.sortedWith(
            compareBy(
                { typeOrder(it.type) },
                { it.effectName.lowercase() },
                { it.action.lowercase() },
            ),
        )

    private fun typeOrder(type: MoodRemovalTriggerType): Int = when (type) {
        MoodRemovalTriggerType.UNCONDITIONAL -> 0
        MoodRemovalTriggerType.GAIN_EFFECT -> 1
        MoodRemovalTriggerType.LOSE_EFFECT -> 2
    }

    /** True when desktop [MoodTrigger.isSkill] would run this trigger in the first pass. */
    fun isEffectMappedSkillTrigger(trigger: MoodRemovalTrigger): Boolean {
        if (trigger.type != MoodRemovalTriggerType.LOSE_EFFECT) return false
        val skillName = UneffectSkillEffectMap.effectToSkill(trigger.effectName) ?: return false
        return skillName.isNotEmpty()
    }

    suspend fun executeApplicable(
        triggers: List<MoodRemovalTrigger>,
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
        preferences: Preferences,
        skillManager: SkillManager,
        uneffectRequest: UneffectRequest?,
        cliExecutor: (suspend (String) -> Unit)?,
        isAtSong: (String) -> Boolean,
        moodTriggers: List<MoodTrigger>,
        atSongTracker: AtSongSlotTracker = AtSongSlotTracker(),
        multiplicity: Int = 0,
    ) {
        val ordered = sortedForExecution(triggers)
        val skillMapped = ordered.filter { isEffectMappedSkillTrigger(it) && shouldExecute(it, effectState, multiplicity) }
        val other = ordered.filter { !isEffectMappedSkillTrigger(it) && shouldExecute(it, effectState, multiplicity) }

        for (trigger in skillMapped + other) {
            executeOne(
                trigger = trigger,
                effectState = effectState,
                skillState = skillState,
                charState = charState,
                preferences = preferences,
                skillManager = skillManager,
                uneffectRequest = uneffectRequest,
                cliExecutor = cliExecutor,
                isAtSong = isAtSong,
                moodTriggers = moodTriggers,
                atSongTracker = atSongTracker,
                multiplicity = multiplicity,
            )
        }
    }

    internal suspend fun executeOne(
        trigger: MoodRemovalTrigger,
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
        preferences: Preferences,
        skillManager: SkillManager,
        uneffectRequest: UneffectRequest?,
        cliExecutor: (suspend (String) -> Unit)?,
        isAtSong: (String) -> Boolean,
        moodTriggers: List<MoodTrigger>,
        atSongTracker: AtSongSlotTracker,
        multiplicity: Int = 0,
    ) {
        val action = trigger.action.trim()
        if (action.isEmpty()) return

        if (action.startsWith("cast ", ignoreCase = true) ||
            action.startsWith("skill ", ignoreCase = true) ||
            action.startsWith("buff ", ignoreCase = true)
        ) {
            executeCastAction(
                action = action,
                trigger = trigger,
                effectState = effectState,
                skillState = skillState,
                charState = charState,
                skillManager = skillManager,
                uneffectRequest = uneffectRequest,
                isAtSong = isAtSong,
                moodTriggers = moodTriggers,
                atSongTracker = atSongTracker,
                multiplicity = multiplicity,
            )
            return
        }

        val uneffectCtx = buildUneffectContext(trigger, preferences, charState, skillState)
        MoodUneffectActionParser.parse(action, uneffectCtx)?.let { parsed ->
            executeUneffectAction(parsed, action, multiplicity, skillManager, cliExecutor)
            return
        }

        cliExecutor?.invoke(action)
    }

    private suspend fun executeCastAction(
        action: String,
        trigger: MoodRemovalTrigger,
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
        skillManager: SkillManager,
        uneffectRequest: UneffectRequest?,
        isAtSong: (String) -> Boolean,
        moodTriggers: List<MoodTrigger>,
        atSongTracker: AtSongSlotTracker,
        multiplicity: Int = 0,
    ) {
        val skillName = MoodUneffectActionParser.parseSkillFromCastAction(action)
        if (skillName.isEmpty()) return
        val skill = skillState.skills.firstOrNull { it.name.equals(skillName, ignoreCase = true) } ?: return
        if (skill.mpCost > charState.currentMp) return
        if (skill.dailyLimit > 0 && skill.timesCast >= skill.dailyLimit) return

        val songLimit = charState.atSongLimit
        if (songLimit > 0 && trigger.type == MoodRemovalTriggerType.LOSE_EFFECT) {
            AtSongEviction.evictBeforeCast(
                effectName = trigger.effectName,
                effectState = effectState,
                songLimit = songLimit,
                moodTriggers = moodTriggers,
                isAtSong = isAtSong,
                uneffectRequest = uneffectRequest,
                tracker = atSongTracker,
            )
        }

        val count = MoodUneffectActionParser.parseCastCount(action)
        skillManager.cast(skill, scaledCount(count, multiplicity))
    }

    private fun buildUneffectContext(
        trigger: MoodRemovalTrigger,
        preferences: Preferences,
        charState: CharacterState,
        skillState: SkillState,
    ): UneffectActionContext =
        UneffectActionContext(
            effectId = trigger.effectId,
            effectName = trigger.effectName,
            moodPredefinedAction = trigger.action,
            preferences = preferences,
            characterState = charState,
            hasItemId = { false },
            hasSkill = { name -> skillState.skills.any { it.name.equals(name, ignoreCase = true) } },
            canCastSkill = { name ->
                val skill = skillState.skills.firstOrNull { it.name.equals(name, ignoreCase = true) }
                skill != null &&
                    skill.mpCost <= charState.currentMp &&
                    (skill.dailyLimit == 0 || skill.timesCast < skill.dailyLimit)
            },
        )

    private suspend fun executeUneffectAction(
        action: UneffectAction,
        rawAction: String,
        multiplicity: Int,
        skillManager: SkillManager,
        cliExecutor: (suspend (String) -> Unit)?,
    ) {
        when (action) {
            is UneffectAction.CastSkill -> {
                // Parsed cast actions route through executeCastAction instead.
            }
            is UneffectAction.HotTub -> cliExecutor?.invoke("hottub")
            is UneffectAction.UseItem -> {
                val count = scaledCount(MoodUneffectActionParser.parseUseCount(rawAction), multiplicity)
                val cmd = if (action.retrieveFirst) {
                    "use * ${action.itemId}"
                } else {
                    "use $count [${action.itemId}]"
                }
                cliExecutor?.invoke(cmd)
            }
            UneffectAction.HttpUneffect -> Unit
        }
    }
}
