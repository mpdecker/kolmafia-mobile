package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.FloristRequest

/** Desktop FloristRequest parsing for choice 720. */
object FloristFriarChoiceSync {
    const val CHOICE_ID = 720

    fun reset() = FloristRequest.reset()
    fun plantsAt(location: String): List<Int> = FloristRequest.getPlants(location).map { it.id }

    fun apply(choiceId: Int, choiceUrl: String, html: String, preferences: Preferences?): Boolean {
        if (choiceId != CHOICE_ID) return false
        val url = if (choiceUrl.contains("whichchoice=720", ignoreCase = true)) {
            choiceUrl
        } else {
            val joiner = if (choiceUrl.contains('?')) "&" else "?"
            "${choiceUrl.ifBlank { "choice.php" }}${joiner}whichchoice=720"
        }
        return FloristRequest.parseResponse(url, html, preferences)
    }
}
