package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.ChoiceCombatAshState

/**
 * Selective GenericRequest.abortIfInFightOrChoice hub (Phases 2211–2225).
 * Full GenericRequest remains deferred.
 */
object RequestAbortGate {
    @Volatile
    var forceAbort: Boolean = false

    @Volatile
    var inChoiceProvider: () -> Boolean = {
        ChoiceCombatAshState.handlingChoice || ChoiceCombatAshState.choiceFollowsFight
    }

    @Volatile
    var inFightProvider: () -> Boolean = {
        ChoiceCombatAshState.inMultiFight || ChoiceCombatAshState.currentRound > 0
    }

    fun shouldAbort(): Boolean =
        forceAbort || inFightProvider() || inChoiceProvider()

    /** Desktop [GenericRequest.abortIfInFightOrChoice]. */
    fun abortIfInFightOrChoice(silent: Boolean = true): Boolean {
        if (!shouldAbort()) return false
        if (!silent) {
            lastAbortMessage = when {
                forceAbort -> "Request aborted."
                inFightProvider() -> "You are currently in a fight."
                else -> "You are currently in a choice."
            }
        }
        return true
    }

    @Volatile
    var lastAbortMessage: String = ""

    fun resetForTest() {
        forceAbort = false
        lastAbortMessage = ""
        inChoiceProvider = {
            ChoiceCombatAshState.handlingChoice || ChoiceCombatAshState.choiceFollowsFight
        }
        inFightProvider = {
            ChoiceCombatAshState.inMultiFight || ChoiceCombatAshState.currentRound > 0
        }
    }
}
