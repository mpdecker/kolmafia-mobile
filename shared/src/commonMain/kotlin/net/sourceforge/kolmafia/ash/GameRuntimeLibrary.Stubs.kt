package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerLongTailStubs(scope: AshScope) {
    regFn(scope, "pvp_attack", AshType.BOOLEAN, listOf("player" to AshType.STRING)) { _, _ ->
        AshValue.of(false)
    }

    regFn(scope, "ranked_fam", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(false)
    }
}
