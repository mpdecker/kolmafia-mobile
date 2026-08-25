package net.sourceforge.kolmafia.recovery

/**
 * Desktop "for the benefit of betweenBattleScripts" hook from UseItem / IoTM requests.
 * Wired from DI; no-ops until [invoke] is set.
 */
object BetweenBattleInvoker {
    @Volatile
    var invoke: (suspend (isFullCheck: Boolean) -> Unit)? = null

    suspend fun run(isFullCheck: Boolean = true) {
        invoke?.invoke(isFullCheck)
    }

    fun resetForTest() {
        invoke = null
    }
}
