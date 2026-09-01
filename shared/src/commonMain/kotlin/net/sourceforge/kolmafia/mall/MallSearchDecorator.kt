package net.sourceforge.kolmafia.mall

/**
 * Headless mall-search HTML decoration — desktop [MallSearchRequest.decorateMallSearch] parity.
 * Used when relay mode is active; does not require a relay server process.
 */
object MallSearchDecorator {
    private val STOREDETAIL_PATTERN = Regex(
        """<tr class="graybelow[^"]*".*?</tr>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val NOBUYERS_PATTERN = Regex(
        """<td valign="center" class="buyers">&nbsp;</td>""",
        RegexOption.IGNORE_CASE,
    )
    private val LISTDETAIL_PATTERN = Regex(
        """whichstore=(\d+)&(?:amp;)?searchitem=([\d.]+)&(?:amp;)?searchprice=(\d+)"><b>(.*?)</b>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun decorateMallSearch(html: String, passwordHash: String = ""): String {
        var buffer = html
        buffer = decorateAddBuyButtons(buffer, passwordHash)
        buffer = decorateHighlightStores(buffer)
        return buffer
    }

    private fun decorateAddBuyButtons(html: String, passwordHash: String): String {
        val buffer = StringBuilder(html)
        for (match in STOREDETAIL_PATTERN.findAll(html)) {
            val store = match.value
            val nobuyers = NOBUYERS_PATTERN.find(store) ?: continue
            val details = LISTDETAIL_PATTERN.find(store) ?: continue
            val whichstore = details.groupValues[1]
            val searchitem = details.groupValues[2]
            val searchprice = details.groupValues[3].toLongOrNull() ?: continue
            val itemId = searchitem.substringBefore('.').toIntOrNull() ?: continue
            val storeString = MallPurchaseRequest.getStoreString(itemId, searchprice)
            val buyers = buildString {
                append("""<td valign="center" class="buyers">""")
                append("""[<a href="mallstore.php?buying=1&quantity=1&whichitem=""")
                append(storeString)
                append("""&ajax=1&pwd=""")
                append(passwordHash)
                append("""&whichstore=""")
                append(whichstore)
                append(""" class="buyone">buy</a>]&nbsp;""")
                append("""[<a href="#" rel ="mallstore.php?buying=1&whichitem=""")
                append(storeString)
                append("""&ajax=1&pwd=""")
                append(passwordHash)
                append("""&whichstore=""")
                append(whichstore)
                append("""&quantity=" class="buysome">buy&nbsp;some</a>]""")
                append("</td>")
            }
            val start = match.range.first + nobuyers.range.first
            val end = match.range.first + nobuyers.range.last + 1
            buffer.replace(start, end, buyers)
        }
        return buffer.toString()
    }

    private fun decorateHighlightStores(html: String): String {
        val forbidden = MallPurchaseRequest.getForbiddenStores()
        if (forbidden.isEmpty()) return html
        val buffer = StringBuilder(html)
        for (match in STOREDETAIL_PATTERN.findAll(html)) {
            val store = match.value
            val details = LISTDETAIL_PATTERN.find(store) ?: continue
            val storeId = details.groupValues[1].toIntOrNull() ?: continue
            if (storeId !in forbidden) continue
            val replacement = store.replace(
                """<tr class="graybelow">""",
                """<tr class="graybelow forbidden">""",
                ignoreCase = true,
            )
            if (replacement != store) {
                buffer.replace(match.range.first, match.range.last + 1, replacement)
            }
        }
        return buffer.toString()
    }
}
