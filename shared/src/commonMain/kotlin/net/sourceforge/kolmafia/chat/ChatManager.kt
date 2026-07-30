package net.sourceforge.kolmafia.chat

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatManager {

    // All map mutations happen on the ChatPoller's single coroutine (sequential).
    // channelFlow/pmFlow are called from the UI before polling starts.
    // This ordering assumption holds for the current usage in ChatScreen.
    private val channelBuffers = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()
    private val pmBuffers = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()

    var activeFaxBot: String? = null
    private var lastFaxBotMessage: String? = null

    private val _knownChannels = MutableStateFlow<Set<String>>(emptySet())
    val knownChannels: StateFlow<Set<String>> = _knownChannels.asStateFlow()

    fun channelFlow(channel: String): StateFlow<List<ChatMessage>> =
        channelBuffers.getOrPut(channel) { MutableStateFlow(emptyList()) }.asStateFlow()

    fun pmFlow(sender: String): StateFlow<List<ChatMessage>> =
        pmBuffers.getOrPut(sender) { MutableStateFlow(emptyList()) }.asStateFlow()

    /** Synthetic internal event (desktop `InternalMessage` / `chat_notify` ASH). */
    fun notify(message: String, color: String) {
        val normalizedColor = color.trim().trim('"')
        dispatch(
            listOf(
                ChatMessage(
                    sender = "",
                    senderId = "0",
                    recipient = null,
                    channel = EVENTS_CHANNEL,
                    content = message.trim(),
                    isAction = false,
                    epochSeconds = System.currentTimeMillis() / 1000,
                    color = normalizedColor,
                ),
            ),
        )
    }

    fun dispatch(messages: List<ChatMessage>) {
        for (msg in messages) {
            captureFaxBotMessage(msg)
            if (msg.senderId.isNotBlank() && msg.senderId != "0") {
                PlayerIdRegistry.register(msg.sender, msg.senderId)
            }
            when {
                msg.channel != null -> {
                    val flow = channelBuffers.getOrPut(msg.channel) { MutableStateFlow(emptyList()) }
                    flow.update { it + msg }
                    _knownChannels.update { it + msg.channel }
                }
                msg.recipient != null -> {
                    val flow = pmBuffers.getOrPut(msg.sender) { MutableStateFlow(emptyList()) }
                    flow.update { it + msg }
                }
            }
        }
    }

    fun captureFaxBotMessage(msg: ChatMessage) {
        val bot = activeFaxBot ?: return
        if (msg.channel != null) return
        if (msg.sender.equals(bot, ignoreCase = true)) {
            lastFaxBotMessage = msg.content
        }
    }

    fun consumeLastFaxBotMessage(): String? {
        val message = lastFaxBotMessage
        lastFaxBotMessage = null
        return message
    }

    companion object {
        const val EVENTS_CHANNEL = "events"
    }
}
