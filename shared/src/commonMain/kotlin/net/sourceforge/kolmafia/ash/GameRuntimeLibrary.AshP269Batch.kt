package net.sourceforge.kolmafia.ash

/** AshP269 — Synthetic internal chat events: `chat_notify`. */
internal fun GameRuntimeLibrary.registerAshP269Batch(scope: AshScope) {
    regFn(
        scope,
        "chat_notify",
        AshType.VOID,
        listOf("message" to AshType.STRING, "color" to AshType.STRING),
    ) { _, args ->
        chatNotifyAsh(args[0].toString(), args[1].toString())
    }
}

internal fun GameRuntimeLibrary.chatNotifyAsh(message: String, color: String): AshValue {
    chatManager?.notify(message, color)
    return AshValue.VOID
}
