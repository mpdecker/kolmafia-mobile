package net.sourceforge.kolmafia.ash

/**
 * Phases 5771–5830 — ASH behavioral deepen XXXII.
 *
 * 5771–5782 DreadsylvaniaRequest full port (SHORTCUTS, zone mapping, parseResponse,
 *           registerRequest forceloc + feedbooze) ·
 * 5783–5798 RequestLogger session-log depth (clan lounge/rumpus/stash, closet/storage/
 *           display, chateau/campaway, edbase/spaaace/volcano/leaflet/mom/mcd/island/hermit) ·
 * 5799–5808 MANUAL concoction gate + craft_type (MANUAL) qualifier ·
 * 5809–5818 ASH clan lounge/rumpus + familiar lockequip + file_to_map corpus ·
 * 5819–5822 HobopolisManager WINWINWIN boss-drop parse ·
 * 5823–5826 HolidayNames KoL combo-day / Drunksgiving merge ·
 * 5827–5830 Banish/quest edges verified at parity + recount
 */
internal fun GameRuntimeLibrary.registerPhase5830(scope: AshScope) {
    // Behavioral patches live in DreadsylvaniaRequest / RequestLogger /
    // ConcoctionData / CraftTypeDescription / HobopolisManager / HolidayNames /
    // ClanManager ASH / FamiliarRequest.lockEquip.
}
