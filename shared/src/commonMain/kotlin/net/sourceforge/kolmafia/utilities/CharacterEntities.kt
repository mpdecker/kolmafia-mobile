package net.sourceforge.kolmafia.utilities

/**
 * Minimal port of desktop `CharacterEntities` for `entity_encode` / `entity_decode`.
 * Covers the standard named HTML entities used in KoL HTML.
 */
object CharacterEntities {
    private val ENTITIES: Array<Pair<String, Char>> = arrayOf(
        "&amp;" to '&',
        "&lt;" to '<',
        "&gt;" to '>',
        "&quot;" to '"',
        "&apos;" to '\'',
        "&nbsp;" to '\u00A0',
        "&iexcl;" to '\u00A1',
        "&cent;" to '\u00A2',
        "&pound;" to '\u00A3',
        "&curren;" to '\u00A4',
        "&yen;" to '\u00A5',
        "&brvbar;" to '\u00A6',
        "&sect;" to '\u00A7',
        "&uml;" to '\u00A8',
        "&copy;" to '\u00A9',
        "&ordf;" to '\u00AA',
        "&laquo;" to '\u00AB',
        "&not;" to '\u00AC',
        "&shy;" to '\u00AD',
        "&reg;" to '\u00AE',
        "&macr;" to '\u00AF',
        "&deg;" to '\u00B0',
        "&plusmn;" to '\u00B1',
        "&sup2;" to '\u00B2',
        "&sup3;" to '\u00B3',
        "&acute;" to '\u00B4',
        "&micro;" to '\u00B5',
        "&para;" to '\u00B6',
        "&middot;" to '\u00B7',
        "&cedil;" to '\u00B8',
        "&sup1;" to '\u00B9',
        "&ordm;" to '\u00BA',
        "&raquo;" to '\u00BB',
        "&frac14;" to '\u00BC',
        "&frac12;" to '\u00BD',
        "&frac34;" to '\u00BE',
        "&iquest;" to '\u00BF',
        "&Agrave;" to '\u00C0',
        "&Aacute;" to '\u00C1',
        "&Acirc;" to '\u00C2',
        "&Atilde;" to '\u00C3',
        "&Auml;" to '\u00C4',
        "&Aring;" to '\u00C5',
        "&AElig;" to '\u00C6',
        "&Ccedil;" to '\u00C7',
        "&Egrave;" to '\u00C8',
        "&Eacute;" to '\u00C9',
        "&Ecirc;" to '\u00CA',
        "&Euml;" to '\u00CB',
        "&Igrave;" to '\u00CC',
        "&Iacute;" to '\u00CD',
        "&Icirc;" to '\u00CE',
        "&Iuml;" to '\u00CF',
        "&ETH;" to '\u00D0',
        "&Ntilde;" to '\u00D1',
        "&Ograve;" to '\u00D2',
        "&Oacute;" to '\u00D3',
        "&Ocirc;" to '\u00D4',
        "&Otilde;" to '\u00D5',
        "&Ouml;" to '\u00D6',
        "&times;" to '\u00D7',
        "&Oslash;" to '\u00D8',
        "&Ugrave;" to '\u00D9',
        "&Uacute;" to '\u00DA',
        "&Ucirc;" to '\u00DB',
        "&Uuml;" to '\u00DC',
        "&Yacute;" to '\u00DD',
        "&THORN;" to '\u00DE',
        "&szlig;" to '\u00DF',
        "&agrave;" to '\u00E0',
        "&aacute;" to '\u00E1',
        "&acirc;" to '\u00E2',
        "&atilde;" to '\u00E3',
        "&auml;" to '\u00E4',
        "&aring;" to '\u00E5',
        "&aelig;" to '\u00E6',
        "&ccedil;" to '\u00E7',
        "&egrave;" to '\u00E8',
        "&eacute;" to '\u00E9',
        "&ecirc;" to '\u00EA',
        "&euml;" to '\u00EB',
        "&igrave;" to '\u00EC',
        "&iacute;" to '\u00ED',
        "&icirc;" to '\u00EE',
        "&iuml;" to '\u00EF',
        "&eth;" to '\u00F0',
        "&ntilde;" to '\u00F1',
        "&ograve;" to '\u00F2',
        "&oacute;" to '\u00F3',
        "&ocirc;" to '\u00F4',
        "&otilde;" to '\u00F5',
        "&ouml;" to '\u00F6',
        "&divide;" to '\u00F7',
        "&oslash;" to '\u00F8',
        "&ugrave;" to '\u00F9',
        "&uacute;" to '\u00FA',
        "&ucirc;" to '\u00FB',
        "&uuml;" to '\u00FC',
        "&yacute;" to '\u00FD',
        "&thorn;" to '\u00FE',
        "&yuml;" to '\u00FF',
        "&ndash;" to '\u2013',
        "&mdash;" to '\u2014',
        "&lsquo;" to '\u2018',
        "&rsquo;" to '\u2019',
        "&ldquo;" to '\u201C',
        "&rdquo;" to '\u201D',
        "&trade;" to '\u2122',
    )

    private val charToEntity: Map<Char, String> = ENTITIES.associate { (entity, ch) -> ch to entity }
    private val entityToChar: Map<String, Char> = ENTITIES.associate { (entity, ch) -> entity to ch }

    fun escape(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val entity = charToEntity[ch]
            if (entity != null) sb.append(entity) else sb.append(ch)
        }
        return sb.toString()
    }

    fun unescape(input: String): String {
        if (!input.contains('&')) return input
        val sb = StringBuilder(input.length)
        var i = 0
        while (i < input.length) {
            if (input[i] == '&') {
                val semi = input.indexOf(';', i)
                if (semi > i) {
                    val entity = input.substring(i, semi + 1)
                    val ch = entityToChar[entity]
                    if (ch != null) {
                        sb.append(ch)
                        i = semi + 1
                        continue
                    }
                    if (entity.startsWith("&#x", ignoreCase = true)) {
                        val hex = entity.substring(3, entity.length - 1)
                        hex.toIntOrNull(16)?.let { code ->
                            sb.append(code.toChar())
                            i = semi + 1
                            continue
                        }
                    }
                    if (entity.startsWith("&#")) {
                        val num = entity.substring(2, entity.length - 1)
                        num.toIntOrNull()?.let { code ->
                            sb.append(code.toChar())
                            i = semi + 1
                            continue
                        }
                    }
                }
            }
            sb.append(input[i])
            i++
        }
        return sb.toString()
    }
}
