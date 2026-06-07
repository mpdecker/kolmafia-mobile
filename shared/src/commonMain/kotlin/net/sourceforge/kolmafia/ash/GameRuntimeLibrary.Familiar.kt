package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking

internal fun GameRuntimeLibrary.registerFamiliarQueries(scope: AshScope) {

    regFn(scope, "have_familiar", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val name = args[0].toString()
        val has = familiarManager?.state?.value?.ownedFamiliars
            ?.any { it.race.equals(name, ignoreCase = true) } ?: false
        AshValue.of(has)
    }

    regFn(scope, "my_familiar_weight", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.familiarWeight ?: 0).toLong())
    }

    regFn(scope, "to_familiar", AshType.FAMILIAR,
        listOf("name" to AshType.STRING)) { _, args ->
        AshValue.familiar(args[0].toString())
    }

    regFn(scope, "use_familiar", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val fm = familiarManager ?: return@regFn AshValue.of(false)
        val success = runBlocking {
            fm.setFamiliar(args[0].toString())
        }.isSuccess
        AshValue.of(success)
    }
}
