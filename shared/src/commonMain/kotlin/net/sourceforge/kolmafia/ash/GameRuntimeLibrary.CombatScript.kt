package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.combat.CombatActionManager
import net.sourceforge.kolmafia.combat.MonsterStatusTracker

internal fun GameRuntimeLibrary.registerCombatScript(scope: AshScope) {
    // Combat ASH script last action (mobile COMBAT script convenience)
    regFn(scope, "get_ccs_action", AshType.STRING, emptyList()) { ctx, _ ->
        val entry = activeCombatScriptEntry() ?: return@regFn AshValue.of("")
        val runtime = ctx as? AshRuntime ?: AshRuntime(this)
        if (!runSavedScript(entry.name, runtime)) AshValue.of("")
        else AshValue.of(runtime.lastCombatAction())
    }

    // Desktop get_ccs_action(index) — resolved CCS / battleAction line for current encounter
    regFn(scope, "get_ccs_action", AshType.STRING, listOf("index" to AshType.INT)) { _, args ->
        val index = args[0].toLong().toInt()
        val encounter = MonsterStatusTracker.getLastMonsterName().ifBlank { "default" }
        AshValue.of(
            CombatActionManager.getCombatAction(encounter, index, allowMacro = true, preferences),
        )
    }

    regFn(scope, "set_ccs_action", AshType.VOID, listOf("action" to AshType.STRING)) { ctx, args ->
        ctx.setCombatAction(args[0].toString())
        AshValue.VOID
    }

    regFn(scope, "can_still_steal", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(adventureManager?.canStillSteal() ?: false)
    }
}
