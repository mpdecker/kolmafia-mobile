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

    fun parseActiveServantType(html: String): String? {
        val match = compactPattern.find(html) ?: expandedPattern.find(html) ?: return null
        val id = match.groupValues[3].toIntOrNull() ?: return null
        return ServantData.typeForId(id)
    }
}
