package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.AscensionHistoryManager

/**
 * Historical ascension row. Class and path stay as page text; numeric fields are
 * null when the table cell is empty or unparseable.
 */
data class AscensionRecord(
    val number: Int?,
    val className: String,
    val pathName: String,
    val turns: Int?,
    val points: Int?,
)

/** Read-only GET for `ascensionhistory.php`. Never mutates character or Valhalla state. */
open class AscensionHistoryRequest(
    private val client: HttpClient,
    private val manager: AscensionHistoryManager,
    // Injected so DI can supply live session objects; this request never writes them.
    @Suppress("unused")
    private val character: KoLCharacter? = null,
    @Suppress("unused")
    private val preferences: Preferences? = null,
) {
    open suspend fun fetch(playerId: Int? = null): Result<List<AscensionRecord>> {
        return try {
            val response = client.get("$KOL_BASE_URL/$PAGE") {
                parameter("back", BACK_SELF)
                if (playerId != null) {
                    parameter("who", playerId.toString())
                }
            }
            if (!response.status.isSuccess()) {
                Result.failure(IllegalStateException("Ascension history request failed."))
            } else {
                Result.success(parse(response.bodyAsText()))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parse(html: String): List<AscensionRecord> {
        val (name, id) = parsePlayerHeader(html)
        val records = parseRows(html)
        manager.remember(records, name, id)
        return records
    }

    fun parseResponse(html: String): Boolean {
        parse(html)
        return true
    }

    fun records(): List<AscensionRecord> = manager.records()

    fun statusLines(): List<String> = manager.statusLines()

    fun lastCompareSummary(): List<String> = manager.lastCompare().summaryLines()

    companion object {
        const val PAGE = "ascensionhistory.php"
        const val BACK_SELF = "self"

        private val ROW_SPLIT = Regex("""</tr>""", RegexOption.IGNORE_CASE)
        private val CELL = Regex(
            """<td\b[^>]*>([\s\S]*?)</td>""",
            setOf(RegexOption.IGNORE_CASE),
        )
        private val IMG = Regex(
            """<img\b([^>]*)/?>""",
            setOf(RegexOption.IGNORE_CASE),
        )
        private val ATTR = { name: String ->
            Regex("""\b$name\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        }

        fun isHistoryUrl(url: String): Boolean =
            url.contains(PAGE, ignoreCase = true)

        fun parse(html: String): List<AscensionRecord> = parseRows(html)

        fun parsePlayerHeader(html: String): Pair<String?, String?> {
            val match = NAME_PATTERN.find(html) ?: return null to null
            return match.groupValues[2].trim() to match.groupValues[1].trim()
        }

        private val NAME_PATTERN = Regex(
            """who=(\d+)["'] class=nounder><font color=white>(.*?)</font>""",
            RegexOption.IGNORE_CASE,
        )

        fun formatRecord(record: AscensionRecord): String {
            val number = record.number?.let { "#$it" } ?: "#"
            val turns = record.turns?.let { "$it turns" } ?: "unknown turns"
            val points = record.points?.let { " (${it} pts)" } ?: ""
            return "$number ${record.className} / ${record.pathName} — $turns$points"
        }

        private fun parseRows(html: String): List<AscensionRecord> =
            ROW_SPLIT.split(html).mapNotNull { parseRow(it) }

        private fun parseRow(rowHtml: String): AscensionRecord? {
            val cells = CELL.findAll(rowHtml).map { it.groupValues[1] }.toList()
            if (cells.size < 4) return null
            if (visibleText(cells[0]) == "#") return null
            if (visibleText(cells.getOrNull(3).orEmpty()).equals("Class", ignoreCase = true)) return null
            val number = parseInt(cells[0])
            val className = parseClassName(cells.getOrNull(3).orEmpty())
            val turns = parseInt(cells.getOrNull(5).orEmpty())
            val restrictionHtml = restrictionCell(cells)
            val pathName = parsePathName(restrictionHtml)
            val points = parsePoints(restrictionHtml)
            return AscensionRecord(number, className, pathName, turns, points)
        }

        private fun restrictionCell(cells: List<String>): String =
            when {
                cells.size >= 9 -> cells[8]
                cells.size >= 8 -> cells[7]
                else -> cells.last()
            }

        private fun parseClassName(html: String): String {
            val fromImage = imageLabels(html).firstOrNull { it.isNotBlank() }
            if (!fromImage.isNullOrBlank()) return fromImage
            return visibleText(html)
        }

        private fun parsePathName(html: String): String {
            val images = imageEntries(html)
            if (images.isNotEmpty()) {
                val path = images.firstOrNull { !it.isType && !it.isSpacer }?.label.orEmpty()
                return path.ifBlank { "None" }
            }
            val text = visibleText(html)
            return if (text.contains(',')) text.substringAfter(',').trim() else text
        }

        private fun parsePoints(html: String): Int? {
            val images = imageEntries(html)
            if (images.any { it.isHardcore }) return 2
            if (images.any { it.isCasual }) return 1
            if (images.any { it.isSpacer } || images.isNotEmpty()) return 1
            val text = visibleText(html)
            if (text.isEmpty()) return null
            val typeToken = if (text.contains(',')) text.substringBefore(',').trim() else ""
            return when {
                typeToken.equals("Hardcore", ignoreCase = true) -> 2
                typeToken.equals("Normal", ignoreCase = true) -> 1
                typeToken.equals("Casual", ignoreCase = true) -> 1
                else -> null
            }
        }

        private fun parseInt(html: String): Int? {
            val text = visibleText(html).replace(",", "")
            if (text.isEmpty()) return null
            return text.toIntOrNull()
        }

        private fun visibleText(html: String): String =
            html.replace("&nbsp;", " ", ignoreCase = true)
                .replace(Regex("<[^>]+>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

        private fun imageLabels(html: String): List<String> =
            imageEntries(html).map { it.label }.filter { it.isNotBlank() }

        private fun imageEntries(html: String): List<ImageEntry> =
            IMG.findAll(html).map { match ->
                val attrs = match.groupValues[1]
                val src = ATTR("src").find(attrs)?.groupValues?.getOrNull(1).orEmpty()
                val alt = ATTR("alt").find(attrs)?.groupValues?.getOrNull(1).orEmpty()
                val title = ATTR("title").find(attrs)?.groupValues?.getOrNull(1).orEmpty()
                val label = alt.ifBlank { title }.trim()
                ImageEntry(src = src, label = label)
            }.toList()

        private data class ImageEntry(val src: String, val label: String) {
            val isSpacer: Boolean
                get() = src.contains("spacer", ignoreCase = true) ||
                    label.equals("spacer", ignoreCase = true)
            val isHardcore: Boolean
                get() = src.contains("hardcore", ignoreCase = true) ||
                    label.equals("Hardcore", ignoreCase = true)
            val isCasual: Boolean
                get() = src.contains("beanbag", ignoreCase = true) ||
                    label.equals("Casual", ignoreCase = true)
            val isType: Boolean get() = isHardcore || isCasual
        }
    }
}
