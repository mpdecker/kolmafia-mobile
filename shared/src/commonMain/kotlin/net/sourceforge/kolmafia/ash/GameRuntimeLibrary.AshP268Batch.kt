package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.chat.PlayerIdRegistry

/** AshP268 — Chat who/player lookups: `who_clan`, `get_player_id`, `get_player_name`. */
internal fun GameRuntimeLibrary.registerAshP268Batch(scope: AshScope) {
    val whoClanType = AggregateType(AshType.STRING, AshType.BOOLEAN)

    regFn(scope, "who_clan", whoClanType, emptyList()) { _, _ ->
        whoClanAsh(whoClanType)
    }

    regFn(scope, "get_player_id", AshType.STRING, listOf("name" to AshType.STRING)) { _, args ->
        getPlayerIdAsh(args[0].toString())
    }

    regFn(scope, "get_player_name", AshType.STRING, listOf("id" to AshType.INT)) { _, args ->
        getPlayerNameAsh(args[0].toString())
    }
}

internal fun GameRuntimeLibrary.whoClanAsh(type: AggregateType): AshValue {
    val contacts = runBlocking {
        chatProbe?.whoClan() ?: emptyMap()
    }
    val result = AggregateValue(type)
    contacts.forEach { (name, inChat) ->
        result[AshValue.of(name)] = AshValue.of(inChat)
    }
    return result
}

internal fun GameRuntimeLibrary.getPlayerIdAsh(name: String): AshValue {
    val playerId = runBlocking {
        chatProbe?.lookupPlayerId(name)
            ?: PlayerIdRegistry.getPlayerId(name, retrieveId = false)
    }
    return AshValue.of(playerId)
}

internal fun GameRuntimeLibrary.getPlayerNameAsh(id: String): AshValue {
    val playerName = runBlocking {
        chatProbe?.lookupPlayerName(id)
            ?: PlayerIdRegistry.getPlayerName(id, retrieveName = false)
    }
    return AshValue.of(playerName)
}
