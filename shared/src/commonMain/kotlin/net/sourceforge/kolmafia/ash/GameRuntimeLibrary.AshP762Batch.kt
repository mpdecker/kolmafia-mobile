package net.sourceforge.kolmafia.ash

/**
 * AshP762 — live `pvp_attack(player)` (mobile convenience; no desktop ASH equivalent).
 *
 * Resolves [player] via profile helpers, prefetches stances, uses mission `lootwhatever`
 * when canInteract else `flowers`, stance 0 or first known, then
 * [net.sourceforge.kolmafia.session.PvpManager.executeDirectedPvpRequest].
 * Returns true iff the fight completed without abort.
 */
internal fun GameRuntimeLibrary.registerAshP762Batch(scope: AshScope) {
    regFn(scope, "pvp_attack", AshType.BOOLEAN, listOf("player" to AshType.STRING)) { _, args ->
        AshValue.of(runAshPvpAttack(args[0].toString()))
    }
}
