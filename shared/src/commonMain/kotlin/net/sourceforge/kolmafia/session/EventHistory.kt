package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.mall.currentEpochMillis

/**
 * Desktop [EventManager] red-bar parse/dump — no balloon, tray, or chat broadcast.
 */
object EventHistory {
    data class Event(
        val text: String,
        val html: String,
        val timestampMillis: Long,
    )

    private val events = mutableListOf<Event>()

    private val eventPatterns = listOf(
        Regex(
            """<table[^>]*><tr><td[^>]*><b(?:| [^>]+)>New Events:</b></td></tr>\s*""" +
                """<tr><td style="padding: 5px; border: 1px solid orange;"><center><table><tr><td>""" +
                """(.*?)</td></tr></table></center></td></tr><tr><td height=4></td></tr></table>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ),
        Regex(
            """<table[^>]*><tr><td[^>]*><b(?:| [^>]+)>New Events:</b></td></tr>\s*""" +
                """<tr><td style="padding: 5px; border: 1px solid orange;" align=center>""" +
                """(.*?)</td></tr><tr><td height=4></td></tr></table>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ),
    )

    private val brSplit = Regex("""<br(?: /)?>|\n""", RegexOption.IGNORE_CASE)
    private val htmlTag = Regex("""<[^>]*>""")

    fun texts(filter: String? = null): List<String> =
        events.asSequence()
            .filter { filter.isNullOrBlank() || it.text.contains(filter, ignoreCase = true) }
            .map { it.text }
            .toList()

    fun entries(filter: String? = null): List<Event> =
        events.asSequence()
            .filter { filter.isNullOrBlank() || it.text.contains(filter, ignoreCase = true) }
            .toList()

    fun clear() {
        events.clear()
    }

    fun resetForTest() {
        clear()
    }

    fun checkForNewEvents(html: String) {
        if (html.isEmpty()) return
        for (block in parseEvents(html)) {
            for (paragraph in block.split("<p>")) {
                for (line in paragraph.split(brSplit)) {
                    addNormalEvent(line)
                }
            }
        }
    }

    private fun parseEvents(html: String): List<String> =
        eventPatterns.flatMap { pattern ->
            pattern.findAll(html).mapNotNull { it.groupValues.getOrNull(1) }
        }

    private fun addNormalEvent(eventHtml: String) {
        if (eventHtml.contains("logged", ignoreCase = true) ||
            eventHtml.contains("has left the building", ignoreCase = true)
        ) {
            return
        }
        val text = eventHtml.replace(htmlTag, "").trim()
        if (text.isNotEmpty()) {
            events.add(Event(text, eventHtml.trim(), currentEpochMillis()))
        }
    }

}
