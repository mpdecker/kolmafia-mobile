package net.sourceforge.kolmafia.ash

/**
 * AshP763 — live `ranked_fam()` (mobile convenience; no desktop ASH equivalent).
 *
 * Name is historical/non-desktop: runs one tougher/ranked random flower fight
 * (`ranked=2` / `tougher=true`) via [net.sourceforge.kolmafia.session.PvpManager.executePvpRequest].
 * Returns true iff the fight completed without abort.
 */
internal fun GameRuntimeLibrary.registerAshP763Batch(scope: AshScope) {
    regFn(scope, "ranked_fam", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(runAshRankedFam())
    }
}
