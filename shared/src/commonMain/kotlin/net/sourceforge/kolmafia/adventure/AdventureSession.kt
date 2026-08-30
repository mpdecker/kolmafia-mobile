package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [KoLAdventure] last/next adventure + recordToSession glue
 * (Phases 2721–2735).
 */
object AdventureSession {
    @Volatile
    var lastVisitedLocationName: String? = null
        private set

    @Volatile
    var lastLocationName: String? = null
        private set

    @Volatile
    var lastLocationURL: String? = null
        private set

    @Volatile
    var nextAdventureName: String? = null
        private set

    @Volatile
    var locationLogged: Boolean = false

    fun clearLocation(preferences: Preferences? = null) {
        lastVisitedLocationName = null
        lastLocationName = null
        lastLocationURL = null
        nextAdventureName = null
        locationLogged = false
        preferences?.setString("lastAdventure", "None")
        preferences?.setString("nextAdventure", "None")
    }

    fun setLastAdventure(
        adventureName: String,
        preferences: Preferences? = null,
        url: String? = null,
    ) {
        if (adventureName.equals("None", ignoreCase = true) || adventureName.isBlank()) {
            clearLocation(preferences)
            return
        }
        val zone = AdventureDatabase.getByName(adventureName)
            ?: AdventureDatabase.getAdventureByURL(url.orEmpty())
        val resolved = zone?.locationName ?: adventureName
        lastVisitedLocationName = resolved
        lastLocationName = getPrettyAdventureName(resolved, url)
        lastLocationURL = url ?: zone?.let {
            AdventureFormBuilder.build(it.formSource, it.adventureId).requestUrl
        }
        preferences?.setString("lastAdventure", lastVisitedLocationName.orEmpty())
    }

    fun setNextAdventure(adventureName: String, preferences: Preferences? = null) {
        nextAdventureName = adventureName
        preferences?.setString("nextAdventure", adventureName)
    }

    fun getPrettyAdventureName(name: String, url: String? = null): String {
        if (name.contains("Daily Dungeon", ignoreCase = true)) {
            val chamber = url?.let { Regex("""whichroom=(\d+)""").find(it)?.groupValues?.get(1) }
            if (chamber != null) return "$name (Chamber $chamber)"
        }
        return name
    }

    fun lastAdventureId(): String {
        val name = lastVisitedLocationName ?: return ""
        return AdventureDatabase.getByName(name)?.adventureId.orEmpty()
    }

    /**
     * Pre-response half: stash tentative location; mark not yet logged.
     */
    fun recordToSession(
        urlString: String,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        val adventure = AdventureDatabase.getAdventureByURL(urlString)
        if (adventure != null) {
            if (adventure.adventureId == "shadow_rift" && urlString.contains("place.php")) {
                val place = Regex("""whichplace=([^&]+)""").find(urlString)?.groupValues?.get(1)
                if (!place.isNullOrBlank()) {
                    val message = "Entering the Shadow Rift via $place"
                    sessionLogger?.appendRawLine(message)
                    preferences?.setString("shadowRiftIngress", place)
                }
            }
            lastVisitedLocationName = adventure.locationName
            lastLocationName = getPrettyAdventureName(adventure.locationName, urlString)
            lastLocationURL = urlString
            locationLogged = false
            return true
        }
        if (!urlString.contains("?")) return false
        lastVisitedLocationName = null
        lastLocationName = urlString.substringBefore("?")
        lastLocationURL = urlString
        locationLogged = false
        return true
    }

    /**
     * Post-response half: skip session adventure line on failure; otherwise log location.
     * @return false if failure short-circuited logging
     */
    fun recordToSession(
        urlString: String,
        responseText: String,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        adventureCount: Int = 0,
    ): Boolean {
        if (locationLogged) return true
        val location = lastLocationName ?: return false
        locationLogged = true
        if (lastLocationURL?.contains("basement.php") == true) return true

        val failure = AdventureFailures.findAdventureFailure(responseText, preferences)
        if (failure >= 0) return false

        when {
            urlString.contains("cove.php") || urlString.contains("mining.php") -> return false
            urlString.contains("fight.php") || urlString.contains("choice.php") -> Unit
            else -> {
                AdventureDatabase.getAdventureByURL(urlString)?.let {
                    setLastAdventure(it.locationName, preferences, urlString)
                }
            }
        }

        setLastAdventure(lastVisitedLocationName ?: location, preferences, urlString)
        setNextAdventure(lastVisitedLocationName ?: location, preferences)
        val pretty = lastLocationName ?: location
        val line = if (adventureCount > 0) "[$adventureCount] $pretty" else pretty
        sessionLogger?.appendRawLine(line)
        return true
    }

    fun resetForTest() {
        lastVisitedLocationName = null
        lastLocationName = null
        lastLocationURL = null
        nextAdventureName = null
        locationLogged = false
    }
}
