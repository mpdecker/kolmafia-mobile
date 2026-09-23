package net.sourceforge.kolmafia.webui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop RelayRequest.handleJsonApi — properties + limited ASH function calls.
 * Full JSONValueConverter identity/proxy graph remains deferred.
 */
object RelayJsonApi {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun handle(
        body: String?,
        library: GameRuntimeLibrary?,
        preferences: Preferences?,
    ): Pair<Int, String> {
        if (body.isNullOrBlank()) {
            return 400 to """{"error":"Missing body"}"""
        }
        val root = try {
            json.parseToJsonElement(body).jsonObject
        } catch (_: Exception) {
            return 400 to """{"error":"Invalid JSON object in request."}"""
        }

        val result = mutableMapOf<String, JsonElement>()

        root["properties"]?.let { propsEl ->
            val arr = propsEl as? JsonArray
                ?: return 400 to """{"error":"Invalid property names"}"""
            val values = buildJsonArray {
                for (el in arr) {
                    val name = (el as? JsonPrimitive)?.contentOrNull
                        ?: return 400 to """{"error":"Invalid property names"}"""
                    add(JsonPrimitive(preferences?.getString(name, "").orEmpty()))
                }
            }
            result["properties"] = values
        }

        root["functions"]?.let { funcsEl ->
            val arr = funcsEl as? JsonArray
                ?: return 400 to """{"error":"Invalid function calls"}"""
            val out = buildJsonArray {
                for (el in arr) {
                    val obj = el as? JsonObject
                        ?: return 400 to """{"error":"Invalid function calls"}"""
                    val name = obj["name"]?.jsonPrimitive?.contentOrNull
                        ?: return 400 to """{"error":"Invalid function calls"}"""
                    val args = obj["args"] as? JsonArray
                        ?: return 400 to """{"error":"Invalid function calls"}"""
                    if (args.any { it is JsonNull }) {
                        return 400 to """{"error":"Invalid function calls"}"""
                    }
                    when {
                        name == "identity" && args.size == 1 -> add(args[0])
                        else -> add(invokeSimple(name, args, library, preferences))
                    }
                }
            }
            result["functions"] = out
        }

        return 200 to json.encodeToString(JsonObject.serializer(), JsonObject(result))
    }

    private fun invokeSimple(
        name: String,
        args: JsonArray,
        library: GameRuntimeLibrary?,
        preferences: Preferences?,
    ): JsonPrimitive {
        val underscore = name.replace('-', '_')
        return when (underscore.lowercase()) {
            "get_property", "getproperty" -> {
                val key = args.firstOrNull()?.jsonPrimitive?.contentOrNull.orEmpty()
                JsonPrimitive(preferences?.getString(key, "").orEmpty())
            }
            "my_name", "myname" ->
                JsonPrimitive(library?.character?.state?.value?.name.orEmpty())
            "my_hash", "myhash" ->
                JsonPrimitive(preferences?.getString("pwdHash", "").orEmpty())
            "get_revision", "getrevision" ->
                JsonPrimitive(GameRuntimeLibrary.REVISION)
            "boolean_modifier", "booleanmodifier" -> JsonPrimitive(false)
            else -> JsonPrimitive("unsupported:$name")
        }
    }
}
