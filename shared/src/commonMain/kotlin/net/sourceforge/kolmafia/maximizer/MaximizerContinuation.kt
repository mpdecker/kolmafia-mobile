package net.sourceforge.kolmafia.maximizer

/** Desktop KoLmafia.permitsContinue / forceContinue for maximizer cooperative abort. */
object MaximizerContinuation {
    private var aborted = false

    fun forceContinue() {
        aborted = false
    }

    fun abort() {
        aborted = true
    }

    fun permitsContinue(): Boolean = !aborted
}
