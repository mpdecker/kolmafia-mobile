package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.preferences.Preferences

internal fun GameRuntimeLibrary.registerChatQueries(scope: AshScope) {

    regFn(scope, "say", AshType.BOOLEAN, listOf("msg" to AshType.STRING)) { _, args ->
        val sender = chatSender ?: return@regFn AshValue.of(false)
        val channel = preferences?.getString(Preferences.CURRENT_CHAT_CHANNEL, "clan") ?: "clan"
        val ok = kotlinx.coroutines.runBlocking {
            sender.send(channel, args[0].toString()).isSuccess
        }
        AshValue.of(ok)
    }

    regFn(scope, "say", AshType.BOOLEAN,
        listOf("channel" to AshType.STRING, "msg" to AshType.STRING)) { _, args ->
        val sender = chatSender ?: return@regFn AshValue.of(false)
        val ok = kotlinx.coroutines.runBlocking {
            sender.send(args[0].toString(), args[1].toString()).isSuccess
        }
        AshValue.of(ok)
    }

    regFn(scope, "msg", AshType.BOOLEAN,
        listOf("recipient" to AshType.STRING, "msg" to AshType.STRING)) { _, args ->
        val sender = chatSender ?: return@regFn AshValue.of(false)
        val ok = kotlinx.coroutines.runBlocking {
            sender.sendPrivate(args[0].toString(), args[1].toString()).isSuccess
        }
        AshValue.of(ok)
    }
}
