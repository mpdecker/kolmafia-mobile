package net.sourceforge.kolmafia.ash

/**
 * Phases 6591–6610 — ASH behavioral deepen XLV Track C.
 *
 * - get_revision → INT (desktop StaticEntity.getRevision); REVISION stays phaseNN string
 * - get_counter / get_counters loc=* exempt skip
 * - session_logs negative-day throw + multi-day files
 * - get_property / remove_property user-editable + per-user-global polish
 * - form_fields last-visit / POST decodeField parity
 * - is_banished INT stub removed (MONSTER/PHYLUM live)
 * - monster_factoids_available cachedOnly corpus
 * - sweet_synthesis non-candy early false
 *
 * REVISION not bumped (parent XLV wrap).
 */
internal fun GameRuntimeLibrary.registerPhase6591(scope: AshScope) {
    // Behavioral patches live in Environment / AshP905 / SessionLog / Prefs /
    // AshP950 / AshP8 / Phase4470 / AshP928 / ChoiceCombatAshState / TurnCounter.
}
