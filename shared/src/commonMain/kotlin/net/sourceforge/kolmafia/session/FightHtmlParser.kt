package net.sourceforge.kolmafia.session

/**
 * Small ordered parser for fight fragments. It is intentionally not a DOM:
 * mobile needs paragraph/table/comment order and text, not Swing decorators or
 * relay nodes. Unclosed tags are accepted and produce useful partial events.
 */
object FightHtmlParser {
    sealed class Event(open val raw: String, open val position: Int) {
        data class Paragraph(
            override val raw: String,
            override val position: Int,
            val text: String,
        ) : Event(raw, position)

        data class Table(
            override val raw: String,
            override val position: Int,
            val text: String,
        ) : Event(raw, position)

        data class Comment(
            override val raw: String,
            override val position: Int,
            val text: String,
        ) : Event(raw, position)

        data class HorizontalRule(
            override val raw: String,
            override val position: Int,
        ) : Event(raw, position)
    }

    data class Document(val events: List<Event>) {
        val paragraphs: List<Event.Paragraph> get() = events.filterIsInstance<Event.Paragraph>()
        val tables: List<Event.Table> get() = events.filterIsInstance<Event.Table>()
        val comments: List<Event.Comment> get() = events.filterIsInstance<Event.Comment>()
    }

    private val EVENT = Regex(
        """<!--.*?-->|<p\b[^>]*>.*?</p\s*>|<table\b[^>]*>.*?</table\s*>|<hr\b[^>]*>|<p\b[^>]*>.*$|<table\b[^>]*>.*$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val TAG = Regex("""<[^>]*>""")

    fun parse(html: String): Document {
        if (html.isBlank()) return Document(emptyList())
        val events = EVENT.findAll(html).mapNotNull { match ->
            val raw = match.value
            when {
                raw.startsWith("<!--") -> Event.Comment(raw, match.range.first, raw
                    .removePrefix("<!--").removeSuffix("-->").trim())
                raw.startsWith("<hr", ignoreCase = true) ->
                    Event.HorizontalRule(raw, match.range.first)
                raw.startsWith("<table", ignoreCase = true) ->
                    Event.Table(raw, match.range.first, text(raw))
                raw.startsWith("<p", ignoreCase = true) ->
                    Event.Paragraph(raw, match.range.first, text(raw))
                else -> null
            }
        }.toList()
        return Document(events)
    }

    fun text(fragment: String): String = fragment
        .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        .replace(TAG, " ")
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace("&amp;", "&", ignoreCase = true)
        .replace("&quot;", "\"", ignoreCase = true)
        .replace("&#39;", "'", ignoreCase = true)
        .replace(Regex("""\s+"""), " ")
        .trim()
}
