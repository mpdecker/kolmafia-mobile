package net.sourceforge.kolmafia.request

/** Desktop [net.sourceforge.kolmafia.request.TrophyRequest] trophies.php catalog. */
object TrophyRequest {
    data class Trophy(
        val filename: String,
        val name: String,
        val id: Int,
        val visible: Boolean,
    )

    private val TROPHY = Regex(
        """<td><img.*?src=[^>]*?/(?:cloudfront\.net|images\.kingdomofloathing\.com|/images)/(.+?)".*?<td[^>]*?>(.+?)<.*?name=public(\d+)\s*(checked)?\s*>""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parseTrophies(html: String): List<Trophy> =
        TROPHY.findAll(html).map { m ->
            Trophy(
                filename = m.groupValues[1],
                name = m.groupValues[2],
                id = m.groupValues[3].toIntOrNull() ?: 0,
                visible = m.groupValues[4].isNotBlank(),
            )
        }.toList()

    fun parseResponse(url: String, html: String): List<Trophy> {
        if (!url.contains("trophies.php", ignoreCase = true)) return emptyList()
        return parseTrophies(html)
    }

    fun registerRequest(url: String): Boolean = url.contains("trophies.php", ignoreCase = true)
}
