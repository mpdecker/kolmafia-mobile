package net.sourceforge.kolmafia.chat

import kotlinx.serialization.json.*

object ChatParser {

    fun parse(json: String): ChatPollResponse {
        val root = Json.parseToJsonElement(json).jsonObject
        val lastTime = root["last"]?.jsonPrimitive?.content ?: "0"
        val delay = root["delay"]?.jsonPrimitive?.long ?: 3000L
        val msgs = root["msgs"]?.jsonArray ?: JsonArray(emptyList())

        val messages = msgs.mapNotNull { element ->
            val obj = element.jsonObject
            val type = obj["type"]?.jsonPrimitive?.content ?: return@mapNotNull null
            if (type == "system") return@mapNotNull null

            val who = obj["who"]?.jsonObject
            val sender = who?.get("name")?.jsonPrimitive?.content ?: "Unknown"
            val senderId = who?.get("id")?.jsonPrimitive?.content ?: "0"
            val channel = obj["channel"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() }
            val content = obj["msg"]?.jsonPrimitive?.content ?: ""
            val time = obj["time"]?.jsonPrimitive?.long ?: 0L

            ChatMessage(
                sender = sender,
                senderId = senderId,
                recipient = if (type == "private") sender else null,
                channel = channel,
                content = content,
                isAction = content.startsWith("/me"),
                epochSeconds = time
            )
        }

        return ChatPollResponse(messages = messages, lastTime = lastTime, delayMillis = delay)
    }
}
