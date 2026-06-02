package net.sourceforge.kolmafia.chat

data class ChatMessage(
    val sender: String,
    val senderId: String,
    val recipient: String?,
    val channel: String?,       // null for private messages
    val content: String,
    val isAction: Boolean,
    val epochSeconds: Long
)

data class ChatPollResponse(
    val messages: List<ChatMessage>,
    val lastTime: String,
    val delayMillis: Long
)
