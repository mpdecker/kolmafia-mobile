package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerEnvironmentQueries(scope: AshScope) {
    regFn(scope, "get_version", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(GameRuntimeLibrary.VERSION)
    }

    // Desktop get_revision → INT via StaticEntity.getRevision(). Mobile REVISION remains the
    // phaseNN string constant; ASH returns the numeric phase digits (desktop-shaped INT).
    regFn(scope, "get_revision", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(GameRuntimeLibrary.revisionNumber().toLong())
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
