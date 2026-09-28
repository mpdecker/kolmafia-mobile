package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop `UseItemRequest.parseEVHelmet`: a punchcard that makes the El Vibrato helmet speak
 * “(for a long time)” stores the nine conduit levels in `lastEVHelmetValue` and the ascension
 * in `lastEVHelmetReset`.
 */
object UseItemEvHelmetSync {
    val LEVELS = listOf(
        "PA",
        "ZERO",
        "NOKGAGA",
        "NEGLIGIBLE",
        "GABUHO NO",
        "EXTREMELY LOW",
        "GA NO",
        "VERY LOW",
        "NO",
        "LOW",
        "FUZEVENI",
        "MODERATE",
        "PAPACHA",
        "ELEVATED",
        "FU",
        "HIGH",
        "GA FU",
        "VERY HIGH",
        "GABUHO FU",
        "EXTREMELY HIGH",
        "CHOSOM",
        "MAXIMAL",
    )

    private val PUNCHCARDS = 3146..3156
    private val HELPER = Regex("""dowhichitem=(\d+)""", RegexOption.IGNORE_CASE)
    private val TAGS = Regex("(<.*?>)+")

    fun parse(
        url: String,
        responseText: String,
        preferences: Preferences?,
        ascensions: Int = 0,
    ): Boolean {
        val helper = HELPER.find(url)?.groupValues?.get(1)?.toIntOrNull() ?: return false
        if (helper !in PUNCHCARDS) return false
        if (responseText.contains("A tinny voice emerges from the drone")) return false
        if (!responseText.contains("(for a long time)")) return false
        val pieces = responseText.split(TAGS)
        var start = pieces.indexOf("KROKRO LAZAK FULA:")
        if (start == -1) start = pieces.indexOf("SPEAR POWER CONDUIT:")
        if (start == -1 || pieces.size - start < 20) return false
        var data = 0
        for (i in 0 until 9) {
            val piece = pieces.getOrNull(start + i * 2 + 1) ?: return false
            val value = LEVELS.indexOf(piece)
            if (value < 0) return false
            data = data * 11 + value / 2
        }
        preferences?.setInt("lastEVHelmetValue", data)
        preferences?.setInt("lastEVHelmetReset", ascensions)
        return preferences != null
    }
}
