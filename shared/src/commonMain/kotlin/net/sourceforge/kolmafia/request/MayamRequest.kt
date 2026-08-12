package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.data.MayamAvailability
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [MayamCommand] resonance path — Mayam Calendar choice 1527. */
class MayamRequest(
    private val useItemRequest: UseItemRequest,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun takeResonance(
        resonanceQuery: String,
        preferences: Preferences?,
        inventoryCounts: (Int) -> Int,
        calendarEquipped: Boolean = false,
    ): Result<String> {
        if (inventoryCounts(CALENDAR_ID) <= 0 && !calendarEquipped) {
            return Result.failure(IllegalStateException("You need a Mayam Calendar"))
        }
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        val resonance = MayamAvailability.resolveResonance(resonanceQuery, prefs)
            ?: return Result.failure(
                IllegalArgumentException("Too many resonance matches for $resonanceQuery."),
            )
        val symbols = MayamAvailability.symbolsFor(resonance)
            ?: return Result.failure(IllegalArgumentException("Unknown resonance: $resonance"))
        val available = MayamAvailability.availableResonances(prefs)
        if (!available.contains(resonance)) {
            return Result.failure(
                IllegalStateException("Resonance \"$resonance\" is not available."),
            )
        }

        useItemRequest.use(CALENDAR_ID, 1).exceptionOrNull()?.let { return Result.failure(it) }

        for ((ringFromTop, symbol) in symbols.withIndex()) {
            val ringIndex = ringFromTop // 0..3 from outer
            val pos = MayamAvailability.positionOnRing(ringIndex, symbol)
                ?: return Result.failure(
                    IllegalStateException("Cannot match symbol $symbol on ring ${ringIndex + 1}."),
                )
            // Desktop spin(3 - ring, pos) where ring starts at 0 → r = 3,2,1,0
            val r = 3 - ringIndex
            choiceRequest.choose(
                CHOICE_ID,
                SPIN_OPTION,
                mapOf("r" to r.toString(), "p" to pos.toString()),
            ).exceptionOrNull()?.let { return Result.failure(it) }
        }

        return choiceRequest.choose(CHOICE_ID, CONSIDER_OPTION).map { (html, _) ->
            MayamAvailability.markSymbolsUsed(prefs, symbols)
            html
        }
    }

    companion object {
        const val CALENDAR_ID = 11572
        const val CHOICE_ID = 1527
        const val SPIN_OPTION = 2
        const val CONSIDER_OPTION = 1

        /** Parse `resonance <name>` or bare `<name>` from CLI parameters. */
        fun parseResonanceQuery(parameters: String): String? {
            val trimmed = parameters.trim()
            if (trimmed.isEmpty()) return null
            val lower = trimmed.lowercase()
            return if (lower.startsWith("resonance")) {
                trimmed.substringAfter("resonance").trim().ifEmpty { null }
            } else {
                trimmed
            }
        }
    }
}
