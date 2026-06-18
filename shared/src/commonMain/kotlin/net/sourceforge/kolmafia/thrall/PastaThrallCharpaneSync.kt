package net.sourceforge.kolmafia.thrall

/**
 * Parses active pasta thrall from charpane HTML. Mirrors desktop [CharPaneRequest.checkPastaThrall].
 */
object PastaThrallCharpaneSync {

    data class ParsedThrall(
        val customName: String,
        val level: Int,
        val type: String,
    )

    private val pattern = Regex(
        """desc_guardian.php.*?itemimages/(.*?.gif).*?<b>(.*?)</b>.*?the Lvl. (\d+) (.*?)</font>""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parse(html: String): ParsedThrall? {
        val match = pattern.find(html) ?: return null
        val customName = decodeHtmlEntities(match.groupValues[2].trim())
        val level = match.groupValues[3].toIntOrNull() ?: return null
        val type = match.groupValues[4].trim()
        if (type.isBlank()) return null
        return ParsedThrall(customName, level, type)
    }

    private fun decodeHtmlEntities(text: String): String =
        text
            .replace("&amp;", "&")
            .replace("&Aring;", "Å")
            .replace("&aring;", "å")
            .replace("&Eacute;", "É")
            .replace("&eacute;", "é")
            .replace("&Auml;", "Ä")
            .replace("&auml;", "ä")
            .replace("&Ouml;", "Ö")
            .replace("&ouml;", "ö")
            .replace("&Uuml;", "Ü")
            .replace("&uuml;", "ü")
}
