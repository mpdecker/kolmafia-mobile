package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking

/** AshP267 — Chat slash commands: `slash_count`, `chat_clan`, `chat_private`, `chat_macro`. */
internal fun GameRuntimeLibrary.registerAshP267Batch(scope: AshScope) {
    regFn(scope, "slash_count", AshType.INT, listOf("item" to AshType.ITEM)) { _, args ->
        slashCountAsh(args[0])
    }

    regFn(scope, "chat_clan", AshType.VOID, listOf("message" to AshType.STRING)) { _, args ->
        chatClanAsh(args[0].toString())
    }

    regFn(
        scope,
        "chat_clan",
        AshType.VOID,
        listOf("message" to AshType.STRING, "channel" to AshType.STRING),
    ) { _, args ->
        chatClanAsh(args[0].toString(), args[1].toString())
    }

    regFn(
        scope,
        "chat_private",
        AshType.VOID,
        listOf("recipient" to AshType.STRING, "message" to AshType.STRING),
    ) { _, args ->
        chatPrivateAsh(args[0].toString(), args[1].toString())
    }

    regFn(scope, "chat_macro", AshType.VOID, listOf("macro" to AshType.STRING)) { _, args ->
        chatMacroAsh(args[0].toString())
    }
}

internal fun GameRuntimeLibrary.slashCountAsh(item: AshValue): AshValue {
    val itemId = resolveSlashCountItemId(item) ?: return AshValue.ZERO
    val probe = chatProbe ?: return AshValue.ZERO
    return runBlocking {
        AshValue.of(probe.slashCount(itemId).toLong())
    }
}

internal fun GameRuntimeLibrary.chatClanAsh(message: String, channel: String = "clan"): AshValue {
    val sender = chatSender ?: return AshValue.VOID
    runBlocking {
        sender.send(channel.trim(), message.trim())
    }
    return AshValue.VOID
}

internal fun GameRuntimeLibrary.chatPrivateAsh(recipient: String, message: String): AshValue {
    if (message.isEmpty() || message.startsWith("/")) return AshValue.VOID
    val sender = chatSender ?: return AshValue.VOID
    runBlocking {
        sender.sendPrivate(recipient, message)
    }
    return AshValue.VOID
}

internal fun GameRuntimeLibrary.chatMacroAsh(macro: String): AshValue {
    val probe = chatProbe ?: return AshValue.VOID
    runBlocking {
        probe.sendInternalCommand(macro.trim())
    }
    return AshValue.VOID
}

private fun GameRuntimeLibrary.resolveSlashCountItemId(item: AshValue): Int? {
    val ref = item.toString()
    return ref.toIntOrNull() ?: gameDatabase?.item(ref)?.id
}
