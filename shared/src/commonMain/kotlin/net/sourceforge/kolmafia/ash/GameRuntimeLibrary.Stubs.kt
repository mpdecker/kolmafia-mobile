package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerLongTailStubs(scope: AshScope) {
    regFn(scope, "user_confirm", AshType.BOOLEAN, listOf("prompt" to AshType.STRING)) { _, args ->
        throw ScriptException("user_confirm not available on mobile: ${args[0]}")
    }

    regFn(scope, "user_prompt", AshType.STRING, listOf("prompt" to AshType.STRING)) { _, args ->
        throw ScriptException("user_prompt not available on mobile: ${args[0]}")
    }

    regFn(scope, "pvp_attack", AshType.BOOLEAN, listOf("player" to AshType.STRING)) { _, _ ->
        AshValue.of(false)
    }

    regFn(scope, "ranked_fam", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(false)
    }
}
