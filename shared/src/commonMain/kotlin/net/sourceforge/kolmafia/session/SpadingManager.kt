package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Mobile's non-UI spading bridge.
 *
 * Desktop can launch an arbitrary ASH script.  Mobile records the same event
 * boundary and response metadata for session tooling, while intentionally not
 * attempting to execute a file-backed script.
 */
object SpadingManager {
    enum class Event {
        COMBAT_ROUND, CHOICE_VISIT, CHOICE, CONSUME, DESC_ITEM, MEAT_DROP, PVP, PLACE
    }

    fun enabled(preferences: Preferences?): Boolean =
        preferences?.getString("spadingScript", "").orEmpty().isNotBlank()

    fun record(
        event: Event,
        metadata: String,
        response: String?,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ): Boolean {
        if (!enabled(preferences) || response == null) return false
        val signature = "${event.name}:$metadata:${response.hashCode()}"
        if (preferences?.getString("_lastSpadingSignature", "") == signature) return false
        preferences?.setString("_lastSpadingSignature", signature)
        preferences?.setString("_lastSpadingEvent", event.name)
        preferences?.setString("_lastSpadingMeta", metadata)
        preferences?.setInt("_lastSpadingResponseLength", response.length)
        sessionLogger?.appendRawLine("Spading ${event.name}: $metadata")
        return true
    }

    fun processPlace(url: String, html: String, preferences: Preferences?, logger: SessionLogger?) =
        record(Event.PLACE, url, html, preferences, logger)

    fun processChoice(choice: String, html: String, preferences: Preferences?, logger: SessionLogger?) =
        record(Event.CHOICE, choice, html, preferences, logger)

    fun processChoiceVisit(choice: String, html: String, preferences: Preferences?, logger: SessionLogger?) =
        record(Event.CHOICE_VISIT, choice, html, preferences, logger)

    fun processCombatRound(monster: String, html: String, preferences: Preferences?, logger: SessionLogger?) =
        record(Event.COMBAT_ROUND, monster, html, preferences, logger)
}
