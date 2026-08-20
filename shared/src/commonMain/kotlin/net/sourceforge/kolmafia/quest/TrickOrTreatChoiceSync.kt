package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Trick-or-Treating choice 804 —
 * visit rebuild of `_trickOrTreatBlock` + preChoice house mark from `whichhouse=`.
 */
object TrickOrTreatChoiceSync {

    const val CHOICE_ID = 804

    const val BLOCK_PREF = "_trickOrTreatBlock"

    private val HOUSE_URL_PATTERN = Regex("""whichhouse=(\d+)""", RegexOption.IGNORE_CASE)

    // Desktop uses `<img (class='faded')? src=...` which only matches faded tags
    // (requires a space before src when the class group is absent). Match both
    // `<img src=` and `<img class='faded' src=` forms used in live HTML.
    private val HOUSE_GIF_PATTERN = Regex(
        """<img(?<faded> class='faded')? src='.*?/trickortreat/(?:house_)?(?<type>starhouse|[ld])(?:\d+)?\.gif'""",
        RegexOption.IGNORE_CASE,
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val state = HOUSE_GIF_PATTERN.findAll(html).mapNotNull { match ->
            val faded = !match.groups["faded"]?.value.isNullOrEmpty()
            val type = match.groups["type"]?.value?.firstOrNull()?.toString() ?: return@mapNotNull null
            if (faded) type.lowercase() else type.uppercase()
        }.joinToString("")
        if (state.isEmpty()) return false
        preferences.setString(BLOCK_PREF, state)
        return true
    }

    fun apply(
        choiceId: Int,
        preferences: Preferences?,
        choiceUrl: String = "",
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val house = HOUSE_URL_PATTERN.find(choiceUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        val block = preferences.getString(BLOCK_PREF, "")
        if (house < 0 || house >= block.length) return false
        val chars = block.toCharArray()
        chars[house] = chars[house].lowercaseChar()
        preferences.setString(BLOCK_PREF, String(chars))
        return true
    }
}
