package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Afterlife astral shop summary data from [TCRS.astral_pets.txt] and
 * [TCRS.astral_consumables.txt]. Mirrors desktop relay afterlife.1.ash file_to_map lookups.
 */
@OptIn(ExperimentalResourceApi::class)
object TCRSAstralDatabase {

    data class AstralKey(
        val className: String,
        val signName: String,
        val itemId: Int,
    )

    data class ConsumableEntry(
        val size: Int,
        val effectName: String? = null,
        val effectDuration: Int? = null,
        val rawValue: String = "",
    )

    private val petModifiers = linkedMapOf<AstralKey, String>()
    private val consumableEntries = linkedMapOf<AstralKey, ConsumableEntry>()
    private var loaded = false

    val isLoaded: Boolean get() = loaded
    val petEntryCount: Int get() = petModifiers.size
    val consumableEntryCount: Int get() = consumableEntries.size

    suspend fun load() {
        if (loaded) return
        val pets = Res.readBytes("files/data/TCRS.astral_pets.txt").decodeToString()
        val consumables = Res.readBytes("files/data/TCRS.astral_consumables.txt").decodeToString()
        applyParse(parsePetText(pets), parseConsumableText(consumables))
        loaded = true
    }

    fun getPetModifiers(className: String, signName: String, itemId: Int): String {
        if (!TCRSDatabase.validate(className, signName)) return ""
        return petModifiers[AstralKey(className, signName, itemId)] ?: ""
    }

    fun getConsumableEntry(className: String, signName: String, itemId: Int): ConsumableEntry? {
        if (!TCRSDatabase.validate(className, signName)) return null
        return consumableEntries[AstralKey(className, signName, itemId)]
    }

    fun hasPetEntry(className: String, signName: String, itemId: Int): Boolean =
        getPetModifiers(className, signName, itemId).isNotEmpty()

    fun hasConsumableEntry(className: String, signName: String, itemId: Int): Boolean =
        getConsumableEntry(className, signName, itemId) != null

    internal fun parsePetTextForTest(text: String): Map<AstralKey, String> = parsePetText(text)

    internal fun parseConsumableTextForTest(text: String): Map<AstralKey, ConsumableEntry> =
        parseConsumableText(text)

    internal fun resetForTest() {
        petModifiers.clear()
        consumableEntries.clear()
        loaded = false
    }

    internal fun injectForTest(
        pets: Map<AstralKey, String>,
        consumables: Map<AstralKey, ConsumableEntry>,
    ) {
        applyParse(pets, consumables)
        loaded = true
    }

    private fun applyParse(
        pets: Map<AstralKey, String>,
        consumables: Map<AstralKey, ConsumableEntry>,
    ) {
        petModifiers.clear()
        petModifiers.putAll(pets)
        consumableEntries.clear()
        consumableEntries.putAll(consumables)
    }

    private fun parsePetText(text: String): Map<AstralKey, String> {
        val map = linkedMapOf<AstralKey, String>()
        for (row in parseRows(text)) {
            map[row.key] = row.rawValue
        }
        return map
    }

    private fun parseConsumableText(text: String): Map<AstralKey, ConsumableEntry> {
        val map = linkedMapOf<AstralKey, ConsumableEntry>()
        for (row in parseRows(text)) {
            map[row.key] = parseConsumableValue(row.rawValue)
        }
        return map
    }

    private data class ParsedRow(val key: AstralKey, val rawValue: String)

    private fun parseRows(text: String): List<ParsedRow> {
        val rows = mutableListOf<ParsedRow>()
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trimEnd('\r', '\n')
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val cols = line.split('\t')
            if (cols.size < 4) continue

            val className = cols[0].trim()
            val signName = cols[1].trim()
            if (!TCRSDatabase.validate(className, signName)) continue

            val itemId = parseItemId(cols[2].trim()) ?: continue
            val rawValue = cols[3].trim()
            rows.add(ParsedRow(AstralKey(className, signName, itemId), rawValue))
        }
        return rows
    }

    private fun parseItemId(token: String): Int? {
        val match = itemBracketPattern.matchEntire(token) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    internal fun parseConsumableValue(raw: String): ConsumableEntry {
        val slashIndex = raw.indexOf('/')
        val sizePart = if (slashIndex >= 0) raw.substring(0, slashIndex) else raw
        val suffix = if (slashIndex >= 0 && slashIndex + 1 < raw.length) {
            raw.substring(slashIndex + 1)
        } else {
            ""
        }

        val size = sizePart.toIntOrNull() ?: 0
        if (suffix.isEmpty()) {
            return ConsumableEntry(size = size, rawValue = raw)
        }

        val effectMatch = effectPattern.find(suffix)
        return if (effectMatch != null) {
            ConsumableEntry(
                size = size,
                effectName = effectMatch.groupValues[1],
                effectDuration = effectMatch.groupValues[2].toIntOrNull(),
                rawValue = raw,
            )
        } else {
            ConsumableEntry(size = size, rawValue = raw)
        }
    }

    private val itemBracketPattern = Regex("""^\[(\d+)\].*$""")
    private val effectPattern =
        Regex("""Effect: "(.*?)", Effect Duration: (\d+)""")
}
