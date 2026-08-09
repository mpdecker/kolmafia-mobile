package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectDefinitionProxy
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.equipment.OutfitCheckpoint
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.platform.UserDataFileIO
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.UneffectRemovableMaps
import net.sourceforge.kolmafia.request.UneffectRequest
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

open class MoodManager(
    private val skillManager: SkillManager,
    private val preferences: Preferences,
    private val uneffectRequest: UneffectRequest? = null,
) {
    var activeMood: Mood? = null

    /** Desktop `KoLmafiaCLI.executeLine` fallback for mood removal trigger actions. */
    var cliExecutor: (suspend (String) -> Unit)? = null

    @Volatile
    private var executing: Boolean = false

    private var settingsUsername: String = ""

    fun isExecuting(): Boolean = executing

    companion object {
        fun moodsFileName(username: String): String {
            val normalized = username.trim().lowercase().replace(' ', '_')
            return "${normalized}_moods.txt"
        }

        fun missingTriggers(
            mood: Mood,
            effectState: EffectState,
            library: Map<String, Mood> = emptyMap(),
        ): List<MoodTrigger> =
            mood.effectiveTriggers(library).filter { trigger ->
                val remaining = effectState.effects
                    .firstOrNull { it.id == trigger.effectId }
                    ?.duration ?: 0
                remaining < trigger.minimumTurns
            }

        /** Desktop [MoodManager.effectInMood]. */
        fun effectInMood(
            effectId: Int,
            mood: Mood?,
            library: Map<String, Mood> = emptyMap(),
        ): Boolean {
            if (mood == null) return false
            return mood.effectiveTriggers(library).any { it.effectId == effectId }
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

    /**
     * Desktop [MoodManager.checkpointedExecute]: snapshot equipment, run mood once, restore.
     * [multiplicity] is forwarded from ASH `mood_execute(N)` and CLI `mood repeat N`.
     */
    suspend fun checkpointedExecute(
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
        character: KoLCharacter? = null,
        equipmentRequest: EquipmentRequest? = null,
        gameDatabase: GameDatabase? = null,
        multiplicity: Int = 0,
    ) {
        if (executing) return
        if (LimitModeGates.limitRecovery(charState.limitMode)) return
        executing = true
        try {
            val checkpoint = if (character != null && equipmentRequest != null && gameDatabase != null) {
                OutfitCheckpoint.snapshot(character, equipmentRequest, gameDatabase)
            } else {
                null
            }
            if (checkpoint != null) {
                checkpoint.use { executeActiveMood(effectState, skillState, charState, multiplicity) }
            } else {
                executeActiveMood(effectState, skillState, charState, multiplicity)
            }
        } finally {
            executing = false
        }
    }

    open suspend fun executeActiveMood(
        effectState: EffectState,
        skillState: SkillState,
        charState: CharacterState,
        multiplicity: Int = 0,
    ) {
        removeMalignantEffects(effectState)
        val mood = activeMood ?: return
        if (!preferences.getBoolean(Preferences.AUTO_BUFF, true)) return
        val songLimit = charState.atSongLimit  // 0 for non-AT; 3 for AT
        val atSongTracker = AtSongSlotTracker()

        val effectiveTriggers = mood.effectiveTriggers(moodLibrary)
        if (songLimit > 0) {
            AtSongEviction.prePassEvict(
                effectState = effectState,
                moodTriggers = effectiveTriggers,
                songLimit = songLimit,
                isAtSong = ::isAtSong,
                uneffectRequest = uneffectRequest,
                tracker = atSongTracker,
            )
        }

        val buffTriggers = if (multiplicity > 0) {
            effectiveTriggers
        } else {
            missingTriggers(mood, effectState, moodLibrary)
        }
        for (trigger in buffTriggers) {
            val skill = skillState.skills.firstOrNull { it.id == trigger.skillId } ?: continue
            if (skill.mpCost > charState.currentMp) continue
            if (skill.dailyLimit > 0 && skill.timesCast >= skill.dailyLimit) continue

            AtSongEviction.evictBeforeCast(
                effectName = trigger.effectName,
                effectState = effectState,
                songLimit = songLimit,
                moodTriggers = effectiveTriggers,
                isAtSong = ::isAtSong,
                uneffectRequest = uneffectRequest,
                tracker = atSongTracker,
            )

            skillManager.cast(skill, MoodRemovalTriggerExecution.scaledCount(1, multiplicity))
        }

        MoodRemovalTriggerExecution.executeApplicable(
            triggers = mood.effectiveRemovalTriggers(moodLibrary),
            effectState = effectState,
            skillState = skillState,
            charState = charState,
            preferences = preferences,
            skillManager = skillManager,
            uneffectRequest = uneffectRequest,
            cliExecutor = cliExecutor,
            isAtSong = ::isAtSong,
            moodTriggers = effectiveTriggers,
            atSongTracker = atSongTracker,
            multiplicity = multiplicity,
        )
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
        preferences.setString(Preferences.ACTIVE_MOOD_NAME, mood.displayName())
        preferences.setString(Preferences.ACTIVE_MOOD_TRIGGERS, serializeTriggers(mood.triggers))
    }

    /** Restores [activeMood] from preferences. Call once after login. */
    fun loadActiveMood() {
        val storedName = preferences.getString(Preferences.ACTIVE_MOOD_NAME)
        if (storedName.isBlank()) {
            activeMood = null
            return
        }
        val (name, parentNames) = Mood.parseName(storedName)
        val raw = preferences.getString(Preferences.ACTIVE_MOOD_TRIGGERS)
        val libraryMood = moodLibrary[name]
        activeMood = Mood(
            name,
            parseTriggers(raw),
            parentNames,
            libraryMood?.removalTriggers ?: emptyList(),
        )
    }

    // ── Mood library ──────────────────────────────────────────────────────────

    var moodLibrary: Map<String, Mood> = emptyMap()
        private set

    /** Adds or replaces the mood in the library by [Mood.name]. */
    fun addMoodToLibrary(mood: Mood) {
        moodLibrary = moodLibrary + (mood.name to mood)
        for (trigger in mood.removalTriggers) {
            if (trigger.type == MoodRemovalTriggerType.LOSE_EFFECT) {
                MoodRemovalKnownSources.register(trigger.effectName, trigger.action)
            }
        }
    }

    /** Removes the mood with the given [name] from the library. No-op if absent. */
    fun removeMoodFromLibrary(name: String) {
        moodLibrary = moodLibrary - name
        preferences.setString("moodTriggers_$name", "")  // clear orphaned trigger key
        preferences.setString("moodRemovalTriggers_$name", "")
    }

    /**
     * Adds a desktop-style removal trigger to the named mood in the library.
     * Returns false when the mood or trigger line is invalid.
     */
    fun addRemovalTrigger(moodName: String, type: String, effectName: String, action: String): Boolean {
        val canonical = Mood.canonicalName(moodName)
        val mood = moodLibrary[canonical] ?: return false
        val line = "$type $effectName => $action"
        val trigger = MoodRemovalTriggerParser.parseLine(line) ?: return false
        val updated = mood.copy(removalTriggers = mood.removalTriggers + trigger)
        addMoodToLibrary(updated)
        if (activeMood?.name == updated.name) {
            activeMood = updated
        }
        return true
    }

    /**
     * Desktop [MoodManager.getDefaultAction]: mood trigger action with type-specific fallbacks.
     */
    fun getDefaultAction(type: String, name: String): String {
        if (type.isBlank() || name.isBlank()) return ""

        var action = ""
        val triggers = activeMood?.effectiveRemovalTriggers(moodLibrary) ?: emptyList()
        for (trigger in triggers) {
            if (trigger.matches(type, name)) {
                action = trigger.action
                break
            }
        }

        if (type.equals("unconditional", ignoreCase = true)) {
            return action
        }
        if (type.equals("lose_effect", ignoreCase = true)) {
            if (action.isEmpty()) {
                val effectId = EffectDatabase.getByName(name)?.id ?: 0
                action = EffectDefinitionProxy.getDefaultAction(effectId).orEmpty()
                if (action.isEmpty()) {
                    action = MoodRemovalKnownSources.getKnownSources(name)
                }
            }
            return action
        }

        if (action.isEmpty()) {
            val effectId = EffectDatabase.getByName(name)?.id ?: 0
            if (UneffectRemovableMaps.isRemovable(effectId)) {
                action = "uneffect $name"
            }
        }
        return action
    }

    /**
     * Sets [activeMood] to the library entry named [name] and persists via [saveActiveMood].
     * Returns true on success, false if [name] is not in the library.
     */
    fun setActiveMoodByName(name: String): Boolean {
        val canonical = Mood.canonicalName(name)
        val mood = moodLibrary[canonical] ?: return false
        activeMood = mood
        saveActiveMood()
        return true
    }

    /** Desktop MoodCommand list output for buff triggers on the active mood. */
    fun formatTriggerLine(trigger: MoodTrigger): String =
        "${trigger.effectName} => cast ${trigger.skillName}"

    fun activeTriggerLines(): List<String> =
        activeMood?.effectiveTriggers(moodLibrary)?.map(::formatTriggerLine) ?: emptyList()

    /** Sorted library mood display names (matches ASH `get_moods` / `mood_list`). */
    fun libraryDisplayNames(): List<String> =
        moodLibrary.values.sortedBy { it.displayName() }.map { it.displayName() }

    /**
     * Removes all buff triggers from the active mood and persists.
     * Returns false when there is no active mood.
     */
    fun clearActiveTriggers(): Boolean {
        val mood = activeMood ?: return false
        val cleared = mood.copy(triggers = emptyList())
        activeMood = cleared
        if (cleared.name in moodLibrary) {
            addMoodToLibrary(cleared)
        }
        saveActiveMood()
        saveMoodLibrary()
        return true
    }

    fun formatRemovalTriggerLine(trigger: MoodRemovalTrigger): String =
        when (trigger.type) {
            MoodRemovalTriggerType.UNCONDITIONAL ->
                "${trigger.typeWireName()} => ${trigger.action}"
            else ->
                "${trigger.typeWireName()} ${trigger.effectName} => ${trigger.action}"
        }

    fun activeRemovalTriggerLines(): List<String> =
        activeMood?.effectiveRemovalTriggers(moodLibrary)?.map(::formatRemovalTriggerLine) ?: emptyList()

    /** Desktop EditMoodCommand `getTriggers()` — buff and removal lines combined. */
    fun activeEditMoodLines(): List<String> =
        activeTriggerLines() + activeRemovalTriggerLines()

    /**
     * Adds a removal trigger to the active mood, replacing same type+effect.
     * Returns null when there is no active mood or the trigger is invalid.
     */
    fun addActiveRemovalTrigger(type: String, effectName: String, action: String): MoodRemovalTrigger? {
        val mood = activeMood ?: return null
        val resolvedAction = action.trim().ifBlank { getDefaultAction(type, effectName) }.trim()
        if (resolvedAction.isBlank()) return null

        val line = if (type.equals("unconditional", ignoreCase = true)) {
            "unconditional => $resolvedAction"
        } else {
            "$type $effectName => $resolvedAction"
        }
        val trigger = MoodRemovalTriggerParser.parseLine(line) ?: return null

        val filtered = mood.removalTriggers.filterNot { existing ->
            existing.type == trigger.type &&
                (trigger.type == MoodRemovalTriggerType.UNCONDITIONAL ||
                    existing.effectName.equals(trigger.effectName, ignoreCase = true))
        }
        val updated = mood.copy(removalTriggers = filtered + trigger)
        activeMood = updated
        if (updated.name in moodLibrary) {
            addMoodToLibrary(updated)
        } else if (trigger.type == MoodRemovalTriggerType.LOSE_EFFECT) {
            MoodRemovalKnownSources.register(trigger.effectName, trigger.action)
        }
        return trigger
    }

    /**
     * Removes all buff and removal triggers from the active mood (desktop `editmood clear`).
     */
    fun clearAllActiveTriggers(): Boolean {
        val mood = activeMood ?: return false
        val cleared = mood.copy(triggers = emptyList(), removalTriggers = emptyList())
        activeMood = cleared
        if (cleared.name in moodLibrary) {
            addMoodToLibrary(cleared)
        }
        saveActiveMood()
        saveMoodLibrary()
        return true
    }

    /** Persists the current [moodLibrary] to preferences and settings file. */
    fun saveMoodLibrary() {
        val moods = moodLibrary.values.toList()
        val names = moods.map { it.displayName() }.joinToString("|")
        preferences.setString(Preferences.MOOD_LIBRARY_NAMES, names)
        for (mood in moods) {
            preferences.setString("moodTriggers_${mood.name}", serializeTriggers(mood.triggers))
            preferences.setString(
                "moodRemovalTriggers_${mood.name}",
                serializeRemovalTriggers(mood.removalTriggers),
            )
        }
        saveSettings()
    }

    /** Loads mood library from `{username}_moods.txt`, seeding desktop defaults when absent. */
    fun loadSettings(username: String) {
        settingsUsername = username.trim()
        if (settingsUsername.isEmpty()) {
            moodLibrary = MoodSettingsFile.seededLibrary()
            MoodRemovalKnownSources.rebuildFromLibrary(moodLibrary.values)
            return
        }
        val text = UserDataFileIO.readText(moodsFileName(settingsUsername))
        moodLibrary = if (text.isNullOrBlank()) {
            MoodSettingsFile.seededLibrary()
        } else {
            MoodSettingsFile.parse(text)
        }
        MoodRemovalKnownSources.rebuildFromLibrary(moodLibrary.values)
    }

    /** Writes the mood library to `{username}_moods.txt`. */
    fun saveSettings(username: String = settingsUsername) {
        val resolved = username.trim()
        if (resolved.isEmpty()) return
        settingsUsername = resolved
        val text = MoodSettingsFile.serialize(moodLibrary.values)
        UserDataFileIO.writeText(moodsFileName(resolved), text)
    }

    /**
     * Desktop [MoodManager.updateFromPreferences]: load file, restore active mood, persist.
     */
    fun updateFromPreferences(username: String, activeMoodName: String) {
        loadSettings(username)
        val moodName = activeMoodName.trim().ifBlank { "default" }
        if (!setActiveMoodByName(moodName)) {
            setActiveMoodByName("default")
        }
        saveMoodLibrary()
    }

    /** Restores [moodLibrary] from preferences. Call once after login. */
    fun loadMoodLibrary() {
        val namesRaw = preferences.getString(Preferences.MOOD_LIBRARY_NAMES)
        if (namesRaw.isBlank()) {
            moodLibrary = emptyMap()
            MoodRemovalKnownSources.clear()
            return
        }
        val displayNames = namesRaw.split("|").filter { it.isNotBlank() }
        moodLibrary = displayNames.associate { displayName ->
            val (name, parentNames) = Mood.parseName(displayName)
            val raw = preferences.getString("moodTriggers_$name")
            val removalRaw = preferences.getString("moodRemovalTriggers_$name")
            name to Mood(
                name,
                parseTriggers(raw),
                parentNames,
                parseRemovalTriggers(removalRaw),
            )
        }
        MoodRemovalKnownSources.rebuildFromLibrary(moodLibrary.values)
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

    internal fun serializeRemovalTriggers(triggers: List<MoodRemovalTrigger>): String =
        triggers.joinToString("|") { trigger ->
            "${trigger.typeWireName()} ${trigger.effectName} => ${trigger.action}"
        }

    internal fun parseRemovalTriggers(raw: String): List<MoodRemovalTrigger> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { MoodRemovalTriggerParser.parseLine(it) }
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
    internal open fun isAtSong(effectName: String): Boolean =
        net.sourceforge.kolmafia.data.EffectDatabase.getByName(effectName)
            ?.attributes?.contains("song") == true
}
