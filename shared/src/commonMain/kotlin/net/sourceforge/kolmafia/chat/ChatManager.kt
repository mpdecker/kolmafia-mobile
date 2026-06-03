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

    private val _knownChannels = MutableStateFlow<Set<String>>(emptySet())
    val knownChannels: StateFlow<Set<String>> = _knownChannels.asStateFlow()

    fun channelFlow(channel: String): StateFlow<List<ChatMessage>> =
        channelBuffers.getOrPut(channel) { MutableStateFlow(emptyList()) }.asStateFlow()

    fun pmFlow(sender: String): StateFlow<List<ChatMessage>> =
        pmBuffers.getOrPut(sender) { MutableStateFlow(emptyList()) }.asStateFlow()

    fun dispatch(messages: List<ChatMessage>) {
        for (msg in messages) {
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
}
