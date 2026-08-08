package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.clan.ClanManager

/** AshP301 — clan id/name ASH: `get_clan_id`, `get_clan_name`. */
internal fun GameRuntimeLibrary.registerAshP301Batch(scope: AshScope) {
    regFn(scope, "get_clan_id", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(ClanManager.getClanId())
    }

    regFn(scope, "get_clan_name", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(ClanManager.getClanName())
    }
}
