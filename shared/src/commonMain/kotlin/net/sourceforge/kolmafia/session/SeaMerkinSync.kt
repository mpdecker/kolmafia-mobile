package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.SeaMerkinRequest] temple/colosseum visit pref sync. */
object SeaMerkinSync {
    const val MERKIN_COLOSSEUM_SNARFBLEAT = "210"

    private val ACTION_PATTERN = Regex("""action=([^&]+)""", RegexOption.IGNORE_CASE)

    fun parseTemple(
        url: String,
        html: String,
        inSeaPath: Boolean,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        if (!url.contains("sea_merkin.php", ignoreCase = true)) return
        if (actionFromUrl(url) != "temple") return
        val prefs = preferences ?: return

        if (inSeaPath && html.contains("This part of the temple is now empty")) {
            if (url.contains("subaction=left", ignoreCase = true)) {
                prefs.setBoolean("shubJigguwattDefeated", true)
                sessionLogger?.appendRawLine("Mer-kin quest: Shub-Jigguwatt temple empty (Sea path)")
            }
            if (url.contains("subaction=right", ignoreCase = true)) {
                prefs.setBoolean("yogUrtDefeated", true)
                sessionLogger?.appendRawLine("Mer-kin quest: Yog-Urt temple empty (Sea path)")
            }
        } else if (html.contains("The temple is empty")) {
            prefs.setString("merkinQuestPath", "done")
            sessionLogger?.appendRawLine("Mer-kin quest complete (temple empty)")
        }
    }

    fun parseColosseum(
        url: String,
        html: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        if (!url.contains("snarfblat=$MERKIN_COLOSSEUM_SNARFBLEAT", ignoreCase = true)) return
        val prefs = preferences ?: return
        if (prefs.getString("merkinQuestPath", "") == "done") return

        when {
            html.contains("your crowd of Mer-kin admirers") -> {
                prefs.setBoolean("isMerkinGladiatorChampion", true)
                prefs.setString("merkinQuestPath", "gladiator")
                prefs.setInt("lastColosseumRoundWon", 15)
                sessionLogger?.appendRawLine("Mer-kin Gladiator Champion (colosseum visit)")
            }
            html.contains("Praise be to the High Priest") -> {
                prefs.setBoolean("isMerkinHighPriest", true)
                prefs.setString("merkinQuestPath", "scholar")
                sessionLogger?.appendRawLine("Mer-kin High Priest (colosseum visit)")
            }
        }
    }

    private fun actionFromUrl(url: String): String? =
        ACTION_PATTERN.find(url)?.groupValues?.getOrNull(1)?.lowercase()
}
