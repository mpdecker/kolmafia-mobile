package net.sourceforge.kolmafia.servant

import net.sourceforge.kolmafia.modifiers.ServantData

/**
 * Parses Ed choice 1053 HTML for summoned servants. Mirrors desktop [EdServantData.inspectServants].
 */
object EdServantChoiceSync {

    private val busyPattern = Regex(
        """<b>Busy Servant</b>: <img.*?itemimages/(edserv\d+\.gif).*?>(.*?), the (.*?) \(lvl\.? (\d+),? ([\d,]+) XP\)""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val freedTablePattern = Regex(
        """<b>Freed, but Lazy Servants</b><table>(.*?)</table>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val freedRowPattern = Regex(
        """itemimages/(edserv\d+\.gif).*?</td>\s*<td>(.*?), the (.*?)<br>.*?\(level (\d+), ([\d,]+) xp\)""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val servantIdPattern = Regex("""edserv(\d+)\.gif""")

    data class ParseResult(
        val records: List<EdServantRecord>,
        val summonedTypes: List<String>,
        val activeType: String?,
    )

    fun parse(html: String): ParseResult {
        val records = linkedMapOf<String, EdServantRecord>()
        var active: String? = null

        if (html.contains("Busy Servant")) {
            busyPattern.find(html)?.let { match ->
                recordFromMatch(match)?.let { record ->
                    records[record.type] = record
                    active = record.type
                }
            }
        }

        freedTablePattern.find(html)?.groupValues?.get(1)?.let { tableHtml ->
            freedRowPattern.findAll(tableHtml).forEach { row ->
                recordFromMatch(row)?.let { records[it.type] = it }
            }
        }

        val list = records.values.toList()
        return ParseResult(list, list.map { it.type }, active)
    }

    fun parseSummonedTypes(html: String): List<String> = parse(html).summonedTypes

    private fun recordFromMatch(match: MatchResult): EdServantRecord? {
        val image = match.groupValues[1]
        val name = match.groupValues[2].trim()
        val level = match.groupValues[4].toIntOrNull() ?: return null
        val experience = match.groupValues[5].replace(",", "").toIntOrNull() ?: return null
        val type = servantTypeFromImage(image) ?: return null
        return EdServantRecord(type = type, name = name, level = level, experience = experience)
    }

    private fun servantTypeFromImage(image: String): String? {
        val id = servantIdPattern.find(image)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        return ServantData.typeForId(id)
    }
}
