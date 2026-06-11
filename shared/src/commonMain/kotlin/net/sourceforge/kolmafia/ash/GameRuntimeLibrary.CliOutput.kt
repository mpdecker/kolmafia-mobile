package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerCliOutput(scope: AshScope) {
    regFn(scope, "cli_execute_output", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(lastCliOutput.toString().trimEnd())
    }
}
