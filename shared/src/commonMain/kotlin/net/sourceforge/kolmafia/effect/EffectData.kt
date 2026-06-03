package net.sourceforge.kolmafia.effect

import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectQuality

data class EffectData(
    val id: Int,
    val name: String,
    val duration: Int       // turns remaining; -1 = intrinsic / permanent
) {
    val isIntrinsic: Boolean get() = duration < 0
    val isExpiringSoon: Boolean get() = duration in 1..5

    // Quality from the bundled static database (loaded at startup).
    // Returns UNKNOWN before GameDatabase.load() completes or for unknown effects.
    val quality: EffectQuality
        get() = EffectDatabase.getByName(name)?.quality ?: EffectQuality.UNKNOWN

    val isGood: Boolean    get() = quality == EffectQuality.GOOD
    val isBad: Boolean     get() = quality == EffectQuality.BAD
    val isNeutral: Boolean get() = quality == EffectQuality.NEUTRAL
}
