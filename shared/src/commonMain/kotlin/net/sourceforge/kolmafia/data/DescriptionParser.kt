package net.sourceforge.kolmafia.data

/**
 * Description HTML field parsers mirrored from desktop [DebugDatabase].
 */
object DescriptionParser {
    private val NAME_PATTERN = Regex("""<b>(.*?)</b>""", RegexOption.DOT_MATCHES_ALL)
    private val SIZE_PATTERN = Regex("""(?:Size|Potency|Toxicity): <b>(.*?)</b>""")
    private val QUALITY_PATTERN = Regex("""Type: <b>.*?\((.*?)\).*?</b>""", RegexOption.DOT_MATCHES_ALL)

    fun parseName(text: String): String =
        NAME_PATTERN.find(text)?.groupValues?.get(1)?.trim().orEmpty()

    fun parseConsumableSize(text: String): Int =
        SIZE_PATTERN.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    fun parseQuality(text: String): String =
        ConsumableQuality.fromString(
            QUALITY_PATTERN.find(text)?.groupValues?.get(1).orEmpty(),
        ).displayName()
}
