package net.sourceforge.kolmafia.ash

/**
 * Phases 5831–5890 — ASH behavioral deepen XXXIII.
 *
 * 5831–5845 EquipmentManager weapon/outfit queries (wielding*, holsteredSixgun,
 *           usingShield, getWeaponType/getHitStat, glove/foam, outfit helpers) ·
 * 5846–5855 TurnCounter lifecycle (temporary counters, getExpiredCounter,
 *           getUnexpiredCounters, lastWarned memory) ·
 * 5856–5865 RequestLogger transfer item-name depth (closet/storage/display/stash) ·
 * 5866–5872 MallPriceManager age-gated getMallPrice + current-day cache ·
 * 5873–5880 DynamicChoiceSpoilers Overlook Lodge exclusion + Tomb 1049 class answers ·
 * 5881–5885 FamiliarRequest session-log + familiar display DI ·
 * 5886–5890 corpus + parity recount
 */
internal fun GameRuntimeLibrary.registerPhase5890(scope: AshScope) {
    // Behavioral patches live in EquipmentManager / TurnCounter / RequestLogger /
    // MallPriceManager / DynamicChoiceSpoilers / FamiliarRequest.
}
