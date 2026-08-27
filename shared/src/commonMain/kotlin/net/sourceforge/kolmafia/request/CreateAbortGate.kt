package net.sourceforge.kolmafia.request

/**
 * Create/craft alias for [RequestAbortGate] (kept so existing create callers compile).
 */
object CreateAbortGate {
    var forceAbort: Boolean
        get() = RequestAbortGate.forceAbort
        set(value) {
            RequestAbortGate.forceAbort = value
        }

    var inChoiceProvider: () -> Boolean
        get() = RequestAbortGate.inChoiceProvider
        set(value) {
            RequestAbortGate.inChoiceProvider = value
        }

    var inFightProvider: () -> Boolean
        get() = RequestAbortGate.inFightProvider
        set(value) {
            RequestAbortGate.inFightProvider = value
        }

    fun shouldAbort(): Boolean = RequestAbortGate.shouldAbort()

    fun resetForTest() = RequestAbortGate.resetForTest()
}
