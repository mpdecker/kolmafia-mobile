package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking

/** AshP266 — Chat whois probe + `is_online` ASH. */
internal fun GameRuntimeLibrary.registerAshP266Batch(scope: AshScope) {
    regFn(scope, "is_online", AshType.BOOLEAN, listOf("name" to AshType.STRING)) { _, args ->
        isOnlineAsh(args[0].toString())
    }
}

internal fun GameRuntimeLibrary.isOnlineAsh(name: String): AshValue {
    val probe = chatProbe ?: return AshValue.FALSE
    return runBlocking {
        AshValue.of(probe.isPlayerOnline(name))
    }
}
