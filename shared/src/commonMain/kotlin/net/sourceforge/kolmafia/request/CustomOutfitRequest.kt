package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.OutfitData
import net.sourceforge.kolmafia.http.KOL_BASE_URL

open class CustomOutfitRequest(
    private val client: HttpClient,
    private val gameDatabase: GameDatabase? = null,
) {

    companion object {
        private val LIST_PATTERN = Regex("""<form name=manageoutfits.*?</form>""", RegexOption.DOT_MATCHES_ALL)
    }

    open suspend fun fetchCustomOutfits(
        outfitOptions: List<Pair<Int, String>>,
    ): List<OutfitData> {
        val html = try {
            client.get("$KOL_BASE_URL/account_manageoutfits.php").bodyAsText()
        } catch (_: Exception) {
            return outfitOptions.filter { it.first < 0 }.map { (id, name) ->
                OutfitData(id = id, name = name, image = "", equipment = emptyList(), halloweenDrops = emptyList())
            }
        }

        val parsed = parseManageOutfits(html)
        val legacyPieces = parseOutfitPieces(html)
        return outfitOptions.filter { it.first < 0 }.map { (id, name) ->
            val pieces = parsed[id]?.pieces ?: legacyPieces[id] ?: emptyList()
            OutfitData(
                id = id,
                name = parsed[id]?.name ?: name,
                image = "",
                equipment = pieces,
                halloweenDrops = emptyList(),
            )
        }
    }

    data class ParsedCustomOutfit(val name: String, val pieces: List<String>)

    fun parseManageOutfits(html: String): Map<Int, ParsedCustomOutfit> {
        val list = LIST_PATTERN.find(html)?.value ?: return emptyMap()
        val result = mutableMapOf<Int, ParsedCustomOutfit>()
        for (match in EquipmentRequest.MANAGE_ENTRY_PATTERN.findAll(list)) {
            val positiveId = match.groupValues[1].toIntOrNull() ?: continue
            val id = -positiveId
            val name = match.groupValues[2].trim()
            if (name.equals("Your Previous Outfit", ignoreCase = true)) continue
            val pieces = match.groupValues[3]
                .split("<br>", "<BR>")
                .map { it.replace(Regex("<[^>]+>"), "").trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { pieceName ->
                    gameDatabase?.item(pieceName)?.name ?: pieceName.takeIf { it.isNotBlank() }
                }
            result[id] = ParsedCustomOutfit(name, pieces)
        }
        return result
    }

    /** Legacy parser kept for unit tests using comment blocks. */
    internal fun parseOutfitPieces(html: String): Map<Int, List<String>> {
        val comment = Regex("""<!--\s*outfitid:\s*(-?\d+)\s*-->""", RegexOption.IGNORE_CASE)
        val matches = comment.findAll(html).toList()
        val result = mutableMapOf<Int, List<String>>()
        for ((index, match) in matches.withIndex()) {
            val id = match.groupValues[1].toIntOrNull() ?: continue
            if (id >= 0) continue
            val start = match.range.last + 1
            val end = matches.getOrNull(index + 1)?.range?.first ?: html.length
            val section = html.substring(start, end)
            val pieces = Regex("""<li[^>]*>([^<]+)</li>""")
                .findAll(section)
                .map { it.groupValues[1].trim() }
                .filter { it.isNotEmpty() }
                .toList()
            if (pieces.isNotEmpty()) result[id] = pieces
        }
        return result
    }
}
