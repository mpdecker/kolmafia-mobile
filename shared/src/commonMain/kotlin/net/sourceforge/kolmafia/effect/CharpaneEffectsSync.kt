package net.sourceforge.kolmafia.effect

import net.sourceforge.kolmafia.character.CharpaneStatusSync
import net.sourceforge.kolmafia.data.EffectDatabase

/** Parses active effects from charpane.php HTML (Phase 408). */
object CharpaneEffectsSync {

    fun parse(html: String): List<EffectData> {
        val compact = CharpaneStatusSync.isCompact(html)
        val effects = mutableListOf<EffectData>()
        var searchFrom = 0
        while (true) {
            val onClickIndex = html.indexOf("onClick='eff", searchFrom)
            if (onClickIndex < 0) break
            val tagStart = html.lastIndexOf("<", onClickIndex)
            if (tagStart < 0) {
                searchFrom = onClickIndex + 1
                continue
            }
            val parsed = extractEffect(html, tagStart, onClickIndex, compact)
            if (parsed != null) {
                effects += parsed
            }
            searchFrom = onClickIndex + 1
        }
        return effects.distinctBy { it.id }.sortedBy { it.name }
    }

    private fun extractEffect(
        html: String,
        searchIndex: Int,
        onClickIndex: Int,
        compact: Boolean,
    ): EffectData? {
        val durationIndex: Int
        val effectName: String
        if (compact) {
            val altStart = html.indexOf("alt=\"", searchIndex)
            if (altStart < 0) return null
            val nameStart = altStart + 5
            val nameEnd = html.indexOf("\"", nameStart)
            if (nameEnd < 0) return null
            effectName = html.substring(nameStart, nameEnd)
            val tdIndex = html.indexOf("<td>(", nameStart)
            if (tdIndex < 0) return null
            durationIndex = tdIndex + 5
        } else {
            var startIndex = html.indexOf("<font size=2", searchIndex)
            if (startIndex < 0) return null
            startIndex = html.indexOf(">", startIndex) + 1
            val endFont = html.indexOf("</font", startIndex)
            if (endFont < 0) return null
            val openParen = html.lastIndexOf("(", endFont)
            if (openParen < 0) return null
            effectName = html.substring(startIndex, openParen).trim()
            durationIndex = openParen + 1
        }

        val descStart = html.indexOf("(", onClickIndex) + 1
        val descEnd = html.indexOf(")", onClickIndex)
        if (descStart <= 0 || descEnd <= descStart) return null
        val descId = html.substring(descStart + 1, descEnd - 1).trim('"', '\'', ' ')

        val durationEnd = html.indexOf(")", durationIndex)
        if (durationEnd < 0) return null
        val durationString = html.substring(durationIndex, durationEnd)
        val duration = when {
            durationString.equals("&infin;", ignoreCase = true) ||
                durationString.equals("Today", ignoreCase = true) -> Int.MAX_VALUE
            durationString.contains('&') || durationString.contains('<') -> return null
            else -> durationString.filter { it.isDigit() || it == '-' }.toIntOrNull() ?: return null
        }

        val id = EffectDatabase.getByDescId(descId)?.id
            ?: EffectDatabase.getByName(effectName)?.id
            ?: return null
        return EffectData(id = id, name = effectName, duration = duration)
    }
}
