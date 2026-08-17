package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ElVibratoSync

/**
 * Desktop [PortalRequest.parseResponse] — charge El Vibrato portal with spheres.
 */
object PortalRequest {

    const val POWER_SPHERE = 3049
    const val OVERCHARGED_POWER_SPHERE = 3215

    private val actionPattern = Regex("""action=(\w*)elvibratoportal""", RegexOption.IGNORE_CASE)

    fun parseResponse(
        url: String?,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (preferences == null) return false
        val urlString = url.orEmpty()
        if (!urlString.contains("campground.php", ignoreCase = true)) return false
        val itemId = sphereItemId(urlString) ?: return false
        val charges = when (itemId) {
            POWER_SPHERE -> 5
            OVERCHARGED_POWER_SPHERE -> 10
            else -> return false
        }
        if (html.contains("The pieces of the device rise")) {
            preferences.setInt("currentPortalEnergy", charges)
        } else if (html.contains("crackle of energy")) {
            preferences.setInt(
                "currentPortalEnergy",
                preferences.getInt("currentPortalEnergy", 0) + charges,
            )
        } else {
            return false
        }
        ElVibratoSync.updatePortalTrapezoid(preferences)
        consumeItem(itemId, 1)
        return true
    }

    fun sphereItemId(url: String): Int? {
        val prefix = actionPattern.find(url)?.groupValues?.getOrNull(1) ?: return null
        return when (prefix) {
            "power" -> POWER_SPHERE
            "overpower" -> OVERCHARGED_POWER_SPHERE
            else -> null
        }
    }
}
