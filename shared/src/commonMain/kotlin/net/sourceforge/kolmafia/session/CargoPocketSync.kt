package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Ascension picked-pocket tracking for Cargo Cultist Shorts.
 * Mirrors desktop [CargoCultistShortsRequest] load/save/inspect/pick parsing.
 */
class CargoPocketSync(
    private val preferences: Preferences,
    private val yegDemonNameSync: YegDemonNameSync,
) {

    private val pickedPockets = sortedSetOf<Int>()

    init {
        loadPockets()
    }

    fun pickedPocketIds(): Set<Int> = pickedPockets.toSet()

    fun loadPockets() {
        pickedPockets.clear()
        val value = preferences.getString(Preferences.CARGO_POCKETS_EMPTIED, "")
        if (value.isEmpty()) return
        for (part in value.split(',')) {
            val pocket = part.trim().toIntOrNull() ?: continue
            if (pocket in 1..666) {
                pickedPockets.add(pocket)
            }
        }
    }

    fun savePockets() {
        preferences.setString(
            Preferences.CARGO_POCKETS_EMPTIED,
            pickedPockets.joinToString(","),
        )
    }

    fun parseAvailablePockets(responseText: String) {
        if (!responseText.contains("There appear to be 666 pockets on these shorts.")) {
            return
        }

        pickedPockets.clear()

        var expected = 1
        var pocket = 0
        for (match in AVAILABLE_POCKET_PATTERN.findAll(responseText)) {
            pocket = match.groupValues[1].toIntOrNull() ?: continue
            while (expected < pocket) {
                pickedPockets.add(expected++)
            }
            expected++
        }

        while (pocket < 666) {
            pickedPockets.add(++pocket)
        }

        savePockets()
    }

    fun parsePocketPick(pocket: Int, responseText: String) {
        if (pocket !in 1..666) return
        if (responseText.contains("leave your pockets unplundered")) return

        if (responseText.contains("the power of the pockets has been exhausted for the day")) {
            preferences.setBoolean(Preferences.CARGO_POCKET_EMPTIED, true)
            return
        }

        if (!pickedPockets.contains(pocket)) {
            pickedPockets.add(pocket)
            savePockets()
        }

        if (responseText.contains("That pocket is empty.")) {
            return
        }

        preferences.setBoolean(Preferences.CARGO_POCKET_EMPTIED, true)
        yegDemonNameSync.checkScrapPocket(pocket, responseText)
        checkMeatNotePocket(responseText)
    }

    fun registerPocketFight(url: String) {
        registerPocketFightFromPocket(extractPocketFromUrl(url))
    }

    fun registerPocketFightFromPocket(pocket: Int) {
        if (pocket !in 1..666) return
        preferences.setBoolean(Preferences.CARGO_POCKET_EMPTIED, true)
        if (pickedPockets.add(pocket)) {
            savePockets()
        }
    }

    internal fun extractMeatNote(responseText: String): String? {
        val match = MEAT_NOTE_PATTERN.find(responseText) ?: return null
        return match.groupValues[1].trim()
    }

    private fun checkMeatNotePocket(responseText: String) {
        extractMeatNote(responseText) ?: return
    }

    fun parsePocketPickFromUrl(url: String, responseText: String) {
        val pocket = extractPocketFromUrl(url)
        if (pocket == 0) return
        parsePocketPick(pocket, responseText)
    }

    companion object {
        const val CARGO_CULT_CHOICE = 1420

        private val AVAILABLE_POCKET_PATTERN = Regex(
            """<form method="post" action="choice.php" style="display: inline">.*?name="pocket" value="(\d+)".*?</form>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val URL_POCKET_PATTERN = Regex("""pocket=(\d+)""")
        private val MEAT_NOTE_PATTERN = Regex(
            """You pull a note out of your pocket\.  It's wrapped around a pile of meat\..*?<blockquote[^>]*>([^<]*)<""",
            RegexOption.DOT_MATCHES_ALL,
        )

        fun extractPocketFromUrl(url: String): Int =
            URL_POCKET_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }
}
