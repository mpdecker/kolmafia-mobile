package net.sourceforge.kolmafia.character

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.sourceforge.kolmafia.familiar.FamiliarManager

/** Parses terrarium soup JSON from familiar-page HTML (desktop `FamiliarData.parseSoup`). */
object FamiliarSoupSync {

    const val SYNAPTIC_SOUP = 11621
    const val MUSCULAR_SOUP = 11622
    const val FLAGELLATE_SOUP = 11623
    const val ELBOW_SOUP = 11624
    const val LIP_SOUP = 11625

    val protogeneticSoupIds: Set<Int> = setOf(
        SYNAPTIC_SOUP,
        MUSCULAR_SOUP,
        FLAGELLATE_SOUP,
        ELBOW_SOUP,
        LIP_SOUP,
    )

    private val soupCommentPattern = Regex("""<!-- some soup for you! "(.*?)" -->""")
    private val json = Json { ignoreUnknownKeys = true }

    fun containsSoupComment(html: String): Boolean =
        html.contains("some soup for you!")

    fun soupAttributeForItem(itemId: Int): String? = when (itemId) {
        SYNAPTIC_SOUP -> "mp"
        MUSCULAR_SOUP -> "damage"
        FLAGELLATE_SOUP -> "act"
        ELBOW_SOUP -> "hp"
        LIP_SOUP -> "stats"
        else -> null
    }

    fun parse(html: String): Map<Int, SoupEntry> {
        val match = soupCommentPattern.find(html) ?: return emptyMap()
        val soupJson = match.groupValues[1].replace("\\\"", "\"")
        return try {
            parseSoupJson(soupJson)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun apply(html: String, familiarManager: FamiliarManager?) {
        if (familiarManager == null || !containsSoupComment(html)) return
        for ((familiarId, entry) in parse(html)) {
            familiarManager.applySoupData(familiarId, entry.times, entry.attributes)
        }
    }

    fun applyProtogeneticSoupUse(
        itemId: Int,
        html: String,
        familiarId: Int,
        familiarManager: FamiliarManager?,
    ) {
        if (familiarManager == null || itemId !in protogeneticSoupIds || familiarId <= 0) return
        familiarManager.incrementSoup(familiarId, soupAttributeForItem(itemId))
        if (containsSoupComment(html)) {
            apply(html, familiarManager)
        }
    }

    private fun parseSoupJson(rawJson: String): Map<Int, SoupEntry> {
        val root = json.parseToJsonElement(rawJson).jsonObject
        val result = linkedMapOf<Int, SoupEntry>()
        for ((key, value) in root) {
            val familiarId = key.toIntOrNull() ?: continue
            val obj = value.jsonObject
            val times = obj["times"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val attributes = obj["attr"]?.let(::parseStringArray) ?: emptyList()
            result[familiarId] = SoupEntry(times, attributes)
        }
        return result
    }

    private fun parseStringArray(element: kotlinx.serialization.json.JsonElement): List<String> =
        when (element) {
            is JsonArray -> element.mapNotNull { it.jsonPrimitive.content.takeIf(String::isNotBlank) }
            else -> emptyList()
        }

    data class SoupEntry(val times: Int, val attributes: List<String>)
}
