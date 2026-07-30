package net.sourceforge.kolmafia.data

enum class EffectQuality {
    GOOD,
    BAD,
    NEUTRAL,
    UNKNOWN;

    fun description(): String = when (this) {
        GOOD -> "good"
        BAD -> "bad"
        NEUTRAL -> "neutral"
        UNKNOWN -> ""
    }
}

data class EffectData(
    val id: Int,
    val name: String,
    val image: String,
    val descId: String,
    val quality: EffectQuality,
    val attributes: Set<String>,
    val actions: String? = null,
) {
    fun combinedAttributes(): String =
        if (attributes.isEmpty()) "" else attributes.joinToString(",")

    fun qualityDescription(): String = quality.description()

    fun isSong(): Boolean = attributes.contains("song")
}
