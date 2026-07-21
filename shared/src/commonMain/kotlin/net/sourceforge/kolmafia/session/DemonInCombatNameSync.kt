package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Tracks demon-in-combat (demon 14) name segments from Allied Radio grey text.
 * Mirrors desktop [SummoningChamberRequest.updateDemonInCombatSegments].
 */
class DemonInCombatNameSync(private val preferences: Preferences) {

    data class SegmentUpdateResult(
        val updated: Boolean,
        val hintMessage: String? = null,
    )

    fun demonName(): String =
        preferences.getString(Preferences.DEMON_NAME_14, "")

    fun parseRadioResponse(html: String): SegmentUpdateResult {
        val match = GREY_TEXT_PATTERN.find(html) ?: return SegmentUpdateResult(updated = false)
        return updateSegment(match.groupValues[1])
    }

    fun updateSegment(segment: String): SegmentUpdateResult {
        if (preferences.getString(Preferences.DEMON_NAME_14, "").isNotBlank()) {
            return SegmentUpdateResult(updated = false)
        }

        val prefValue = preferences.getString(Preferences.DEMON_NAME_14_SEGMENTS, "")
        if (prefValue.isEmpty()) {
            preferences.setString(Preferences.DEMON_NAME_14_SEGMENTS, segment)
            return SegmentUpdateResult(updated = true)
        }

        val segments = parseSegmentsPref(prefValue).toMutableMap()
        segments.merge(segment, 1) { old, _ -> old + 1 }
        preferences.setString(Preferences.DEMON_NAME_14_SEGMENTS, formatSegmentsPref(segments))

        val hint = if (segments.size > 10) {
            "With ${segments.size} segments you can probably get close to solving your demon name, try running \"demons solve14\""
        } else {
            null
        }
        return SegmentUpdateResult(updated = true, hintMessage = hint)
    }

    fun knownSegmentKeys(): Set<String> =
        parseSegmentsPref(preferences.getString(Preferences.DEMON_NAME_14_SEGMENTS, "")).keys

    fun formatSegmentsPref(segments: Map<String, Int>): String =
        segments.entries.joinToString(",") { (key, count) ->
            if (count > 1) "$key:$count" else key
        }

    fun parseSegmentsPref(value: String): Map<String, Int> {
        if (value.isEmpty()) return emptyMap()
        return value.split(',')
            .filter { it.isNotBlank() }
            .associate { part ->
                val pieces = part.split(':', limit = 2)
                val key = pieces[0]
                val count = pieces.getOrNull(1)?.toIntOrNull() ?: 1
                key to count
            }
    }

    companion object {
        const val ALLIED_RADIO_BACKPACK_CHOICE = 1561
        const val ALLIED_RADIO_HANDHELD_CHOICE = 1563

        private val GREY_TEXT_PATTERN =
            Regex("""<i style='color: #999'>([^<]+)</i>""")

        fun isAlliedRadioChoice(choiceId: Int): Boolean =
            choiceId == ALLIED_RADIO_BACKPACK_CHOICE || choiceId == ALLIED_RADIO_HANDHELD_CHOICE
    }
}
