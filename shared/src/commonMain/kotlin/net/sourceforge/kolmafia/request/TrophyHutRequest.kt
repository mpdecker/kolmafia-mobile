package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.TrophyHutRequest]. */
object TrophyHutRequest {
    private val WHICH_TROPHY_PATTERN = Regex("""whichtrophy=(\d*)""", RegexOption.IGNORE_CASE)

    fun parseResponse(
        url: String,
        html: String,
        character: KoLCharacter?,
        sessionLogger: SessionLogger?,
    ) {
        if (!url.startsWith("trophy.php", ignoreCase = true)) return
        val action = actionFromUrl(url) ?: return
        if (!action.equals("buytrophy", ignoreCase = true)) return
        if (!html.contains("Your trophy has been installed at your campsite")) return
        sessionLogger?.appendRawLine("You spent 10,000 Meat")
        ResultProcessor.processMeat(-10_000, character)
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger?): Boolean {
        if (!url.startsWith("trophy.php", ignoreCase = true)) return false
        val action = actionFromUrl(url) ?: return true
        if (!action.equals("buytrophy", ignoreCase = true)) return false
        val trophyId = WHICH_TROPHY_PATTERN.find(url)?.groupValues?.getOrNull(1) ?: return true
        val message = "Buying trophy #$trophyId at the Trophy Hut"
        sessionLogger?.appendRawLine(message)
        return true
    }

    private fun actionFromUrl(url: String): String? =
        Regex("""action=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(url)
            ?.groupValues
            ?.getOrNull(1)
}
