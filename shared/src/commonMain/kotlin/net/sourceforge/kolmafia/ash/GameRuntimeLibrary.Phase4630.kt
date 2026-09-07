package net.sourceforge.kolmafia.ash

/**
 * Phases 4571–4630 — ASH behavioral deepen XII + HTTP request residual hubs.
 *
 * 4571 get_counters/get_counter loc=* exempt skip ·
 * 4572 run_choice(-1) pickGoalChoice ·
 * 4573 can_equip via EquipmentManager ·
 * 4574 equip CLI fallback without HTTP ·
 * 4575 form_fields() lastVisitPath query ·
 * 4576–4630 Curse/Trophy/Umbrella/Richard/SuburbanDis/ShowClan/
 * MultiUse/SingleUse/NPCPurchase/CreateItem/InternalChat/
 * ClanLoungeSwimmingPool/PalmFrond/CoinMasterPurchase/BarrelShrine hubs
 */
internal fun GameRuntimeLibrary.registerPhase4630(scope: AshScope) {
    // Behavioral patches live in TurnCounter, AshP889, AshP898, AshP950.
    // Request hubs are typed objects wired from processVisitResponseHooks.
}
