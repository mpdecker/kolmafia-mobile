package net.sourceforge.kolmafia.adventure.choice

object ChoiceUtilities {

    private val CHOICE_ID_REGEX =
        Regex("""<input[^>]+name="whichchoice"[^>]+value="(\d+)"""")

    private val OPTION_REGEX =
        Regex(
            """<input[^>]+name="option"[^>]+value="(\d+)"[^>]*>\s*(.*?)(?=\s*<input|\s*</form|\s*$)""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )

    fun extractChoiceId(html: String): Int? =
        CHOICE_ID_REGEX.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()

    fun parseChoices(html: String): Map<Int, String> =
        OPTION_REGEX.findAll(html)
            .mapNotNull { m ->
                val n = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                val text = m.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                if (text.isEmpty()) null else n to text
            }
            .toMap()
}
