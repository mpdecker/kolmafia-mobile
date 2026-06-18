package net.sourceforge.kolmafia.vykea

import net.sourceforge.kolmafia.modifiers.VykeaCompanionData

/**
 * Parses VYKEA companion from charpane HTML. Mirrors desktop [CharPaneRequest.checkVYKEACompanion].
 */
object VykeaCharpaneSync {

    private val outerPattern = Regex(
        """<b>VYKEA Companion</b></font><br><font size=2>(.*?)<br>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val innerPattern = Regex(
        """<b>(.*?)</b> the level (\d).*(bookshelf|ceiling fan|couch|dishrack|dresser|lamp)""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parse(html: String, savedRune: String): VykeaCompanionData.Companion? {
        val block = outerPattern.find(html)?.groupValues?.get(1) ?: return null
        val match = innerPattern.find(block) ?: return null
        val name = decodeHtmlEntities(match.groupValues[1].trim())
        val level = match.groupValues[2].toIntOrNull() ?: return null
        val type = VykeaCompanionData.typeFromString(match.groupValues[3]) ?: return null
        val rune = VykeaCompanionData.runeFromString(savedRune)
        return VykeaCompanionData.Companion(type, level, rune, name)
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
