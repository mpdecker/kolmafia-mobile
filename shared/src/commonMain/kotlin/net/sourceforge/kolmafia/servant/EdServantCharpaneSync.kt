package net.sourceforge.kolmafia.servant

import net.sourceforge.kolmafia.modifiers.ServantData

/**
 * Parses Ed active servant from charpane HTML. Mirrors desktop [CharPaneRequest.checkServant].
 */
object EdServantCharpaneSync {

    private val compactPattern = Regex(
        """<b>Servant:</b>.*?target="mainpane">(.*?) \(lvl (\d+)\).*?edserv(\d+)\.gif""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val expandedPattern = Regex(
        """<b>Servant:</b>.*?target="mainpane">(.*?) the (\d+) level.*?edserv(\d+)\.gif""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parseActiveServantType(html: String): String? = parseActiveServant(html)?.type

    fun parseActiveServant(html: String): EdServantRecord? {
        val match = compactPattern.find(html) ?: expandedPattern.find(html) ?: return null
        val id = match.groupValues[3].toIntOrNull() ?: return null
        val type = ServantData.typeForId(id) ?: return null
        val level = match.groupValues[2].toIntOrNull() ?: return null
        val name = match.groupValues[1].trim()
        return EdServantRecord(type = type, name = name, level = level, experience = level * level)
    }
}
