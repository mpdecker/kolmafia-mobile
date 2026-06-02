package net.sourceforge.kolmafia.effect

data class EffectState(
    val effects: List<EffectData> = emptyList(),
    val isStale: Boolean = false
)
