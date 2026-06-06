package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.UneffectRequest
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

class MoodManager(
    private val skillManager: SkillManager,
    private val preferences: Preferences,
    private val uneffectRequest: UneffectRequest? = null,
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

    // ── Malignant effect removal ──────────────────────────────────────────────

    /**
     * Removes any effect from [MalignantEffects.NAMES] currently active on the character.
     * No-op when [Preferences.REMOVE_MALIGNANT_EFFECTS] is false or [uneffectRequest] is null.
     */
    suspend fun removeMalignantEffects(effectState: EffectState) {
        if (!preferences.getBoolean(Preferences.REMOVE_MALIGNANT_EFFECTS, true)) return
        val req = uneffectRequest ?: return
        for (effect in effectState.effects) {
            if (effect.name in MalignantEffects.NAMES) {
                req.uneffect(effect.id)
            }
        }
    }

    // ── Mood execution ────────────────────────────────────────────────────────

    suspend fun executeActiveMood(
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
    ) {
        removeMalignantEffects(effectState)
        val mood = activeMood ?: return
        if (!preferences.getBoolean(Preferences.AUTO_BUFF, true)) return
        for (trigger in missingTriggers(mood, effectState)) {
            val skill = skillState.skills.firstOrNull { it.id == trigger.skillId } ?: continue
            if (skill.mpCost > charState.currentMp) continue
            if (skill.dailyLimit > 0 && skill.timesCast >= skill.dailyLimit) continue
            skillManager.cast(skill)
        }
    }

    // ── Active mood persistence ───────────────────────────────────────────────

    /** Writes the current [activeMood] to preferences. Call whenever the mood changes. */
    fun saveActiveMood() {
        val mood = activeMood
        if (mood == null) {
            preferences.setString(Preferences.ACTIVE_MOOD_NAME, "")
            preferences.setString(Preferences.ACTIVE_MOOD_TRIGGERS, "")
            return
        }
        preferences.setString(Preferences.ACTIVE_MOOD_NAME, mood.name)
        preferences.setString(Preferences.ACTIVE_MOOD_TRIGGERS, serializeTriggers(mood.triggers))
    }

    /** Restores [activeMood] from preferences. Call once after login. */
    fun loadActiveMood() {
        val name = preferences.getString(Preferences.ACTIVE_MOOD_NAME)
        if (name.isBlank()) {
            activeMood = null
            return
        }
        val raw = preferences.getString(Preferences.ACTIVE_MOOD_TRIGGERS)
        activeMood = Mood(name, parseTriggers(raw))
    }

    // ── Serialization helpers ─────────────────────────────────────────────────

    internal fun serializeTriggers(triggers: List<MoodTrigger>): String =
        triggers.joinToString("|") { t ->
            "${t.effectId}:${t.effectName}:${t.skillId}:${t.skillName}:${t.minimumTurns}"
        }

    internal fun parseTriggers(raw: String): List<MoodTrigger> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            val parts = entry.split(":", limit = 5)
            if (parts.size < 5) return@mapNotNull null
            MoodTrigger(
                effectId     = parts[0].toIntOrNull() ?: return@mapNotNull null,
                effectName   = parts[1],
                skillId      = parts[2].toIntOrNull() ?: return@mapNotNull null,
                skillName    = parts[3],
                minimumTurns = parts[4].toIntOrNull() ?: return@mapNotNull null,
            )
        }
    }
}
