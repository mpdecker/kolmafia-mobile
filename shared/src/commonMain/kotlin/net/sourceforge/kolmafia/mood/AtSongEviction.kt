package net.sourceforge.kolmafia.mood

import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.request.UneffectRequest

/** Shared AT song slot state across buff and removal cast passes in one mood execute. */
class AtSongSlotTracker(
    val locallyEvicted: MutableSet<Int> = mutableSetOf(),
    var locallyAdded: Int = 0,
)

/** Desktop [MoodManager.execute] AT song pre-pass + per-cast slot eviction. */
object AtSongEviction {

    /** Desktop [MoodTrigger.isThiefTrigger]. */
    fun isThiefBuffTrigger(trigger: MoodTrigger): Boolean =
        SkillDefinitionProxy.isAccordionThiefSong(trigger.skillId)

    /**
     * Returns effect ids to uneffect during desktop pre-pass (orphan songs first, active-order).
     * Pure function for unit tests.
     */
    fun computePrePassEvictionIds(
        effectState: EffectState,
        moodTriggers: List<MoodTrigger>,
        songLimit: Int,
        isAtSong: (String) -> Boolean,
        locallyEvicted: Set<Int> = emptySet(),
    ): List<Int> {
        if (songLimit <= 0) return emptyList()

        val thiefBuffs = mutableListOf<EffectData>()
        for (effect in effectState.effects) {
            if (effect.id in locallyEvicted) continue
            if (!isAtSong(effect.name)) continue
            thiefBuffs.add(effect)
        }

        val thiefKeep = mutableListOf<EffectData>()
        val thiefNeed = mutableListOf<MoodTrigger>()

        for (trigger in moodTriggers) {
            if (!isThiefBuffTrigger(trigger)) continue
            val active = effectState.effects.firstOrNull {
                it.id == trigger.effectId && it.id !in locallyEvicted
            }
            if (active != null && thiefBuffs.remove(active)) {
                thiefKeep.add(active)
            } else {
                thiefNeed.add(trigger)
            }
        }

        if (thiefNeed.isEmpty()) return emptyList()

        val buffsToRemove =
            thiefBuffs.size + thiefKeep.size + thiefNeed.size - songLimit
        if (buffsToRemove <= 0) return emptyList()

        return thiefBuffs.take(buffsToRemove).map { it.id }
    }

    /** Desktop pre-pass: shrug orphan/extra AT songs before casting mood thief skills. */
    suspend fun prePassEvict(
        effectState: EffectState,
        moodTriggers: List<MoodTrigger>,
        songLimit: Int,
        isAtSong: (String) -> Boolean,
        uneffectRequest: UneffectRequest?,
        tracker: AtSongSlotTracker,
    ) {
        for (effectId in computePrePassEvictionIds(
            effectState,
            moodTriggers,
            songLimit,
            isAtSong,
            tracker.locallyEvicted,
        )) {
            uneffectRequest?.uneffect(effectId)
            tracker.locallyEvicted += effectId
        }
    }

    /**
     * Per-cast slot management before casting an AT song.
     * Updates [tracker] like desktop per-trigger eviction during execute.
     */
    suspend fun evictBeforeCast(
        effectName: String,
        effectState: EffectState,
        songLimit: Int,
        moodTriggers: List<MoodTrigger>,
        isAtSong: (String) -> Boolean,
        uneffectRequest: UneffectRequest?,
        tracker: AtSongSlotTracker,
    ) {
        if (songLimit <= 0 || !isAtSong(effectName)) return

        val activeSongs = effectState.effects.filter {
            isAtSong(it.name) && it.id !in tracker.locallyEvicted
        }
        val effectiveCount = activeSongs.size + tracker.locallyAdded
        if (effectiveCount >= songLimit) {
            val toEvict = lowestPriorityActiveSong(activeSongs, moodTriggers)
            if (toEvict != null) {
                uneffectRequest?.uneffect(toEvict.id)
                tracker.locallyEvicted += toEvict.id
            } else {
                tracker.locallyAdded = (tracker.locallyAdded - 1).coerceAtLeast(0)
            }
        }
        tracker.locallyAdded++
    }

    /**
     * Returns the active AT song with the lowest priority in the current mood.
     * "Lowest priority" = the active song whose effectId appears LAST in [moodTriggers].
     * Songs not present in the mood trigger list are treated as lowest priority (evicted first).
     */
    fun lowestPriorityActiveSong(
        activeSongs: List<EffectData>,
        moodTriggers: List<MoodTrigger>,
    ): EffectData? {
        if (activeSongs.isEmpty()) return null
        val triggerEffectIds = moodTriggers.map { it.effectId }
        return activeSongs.maxByOrNull { song ->
            val idx = triggerEffectIds.lastIndexOf(song.id)
            if (idx < 0) Int.MAX_VALUE else idx
        }
    }
}
