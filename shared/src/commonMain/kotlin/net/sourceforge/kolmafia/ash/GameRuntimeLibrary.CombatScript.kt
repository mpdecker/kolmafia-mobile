package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerCombatScript(scope: AshScope) {
    regFn(scope, "get_ccs_action", AshType.STRING, emptyList()) { ctx, _ ->
        val entry = activeCombatScriptEntry() ?: return@regFn AshValue.of("")
        val runtime = ctx as? AshRuntime ?: AshRuntime(this)
        if (!runSavedScript(entry.name, runtime)) AshValue.of("")
        else AshValue.of(runtime.lastCombatAction())
    }

    regFn(scope, "set_ccs_action", AshType.VOID, listOf("action" to AshType.STRING)) { ctx, args ->
        ctx.setCombatAction(args[0].toString())
        AshValue.VOID
    }

    regFn(scope, "can_still_steal", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(adventureManager?.canStillSteal() ?: false)
    }
}
