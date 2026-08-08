package net.sourceforge.kolmafia.request

sealed class ConsumptionRequestOutcome {
    data class Completed(val consumed: Int) : ConsumptionRequestOutcome()
    data class Aborted(val consumed: Int, val reason: String) : ConsumptionRequestOutcome()
}
