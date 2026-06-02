package net.sourceforge.kolmafia.chat

import kotlinx.serialization.json.*

object ChatParser {

    fun parse(json: String): ChatPollResponse {
        return try {
            val root = Json.parseToJsonElement(json).jsonObject
            val lastTime = root["last"]?.jsonPrimitive?.content ?: "0"
            val delay = root["delay"]?.jsonPrimitive?.long ?: 3000L
            val msgs = root["msgs"]?.jsonArray ?: JsonArray(emptyList())

            val messages = msgs.mapNotNull { element ->
                val obj = element.jsonObject
                val type = obj["type"]?.jsonPrimitive?.content ?: return@mapNotNull null
                if (type == "system" || type == "event") return@mapNotNull null

                val who = obj["who"]?.jsonObject
                val sender = who?.get("name")?.jsonPrimitive?.content ?: "Unknown"
                val senderId = who?.get("id")?.jsonPrimitive?.content ?: "0"
                val channel = obj["channel"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() }
                val content = obj["msg"]?.jsonPrimitive?.content ?: ""
                val time = obj["time"]?.jsonPrimitive?.long ?: 0L
                val format = obj["format"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0

                val forObj = obj["for"]?.jsonObject
                val recipient = forObj?.get("name")?.jsonPrimitive?.content

                ChatMessage(
                    sender = sender,
                    senderId = senderId,
                    recipient = recipient,
                    channel = channel,
                    content = content,
                    isAction = format == 1,
                    epochSeconds = time
                )
            }

            ChatPollResponse(messages = messages, lastTime = lastTime, delayMillis = delay)
        } catch (_: Exception) {
            ChatPollResponse(messages = emptyList(), lastTime = "0", delayMillis = 3000L)
        }
    }
}
