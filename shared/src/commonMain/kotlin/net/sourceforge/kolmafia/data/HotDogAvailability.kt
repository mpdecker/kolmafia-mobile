package net.sourceforge.kolmafia.data

/** Desktop ClanLoungeRequest.parseHotDogStand — enabled hot dog name set from lounge HTML. */
object HotDogAvailability {

    private val availableNames = mutableSetOf<String>()

    private val HOTDOG_STAND_PATTERN = Regex(
        """<table>(<tr><form action=clan_viplounge.php method=post>.*?)</table>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val HOTDOG_PATTERN = Regex(
        """<input class=button type=submit value=Eat( disabled[^>]*)?>.*?<b>([^<]+)</b>""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun reset() {
        availableNames.clear()
    }

    fun addFromHtml(html: String) {
        val stand = HOTDOG_STAND_PATTERN.find(html)?.groupValues?.getOrNull(1) ?: html
        for (match in HOTDOG_PATTERN.findAll(stand)) {
            if (!match.groupValues.getOrNull(1).isNullOrEmpty()) continue
            val name = match.groupValues.getOrNull(2)?.trim().orEmpty()
            if (name.isNotEmpty() && HotDogDatabase.isHotDog(name)) {
                availableNames.add(name)
            }
        }
    }
    fun isAvailable(name: String): Boolean =
        availableNames.any { it.equals(name, ignoreCase = true) }

    fun isEmpty(): Boolean = availableNames.isEmpty()

    fun snapshotNames(): Set<String> = availableNames.toSet()

    fun restoreName(name: String) {
        if (HotDogDatabase.isHotDog(name)) {
            availableNames.add(name)
        }
    }

    internal fun resetForTest() {
        reset()
    }

    internal fun addForTest(name: String) {
        restoreName(name)
    }

    internal fun namesForTest(): Set<String> = snapshotNames()
}
