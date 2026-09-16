package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Ordered TCRS word/enchantment tables from bundled `tcrs.txt`.
 * Order matters: the RNG indexes by position, so the file must never be sorted or de-duplicated.
 */
@OptIn(ExperimentalResourceApi::class)
object TCRSStringTables {

    private val strings = mutableMapOf<String, MutableList<String>>()
    private val foodSize = mutableMapOf<Int, MutableList<String>>()
    private val boozeSize = mutableMapOf<Int, MutableList<String>>()
    private val foodQuality = mutableMapOf<ConsumableQuality, MutableList<String>>()
    private val boozeQuality = mutableMapOf<ConsumableQuality, MutableList<String>>()
    private val equipmentModifiers = mutableListOf<Pair<String, String>>()
    private var adjectives: Set<String> = emptySet()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/tcrs.txt").decodeToString()
        loadFromText(text)
    }

    fun loadFromText(text: String) {
        strings.clear()
        foodSize.clear()
        boozeSize.clear()
        foodQuality.clear()
        boozeQuality.clear()
        equipmentModifiers.clear()
        for (raw in text.lineSequence()) {
            val line = raw.trimEnd('\r', '\n')
            if (line.isBlank() || line.startsWith('#')) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue
            val cols = line.split('\t')
            if (cols.isEmpty()) continue
            when (cols[0]) {
                "Food Size" -> {
                    val bucket = cols.getOrNull(1)?.toIntOrNull() ?: continue
                    foodSize.getOrPut(bucket) { mutableListOf() }.add(cols.getOrElse(2) { "" })
                }
                "Booze Size" -> {
                    val bucket = cols.getOrNull(1)?.toIntOrNull() ?: continue
                    boozeSize.getOrPut(bucket) { mutableListOf() }.add(cols.getOrElse(2) { "" })
                }
                "Food Quality" -> {
                    val quality = ConsumableQuality.fromEnumName(cols.getOrElse(1) { "" }) ?: continue
                    foodQuality.getOrPut(quality) { mutableListOf() }.add(cols.getOrElse(2) { "" })
                }
                "Booze Quality" -> {
                    val quality = ConsumableQuality.fromEnumName(cols.getOrElse(1) { "" }) ?: continue
                    boozeQuality.getOrPut(quality) { mutableListOf() }.add(cols.getOrElse(2) { "" })
                }
                "Equipment Enchant" -> {
                    val descriptor = cols.getOrElse(1) { "" }
                    val mods = cols.getOrElse(2) { "" }
                    equipmentModifiers += descriptor to mods
                }
                else -> {
                    val word = cols.getOrElse(1) { "" }
                    strings.getOrPut(cols[0]) { mutableListOf() }.add(word)
                }
            }
        }
        adjectives = strings["Adjective"].orEmpty().toSet()
        loaded = true
    }

    fun isLoaded(): Boolean = loaded

    fun list(tag: String): List<String> = strings[tag].orEmpty()

    fun adjectives(): Set<String> = adjectives

    fun foodSize(bucket: Int): List<String> = foodSize[bucket].orEmpty()

    fun boozeSize(bucket: Int): List<String> = boozeSize[bucket].orEmpty()

    fun foodQuality(quality: ConsumableQuality): List<String> = foodQuality[quality].orEmpty()

    fun boozeQuality(quality: ConsumableQuality): List<String> = boozeQuality[quality].orEmpty()

    fun equipmentModifiers(): List<Pair<String, String>> = equipmentModifiers

    internal fun resetForTest() {
        strings.clear()
        foodSize.clear()
        boozeSize.clear()
        foodQuality.clear()
        boozeQuality.clear()
        equipmentModifiers.clear()
        adjectives = emptySet()
        loaded = false
    }
}
