package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerEnvironmentQueries(scope: AshScope) {
    regFn(scope, "get_version", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(GameRuntimeLibrary.VERSION)
    }

    regFn(scope, "get_revision", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(GameRuntimeLibrary.REVISION)
    }

    regFn(scope, "write", AshType.VOID, listOf("msg" to AshType.STRING)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }

    regFn(scope, "writeln", AshType.VOID, listOf("msg" to AshType.STRING)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }
}
