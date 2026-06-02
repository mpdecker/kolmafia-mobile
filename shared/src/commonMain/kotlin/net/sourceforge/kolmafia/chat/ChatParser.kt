package net.sourceforge.kolmafia.chat

import kotlinx.serialization.json.*

object ChatParser {

    fun parse(json: String): ChatPollResponse {
        val root = try {
            Json.parseToJsonElement(json).jsonObject
        } catch (_: Exception) {
            return ChatPollResponse(messages = emptyList(), lastTime = "0", delayMillis = 3000L)
        }

        val lastTime = root["last"]?.jsonPrimitive?.content ?: "0"
        val delay = root["delay"]?.jsonPrimitive?.long ?: 3000L
        val msgs = root["msgs"]?.jsonArray ?: JsonArray(emptyList())

        val messages = msgs.mapNotNull { element ->
            runCatching { parseMessage(element) }.getOrNull()
        }

        return ChatPollResponse(messages = messages, lastTime = lastTime, delayMillis = delay)
    }

    private fun parseMessage(element: JsonElement): ChatMessage? {
        val obj = element.jsonObject
        val type = obj["type"]?.jsonPrimitive?.content ?: return null
        if (type == "system" || type == "event") return null

        val who = obj["who"]?.jsonObject
        val sender = who?.get("name")?.jsonPrimitive?.content ?: "Unknown"
        val senderId = who?.get("id")?.jsonPrimitive?.content ?: "0"
        val channel = obj["channel"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() }
        val content = obj["msg"]?.jsonPrimitive?.content ?: ""
        val time = obj["time"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
        val format = obj["format"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val forObj = obj["for"]?.jsonObject
        val recipient = forObj?.get("name")?.jsonPrimitive?.content

        return ChatMessage(
            sender = sender,
            senderId = senderId,
            recipient = recipient,
            channel = channel,
            content = content,
            isAction = format == 1,
            epochSeconds = time
        )
    }
}
