package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.choice.ChoiceUtilities

/**
 * Classify an `inv_use` response that may already be a followed redirect to
 * fight.php / choice.php (desktop [GenericRequest] + [UseItemRequest.runOneIteration]).
 */
object UseItemRedirect {
    sealed class Kind {
        data object Fight : Kind()
        data class Choice(val choiceId: Int) : Kind()
        data object None : Kind()
    }

    fun classify(responseText: String, finalUrl: String = ""): Kind {
        val url = finalUrl.lowercase()
        if (url.contains("fight.php") || url.contains("fambattle.php") ||
            responseText.contains("You're fighting", ignoreCase = true)
        ) {
            return Kind.Fight
        }
        if (url.contains("choice.php") || responseText.contains("whichchoice", ignoreCase = true)) {
            val id = ChoiceUtilities.extractChoiceId(responseText)
                ?: ChoiceUtilities.extractChoiceFromUrl(finalUrl).takeIf { it > 0 }
                ?: 0
            if (id > 0) return Kind.Choice(id)
        }
        return Kind.None
    }

    /** Desktop processResults: choice redirect owns consumption — skip parseConsumption. */
    fun shouldSkipParseConsumption(kind: Kind): Boolean =
        kind is Kind.Fight || kind is Kind.Choice
}
