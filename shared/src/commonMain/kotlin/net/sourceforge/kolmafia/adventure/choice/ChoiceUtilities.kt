package net.sourceforge.kolmafia.adventure.choice

object ChoiceUtilities {

    private val CHOICE_ID_REGEX =
        Regex("""<input[^>]+name="whichchoice"[^>]+value="(\d+)"""", RegexOption.IGNORE_CASE)

    // Match any <input> tag, capture it and everything after until the next <input or end.
    // Then extract name="option" and value= attributes regardless of order.
    private val OPTION_TAG_REGEX =
        Regex("""(<input[^>]+>)([\s\S]*?)(?=<input|$)""", RegexOption.IGNORE_CASE)
    private val OPTION_NAME_REGEX = Regex("""name="option"""", RegexOption.IGNORE_CASE)
    private val OPTION_VALUE_REGEX = Regex("""value="(\d+)"""", RegexOption.IGNORE_CASE)

    fun extractChoiceId(html: String): Int? =
        CHOICE_ID_REGEX.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()

    fun parseChoices(html: String): Map<Int, String> =
        OPTION_TAG_REGEX.findAll(html)
            .mapNotNull { m ->
                val tag = m.groupValues[1]
                if (!OPTION_NAME_REGEX.containsMatchIn(tag)) return@mapNotNull null
                val n = OPTION_VALUE_REGEX.find(tag)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: return@mapNotNull null
                val text = m.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                if (text.isEmpty()) null else n to text
            }
            .toMap()
}
