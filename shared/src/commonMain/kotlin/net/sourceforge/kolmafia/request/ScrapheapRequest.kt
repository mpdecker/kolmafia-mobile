package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [ScrapheapRequest] deepen — chronolith energy cost, scavenge, charge
 * (Phases 3171–3185 / 3186–3200).
 */
object ScrapheapRequest {
    private val CHRONOLITH_COST = Regex("""title="\((\d+) Energy\)"""")
    private val ENERGY_GAIN = Regex("""You gain (\d+) Energy\.""")

    fun parseResponse(
        urlString: String,
        responseText: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
    ) {
        preferences?.setBoolean("scrapheapAvailable", true)
        val action = PlaceSync.action(urlString)

        when {
            action.isBlank() || action.startsWith("sh_chrono", ignoreCase = true) ->
                parseChronolith(responseText, preferences, character)
            action.startsWith("sh_getpower", ignoreCase = true) ->
                parseCollectEnergy(responseText, preferences)
            action.startsWith("sh_scrounge", ignoreCase = true) ->
                preferences?.setBoolean("youRobotScavenged", true)
            else -> Unit
        }

        // Continuity energy scrape on plain scrapheap visits only (avoid chronolith title false-match)
        if (action.isBlank()) {
            ScrapheapSync.parseEnergyFields(responseText, preferences, character)
        }
        ResultProcessor.processResults(false, responseText, null, character, preferences)
    }

    fun registerRequest(urlString: String, sessionLogger: SessionLogger?): Boolean {
        if (!urlString.contains("whichplace=scrapheap", ignoreCase = true)) return false
        val action = PlaceSync.action(urlString)
        if (action.isBlank()) return true
        val message = when {
            action.startsWith("sh_chrono", ignoreCase = true) -> "Activating the Chronolith"
            action.startsWith("sh_upgrade", ignoreCase = true) -> return true
            action.startsWith("sh_getpower", ignoreCase = true) -> "Collecting energy"
            action.startsWith("sh_scrounge", ignoreCase = true) -> "Scavenging scrap"
            action.startsWith("sh_configure", ignoreCase = true) -> return true
            else -> return false
        }
        RequestLogger.updateSessionLog(message, sessionLogger)
        return true
    }

    private fun parseChronolith(
        responseText: String,
        preferences: Preferences?,
        character: KoLCharacter?,
    ) {
        preferences ?: return
        val lastCost = preferences.getInt("_chronolithNextCost", 0)
        if (responseText.contains("You gain 10 Adventures")) {
            val energy = (character?.state?.value?.youRobotEnergy
                ?: preferences.getInt("youRobotEnergy", 0)) - lastCost
            character?.setYouRobotEnergy(energy.coerceAtLeast(0))
            preferences.setInt("youRobotEnergy", energy.coerceAtLeast(0))
            preferences.setInt("scrapheapEnergy", energy.coerceAtLeast(0))
        }
        CHRONOLITH_COST.find(responseText)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { raw ->
            preferences.setInt("_chronolithNextCost", raw)
            var cost = raw
            if (cost > 148) cost /= 10
            else if (cost > 47) cost /= 2
            cost -= 10
            preferences.setInt("_chronolithActivations", cost)
            preferences.setBoolean("_chronolithUsed", true)
        }
    }

    private fun parseCollectEnergy(responseText: String, preferences: Preferences?) {
        if (ENERGY_GAIN.containsMatchIn(responseText)) {
            preferences?.setInt("_energyCollected", preferences.getInt("_energyCollected", 0) + 1)
            preferences?.setBoolean("_scrapheapPowerCollected", true)
        }
    }
}
