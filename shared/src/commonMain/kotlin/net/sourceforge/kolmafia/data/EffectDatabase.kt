package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses statuseffects.txt from bundled compose resources.
// Format (tab-separated): id  name  image  descid  quality  attributes  default_action
@OptIn(ExperimentalResourceApi::class)
object EffectDatabase {

    private val byId = mutableMapOf<Int, EffectData>()
    private val byName = mutableMapOf<String, EffectData>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/statuseffects.txt").decodeToString()
        parse(text)
        loaded = true
    }

    fun getById(id: Int): EffectData? = byId[id]
    fun getByName(name: String): EffectData? = byName[name.lowercase()]
    fun all(): Collection<EffectData> = byId.values
    fun goodEffects(): List<EffectData> = byId.values.filter { it.quality == EffectQuality.GOOD }
    fun badEffects(): List<EffectData> = byId.values.filter { it.quality == EffectQuality.BAD }

    private fun parse(text: String) {
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val firstToken = line.substringBefore('\t')
            if (firstToken.toIntOrNull() != null && line.count { it == '\t' } < 3) continue

            val parts = line.split('\t')
            if (parts.size < 6) continue

            val id = parts[0].toIntOrNull() ?: continue
            val name = parts[1]
            val image = parts[2]
            val descId = parts[3]
            val quality = when (parts[4].trim().lowercase()) {
                "good" -> EffectQuality.GOOD
                "bad" -> EffectQuality.BAD
                "neutral" -> EffectQuality.NEUTRAL
                else -> EffectQuality.UNKNOWN
            }
            val attrStr = parts[5].trim()
            val attributes = if (attrStr == "none") emptySet()
                             else attrStr.split(',').map { it.trim() }.toSet()

            val effect = EffectData(id, name, image, descId, quality, attributes)
            byId[id] = effect
            byName[name.lowercase()] = effect
        }
    }
}
