package net.sourceforge.kolmafia.ash

/**
 * Phases 6631–6650 — ASH behavioral deepen XLVI Track B (rates + fight actions).
 *
 * - appearance_rates — ZoneCombatCalculator + AdventureQueueDatabase.checkZones
 * - get_location_monsters — roster (!includeQueue) vs stateful positive-rate filter
 * - combat_skill_available — fight-dropdown depth (AvailableCombatSkills / Grey Goose /
 *   lovebug / gladiator / heartstone unlock prefs)
 * - attack / steal / twiddle / runaway / pickpocket — BUFFER offline vs live path
 *
 * REVISION stays phase6670 (do not bump; parent wraps to phase6670).
 */
internal fun GameRuntimeLibrary.registerPhase6631(scope: AshScope) {
    // Behavioral patches live in AshP38Batch / AshP889TrackA / AshP991TrackR /
    // AvailableCombatSkills / CombatSkillDropdownParser / ZoneCombatCalculator.
}
