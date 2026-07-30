package net.sourceforge.kolmafia.ash

/** AshP262 — Interactive ASH `user_notify` headless no-ops (no system-tray UI). */
internal fun GameRuntimeLibrary.registerAshP262Batch(scope: AshScope) {
    regFn(scope, "user_notify", AshType.VOID, listOf("message" to AshType.STRING)) { _, _ ->
        AshValue.VOID
    }

    regFn(
        scope,
        "user_notify",
        AshType.VOID,
        listOf(
            "message" to AshType.STRING,
            "onlyShowWhenHidden" to AshType.BOOLEAN,
        ),
    ) { _, _ ->
        AshValue.VOID
    }
}
