package net.sourceforge.kolmafia.mall

/** Desktop MallSearchRequest HTML preprocess — strip limited rows and normalize breaks. */
object MallSearchHtmlPreprocessor {

    private val LIMITED_BANNER_ROW = Regex(
        """<tr[^>]*>\s*<td[^>]*>\s*Search results are limited[^<]*</td>\s*</tr>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val GRAYBELOW_LIMITED_ROW = Regex(
        """<tr class="graybelow[^"]*limited[^"]*".*?</tr>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun preprocess(html: String): String {
        var buffer = html
        buffer = buffer.replace("<br />", "<br>", ignoreCase = true)
        buffer = buffer.replace("<BR />", "<br>", ignoreCase = true)
        buffer = LIMITED_BANNER_ROW.replace(buffer, "")
        buffer = GRAYBELOW_LIMITED_ROW.replace(buffer, "")
        return buffer
    }
}
