package net.sourceforge.kolmafia.ash

/**
 * Phases 6611–6630 — ASH behavioral deepen XLVI Track A (combat prediction).
 *
 * - expected_damage — RandomModifierStats (OCRS) attack/element + FightRequest absorb/res
 * - elemental_resistance — OCRS attack-element overlay (byLevel matches desktop; callers clamp)
 * - will_usually_miss / will_usually_dodge — OCRS atk/def + ATTACKS_CANT_MISS miss gate
 * - item_drops / item_drops_array — DropFlag.UNKNOWN_RATE `"0"` when chance==0
 * - meat_drop — OCRS broke / solid gold residual (via RandomModifierStats)
 * - jump_chance — OCRS init/attack overlay on all monster overloads
 *
 * REVISION stays phase6850 until parent wraps to phase6850.
 */
internal fun GameRuntimeLibrary.registerPhase6611(scope: AshScope) {
    // Behavioral patches live in CombatAdjustment / AshP39 / AshP43 / AshP45 / AshP46.
}
