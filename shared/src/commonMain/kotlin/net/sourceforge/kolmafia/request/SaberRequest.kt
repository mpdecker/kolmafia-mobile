package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.SaberChoiceSync

/** Desktop [net.sourceforge.kolmafia.request.SaberRequest] choice 1386/1387 hub. */
object SaberRequest {
    fun parseUpgrade(url: String, html: String, preferences: Preferences?) {
        if (!url.contains("whichchoice=1386", ignoreCase = true)) return
        val decision = option(url) ?: return
        SaberChoiceSync.applyUpgrade(SaberChoiceSync.UPGRADE_CHOICE, decision, html, preferences)
    }

    fun parseForce(
        url: String,
        html: String,
        preferences: Preferences?,
        currentMonsterName: String,
        currentTurn: Int,
        banishManager: BanishManager?,
    ) {
        if (!url.contains("whichchoice=1387", ignoreCase = true)) return
        val decision = option(url) ?: return
        SaberChoiceSync.applyForce(
            SaberChoiceSync.FORCE_CHOICE,
            decision,
            preferences,
            currentMonsterName,
            currentTurn,
            banishManager,
        )
    }

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        currentMonsterName: String = "",
        currentTurn: Int = 0,
        banishManager: BanishManager? = null,
    ) {
        parseUpgrade(url, html, preferences)
        parseForce(url, html, preferences, currentMonsterName, currentTurn, banishManager)
    }

    fun registerRequest(url: String): Boolean =
        url.contains("whichchoice=1386", ignoreCase = true) ||
            url.contains("whichchoice=1387", ignoreCase = true)

    private fun option(url: String): Int? =
        Regex("""(?:option|decision)=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
}
