package net.sourceforge.kolmafia.ash

/**
 * Phases 6711–6730 — ASH behavioral deepen XLVII Track C
 * (maximize / modifiers / character / collections).
 *
 * - maximize — exotic boost-source residual (HotDog/Speakeasy/synthesize organ caps
 *   via ConsumptionEligibility path-base + SPLEEN_CAPACITY)
 * - numeric_modifier / boolean_modifier / string_modifier — Generated/_spec +
 *   Monster Type:name case-insensitive edges
 * - get_property / remove_property — global/user/System.* residual
 *   (2-arg get no longer blocks per-user-global; 1-arg remove user→global fallthrough)
 * - refresh_status / restore_hp / restore_mp — checkpoint wasRecoveryActive nesting
 * - mood_execute — inheritance residual (library parents/triggers on load)
 * - fullness_limit / inebriety_limit / spleen_limit — path/class organ caps + spleen mod
 * - available_amount / *_amount / get_free_pulls / get_no_pulls — freepulls lazy cache
 * - eudora — AccountSync whichpenpal + currentEudora + status-refresh flag
 *
 * REVISION stays phase6850 (parent XLVII wrap bumps to phase6850).
 */
internal fun GameRuntimeLibrary.registerPhase6711(scope: AshScope) {
    // Behavioral patches live in MaximizerBoostSourceRules / ModifierDatabase /
    // AshP21 / Prefs / AshP905 / RecoveryManager / MoodManager /
    // ConsumptionEligibility / AscensionPath / Collections / AccountSync /
    // LongTailCli / AshP943 / AshP118 / AshP120.
}
