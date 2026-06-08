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
                req.uneffect(effect.id).onFailure { /* best-effort; continue on network failure */ }
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
        val songLimit = charState.atSongLimit  // 0 for non-AT; 3 for AT

        for (trigger in missingTriggers(mood, effectState)) {
            val skill = skillState.skills.firstOrNull { it.id == trigger.skillId } ?: continue
            if (skill.mpCost > charState.currentMp) continue
            if (skill.dailyLimit > 0 && skill.timesCast >= skill.dailyLimit) continue

            // AT song slot management: evict lowest-priority song before overcasting
            if (songLimit > 0 && isAtSong(trigger.effectName)) {
                val activeSongs = effectState.effects.filter { isAtSong(it.name) }
                if (activeSongs.size >= songLimit) {
                    val toEvict = lowestPriorityActiveSong(activeSongs, mood.triggers)
                    if (toEvict != null) {
                        uneffectRequest?.uneffect(toEvict.id)
                    }
                }
            }

            skillManager.cast(skill)
        }
    }

    /**
     * Returns the active AT song with the lowest priority in the current mood.
     * "Lowest priority" = the active song whose effectId appears LAST in [moodTriggers].
     * Songs not present in the mood trigger list are treated as lowest priority (evicted first).
     */
    private fun lowestPriorityActiveSong(
        activeSongs: List<net.sourceforge.kolmafia.effect.EffectData>,
        moodTriggers: List<MoodTrigger>,
    ): net.sourceforge.kolmafia.effect.EffectData? {
        if (activeSongs.isEmpty()) return null
        val triggerEffectIds = moodTriggers.map { it.effectId }
        return activeSongs.maxByOrNull { song ->
            val idx = triggerEffectIds.lastIndexOf(song.id)
            if (idx < 0) Int.MAX_VALUE else idx  // not in mood → treat as lowest priority
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

    // ── Mood library ──────────────────────────────────────────────────────────

    var moodLibrary: Map<String, Mood> = emptyMap()
        private set

    /** Adds or replaces the mood in the library by [Mood.name]. */
    fun addMoodToLibrary(mood: Mood) {
        moodLibrary = moodLibrary + (mood.name to mood)
    }

    /** Removes the mood with the given [name] from the library. No-op if absent. */
    fun removeMoodFromLibrary(name: String) {
        moodLibrary = moodLibrary - name
        preferences.setString("moodTriggers_$name", "")  // clear orphaned trigger key
    }

    /**
     * Sets [activeMood] to the library entry named [name] and persists via [saveActiveMood].
     * Returns true on success, false if [name] is not in the library.
     */
    fun setActiveMoodByName(name: String): Boolean {
        val mood = moodLibrary[name] ?: return false
        activeMood = mood
        saveActiveMood()
        return true
    }

    /** Persists the current [moodLibrary] to preferences. */
    fun saveMoodLibrary() {
        val names = moodLibrary.keys.joinToString("|")
        preferences.setString(Preferences.MOOD_LIBRARY_NAMES, names)
        for ((name, mood) in moodLibrary) {
            preferences.setString("moodTriggers_$name", serializeTriggers(mood.triggers))
        }
    }

    /** Restores [moodLibrary] from preferences. Call once after login. */
    fun loadMoodLibrary() {
        val namesRaw = preferences.getString(Preferences.MOOD_LIBRARY_NAMES)
        if (namesRaw.isBlank()) { moodLibrary = emptyMap(); return }
        val names = namesRaw.split("|").filter { it.isNotBlank() }
        moodLibrary = names.associate { name ->
            val raw = preferences.getString("moodTriggers_$name")
            name to Mood(name, parseTriggers(raw))
        }
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

    /**
     * Returns true if [effectName] is an Accordion Thief song, determined by the
     * "song" attribute in statuseffects.txt (parsed by EffectDatabase at load time).
     *
     * AT songs (Polka of Plenty, Fat Leon's, Ode to Booze, Aloysius' Antiphon, etc.)
     * have "song" in their `attributes` column.
     *
     * Returns false when EffectDatabase is not loaded (e.g., in test environments
     * that don't load game data files).
     */
    internal fun isAtSong(effectName: String): Boolean =
        net.sourceforge.kolmafia.data.EffectDatabase.getByName(effectName)
            ?.attributes?.contains("song") == true
}
