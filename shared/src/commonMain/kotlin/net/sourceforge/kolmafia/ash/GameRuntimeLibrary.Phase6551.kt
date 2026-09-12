package net.sourceforge.kolmafia.ash

/**
 * Phases 6551–6570 — ASH behavioral deepen XLV Track A.
 *
 * - can_adventure / pre_validate_adventure — Removed root + cellar Quest.RAT + none/blank
 * - prepare_for_adventure — last-location / none → FALSE; prep clusters via AdventurePrep
 * - adventure / adv1 — Macrofier.setMacroOverride filter + continueValue / EXIT VOID
 * - can_walk_from_choice / choice_follows_fight / fight_follows_choice / in_multi_fight polish
 * - tavern() / tavern(goal) — unknown goal → -1; permitsContinue gate
 *
 * REVISION stays phase6850 until parent wraps to phase6850.
 */
internal fun GameRuntimeLibrary.registerPhase6551(scope: AshScope) {
    // Behavioral patches live in AdventurePrep / AdventureZoneGates / AdventureManager /
    // ChoiceCombatAshState / Macrofier / GameRuntimeLibrary.runAdventureTurns /
    // Character.kt / Phase4470 / Combat.kt / AshP950 / AshP1004 tavern.
}
