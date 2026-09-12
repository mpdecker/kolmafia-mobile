package net.sourceforge.kolmafia.ash

/**
 * Phases 6651–6670 — ASH behavioral deepen XLVI Track C (adventure / choice / CCS).
 *
 * - can_adventure / pre_validate_adventure — Barroom Brawl Quest.RAT + isNoneLocation
 * - prepare_for_adventure / adventure / adv1 — Macrofier filter (incl. blank) override
 * - run_choice — option==-1 goal pick only; custom=false skips auto-fight
 * - run_combat(filter) — Macrofier.setMacroOverride always (blank clears)
 * - get_ccs_action / set_ccs / write_ccs — ccs/ directory rescan reload edges
 * - available_choice_options / can_walk_from_choice — ChoiceCombatAshState.canWalkAway sync
 * - tavern residual verified; get_auto_attack / set_auto_attack account+pref sync
 *
 * REVISION stays phase6850 until parent wraps to phase6850.
 */
internal fun GameRuntimeLibrary.registerPhase6651(scope: AshScope) {
    // Behavioral patches live in AdventureZoneGates / AdventurePrep / AdventureManager /
    // ChoiceCombatAshState / ChoiceWalkAway / CombatActionManager / Macrofier /
    // AshP889 / AshP943 / Phase4470 / GameRuntimeLibrary.runAdventureTurns /
    // UserDataFileIO.listNames / LongTailCli autoattack.
}
