package net.sourceforge.kolmafia.request

/** Desktop [net.sourceforge.kolmafia.request.InternalChatRequest] chat poll alias. */
object InternalChatRequest {
    fun pollUrl(lastSeen: Long, tabbedChat: Boolean, afk: Boolean): String =
        ChatRequest.pollUrl(lastSeen, tabbedChat, afk)

    fun registerRequest(url: String): Boolean = ChatRequest.registerRequest(url)
}
