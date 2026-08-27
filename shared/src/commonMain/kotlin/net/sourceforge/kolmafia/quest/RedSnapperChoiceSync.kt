package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.track.TrackManager

/**
 * Desktop [ChoiceControl] Adjusting Your Fish choice 1396.
 */
object RedSnapperChoiceSync {

    const val CHOICE_ID = 1396

    private val RED_SNAPPER_PATTERN =
        Regex("""guiding you towards: <b>(.*?)</b>\.  You've found <b>(\d+)</b> of them""")

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        currentTurn: Int = 0,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val match = RED_SNAPPER_PATTERN.find(html) ?: return false
        val phylum = match.groupValues[1].trim()
        val progress = match.groupValues[2].toIntOrNull() ?: return false
        TrackManager.track(preferences, phylum, TrackManager.Tracker.RED_SNAPPER, currentTurn)
        preferences.setString("redSnapperPhylum", phylum)
        preferences.setInt("redSnapperProgress", progress)
        return true
    }

    fun apply(
        choiceId: Int,
        preferences: Preferences?,
        choiceUrl: String = "",
        currentTurn: Int = 0,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (!choiceUrl.contains("option=1") || !choiceUrl.contains("cat=")) return false
        val raw = Regex("""cat=([^&]*)""").find(choiceUrl)?.groupValues?.getOrNull(1)
            ?: return false
        val phylum = if (raw == "merkin") "mer-kin" else raw
        TrackManager.track(preferences, phylum, TrackManager.Tracker.RED_SNAPPER, currentTurn)
        preferences.setString("redSnapperPhylum", phylum)
        preferences.setInt("redSnapperProgress", 0)
        return true
    }
}
