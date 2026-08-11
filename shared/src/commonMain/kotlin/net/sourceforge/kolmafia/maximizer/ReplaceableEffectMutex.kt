package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.effect.EffectData

/** Desktop [Speculation.addEffect] replaceable MutexER peer removal before effect gain. */
object ReplaceableEffectMutex {

    fun applyEffectGain(
        activeEffects: List<EffectData>,
        gained: EffectData,
    ): List<EffectData> {
        if (activeEffects.any {
                it.id == gained.id || it.name.equals(gained.name, ignoreCase = true)
            }
        ) {
            return activeEffects
        }
        val peers = ModifierDatabase.getReplaceableMutexFor(gained.id)
        val filtered = if (peers.isEmpty()) {
            activeEffects
        } else {
            activeEffects.filter { it.id !in peers }
        }
        return filtered + gained
    }
}
