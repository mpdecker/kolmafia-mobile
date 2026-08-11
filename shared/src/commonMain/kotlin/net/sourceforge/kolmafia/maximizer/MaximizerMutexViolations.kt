package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.modifiers.BitmapModifier
import net.sourceforge.kolmafia.modifiers.ModifierValues

/** Desktop Maximizer mutex-violation checks (MUTEX_VIOLATIONS bitmap). */
object MaximizerMutexViolations {
    fun violations(mods: ModifierValues): Int = mods.get(BitmapModifier.MUTEX_VIOLATIONS)

    /** True when [candidate] has violation bits not present in [baseline]. */
    fun introducesNewViolations(baseline: ModifierValues, candidate: ModifierValues): Boolean {
        val baselineMask = violations(baseline)
        val candidateMask = violations(candidate)
        return (candidateMask and baselineMask.inv()) != 0
    }
}
