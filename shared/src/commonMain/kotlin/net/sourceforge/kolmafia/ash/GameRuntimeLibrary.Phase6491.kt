package net.sourceforge.kolmafia.ash

/**
 * Phases 6491–6510 — ASH behavioral deepen XLIV Track A.
 *
 * - get_ccs_action(index) → CombatActionManager.getCurrentKey + allowMacro
 * - set_ccs / read_ccs / write_ccs → CcsFileManager-style lookup + active reload
 * - run_combat(filter) → Macrofier.setMacroOverride ASH filter callback
 * - is_banished / banished_by → Banisher.isEffective via BanishManager
 * - tracked_by / track_copy_count / track_ignore_queue → Tracker.isEffective
 * - choice_follows_fight → ChoiceCombatAshState post-fight sync
 *
 * REVISION stays phase6490 until parent wraps to phase6670.
 */
internal fun GameRuntimeLibrary.registerPhase6491(scope: AshScope) {
    // Behavioral patches live in CombatScript / AshP889 / AshP943 / AshP950 /
    // Macrofier / CombatActionManager / ChoiceCombatAshState / AdventureManager.
}
