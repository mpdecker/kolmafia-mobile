package net.sourceforge.kolmafia.session

/**
 * Desktop [ChoiceControl] case 326 Showdown — Mother Slime wait.
 *
 * Not a Slime Tube zone state machine — desktop has none.
 */
object SlimeTubeManager {

    const val LOCATION = "The Slime Tube"
    const val SHOWDOWN_CHOICE = 326
    const val ENGULFED_CHOICE = 337

    fun motherSlimeWait(choice: Int, decision: Int): String? {
        if (choice != SHOWDOWN_CHOICE || decision != 2) return null
        return "Mother Slime waits for you."
    }

    fun postChoice(choiceId: Int, decision: Int, sessionLog: (String) -> Unit = {}): Boolean {
        val wait = motherSlimeWait(choiceId, decision) ?: return false
        HobopolisManager.recordPendingStop(wait)
        sessionLog(wait)
        return true
    }
}
