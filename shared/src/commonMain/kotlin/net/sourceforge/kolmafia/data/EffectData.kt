package net.sourceforge.kolmafia.data

enum class EffectQuality { GOOD, BAD, NEUTRAL, UNKNOWN }

data class EffectData(
    val id: Int,
    val name: String,
    val image: String,
    val descId: String,
    val quality: EffectQuality,
    val attributes: Set<String>
)
