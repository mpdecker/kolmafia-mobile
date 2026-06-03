package net.sourceforge.kolmafia.adventure.choice

fun interface ChoiceHandler {
    /**
     * Returns the option number to submit (1-based), or null to fall through
     * to raw user preference / manual browser control.
     */
    fun decide(ctx: ChoiceContext): Int?
}
