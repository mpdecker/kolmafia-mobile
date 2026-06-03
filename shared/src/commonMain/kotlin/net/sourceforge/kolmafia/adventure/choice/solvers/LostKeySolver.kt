package net.sourceforge.kolmafia.adventure.choice.solvers

interface LostKeySolver {
    /** Mirrors LostKeyManager.autoKey(decision, stepCount, responseText) */
    fun autoKey(preference: Int, stepCount: Int, responseText: String): Int?

    object NoOp : LostKeySolver {
        override fun autoKey(preference: Int, stepCount: Int, responseText: String) = null
    }
}
