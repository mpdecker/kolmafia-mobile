package net.sourceforge.kolmafia.ash

/**
 * Phases 6451–6470 — ASH behavioral deepen XLIII Tracks C–D.
 *
 * Track C (6451–6460):
 * - can_adventure / prepare_for_adventure — AdventureZoneGates consumable/IoTM depth
 *   (Rabbit Hole / Suburbs / Wormwood / Spaaace / Portal / Memories / Deep Machine /
 *   Video Game / Astral mushroom / Mole gong / Telegram / Casino)
 * - appearance_rates / get_location_monsters — ZoneCombatCalculator residual verified
 * - adventure / adv1 — adventuresUsed override + filter Macrofier polish
 *
 * Track D (6461–6470):
 * - can_walk_from_choice — ChoiceWalkAway 1543/1601 + ChoiceCombatAshState fallback
 * - available_choice_options / run_choice — DynamicChoiceSpoilers via ChoiceAdventures
 * - run_combat / get_ccs_action — Macrofier hulking/RAM + consult decline + encounterKey
 *
 * Runtime revision: phase6490.
 */
internal fun GameRuntimeLibrary.registerPhase6470(scope: AshScope) {
    // Behavioral patches live in AdventureZoneGates / AdventurePrep / AdventureManager /
    // ChoiceWalkAway / Macrofier / CombatScript / GameRuntimeLibrary.runAdventureTurns /
    // resolveCombatMacro / Phase4470 can_walk_from_choice.
}
