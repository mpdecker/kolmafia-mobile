package net.sourceforge.kolmafia.request

/** Desktop [net.sourceforge.kolmafia.request.concoction.MultiUseRequest] multiuse.php. */
object MultiUseRequest {
    fun registerRequest(url: String): Boolean =
        url.contains("multiuse.php", ignoreCase = true)

    fun parseResponse(url: String, html: String) {
        if (!registerRequest(url)) return
        val itemId = Regex("""whichitem=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        UseItemConsumptionSync.parseConsumption(html, itemId)
    }
}
