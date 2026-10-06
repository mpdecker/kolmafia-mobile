package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.AdventureSession
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [GenericRequest.checkOtherRedirection] — fight labels after main.php /
 * campground.php redirects into combat.
 */
object CheckOtherRedirection {
    data class Result(val handled: Boolean, val label: String? = null)

    fun apply(
        location: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger? = null,
    ): Result {
        val loc = location.removePrefix("https://www.kingdomofloathing.com/")
            .removePrefix("http://www.kingdomofloathing.com/")
            .removePrefix("/")
        val otherName = when {
            loc.startsWith("main.php", ignoreCase = true) &&
                loc.contains("fightgodlobster=1", ignoreCase = true) -> {
                preferences?.increment("_godLobsterFights", 1)
                "God Lobster"
            }
            loc.startsWith("campground.php", ignoreCase = true) &&
                loc.contains("action=garden", ignoreCase = true) -> "Bone Garden"
            else -> null
        } ?: return Result(handled = false)

        AdventureSession.clearLocation(preferences)
        AdventureSession.setLastAdventure("None", preferences)
        AdventureSession.setNextAdventure("None", preferences)
        CheckItemRedirection.setItemMonster(otherName)
        RequestLogger.updateSessionLog("[$otherName]", sessionLogger)
        return Result(handled = true, label = otherName)
    }
}
