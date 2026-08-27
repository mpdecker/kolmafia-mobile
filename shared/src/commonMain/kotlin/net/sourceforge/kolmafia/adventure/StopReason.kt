package net.sourceforge.kolmafia.adventure

sealed class StopReason {
    object UserCancelled : StopReason()
    object NoAdventuresLeft : StopReason()
    object CharacterDeath : StopReason()
    object AllMonstersBanished : StopReason()
    data class GoalMet(val description: String) : StopReason()
    data class AutoStop(val encounterName: String) : StopReason()
    data class MacroError(val message: String) : StopReason()
    data class NetworkError(val cause: Throwable) : StopReason()
    /** Desktop adventure-failure table hit (ERROR or PENDING). */
    data class AdventureFailure(val message: String, val pending: Boolean = false) : StopReason()
}
