package net.sourceforge.kolmafia.request

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.sourceforge.kolmafia.character.ApiStatusSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.EquipmentManager

/**
 * Desktop [net.sourceforge.kolmafia.request.ApiRequest] — visit_url / refresh router for
 * `api.php?what=status|inventory|closet|storage` (Phases 6071–6085).
 */
object ApiRequest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val WHAT = Regex("""(?:^|[?&])what=([^&]*)""", RegexOption.IGNORE_CASE)

    fun registerRequest(url: String): Boolean = url.contains("api.php", ignoreCase = true)

    fun whatFromUrl(url: String): String =
        WHAT.find(url)?.groupValues?.getOrNull(1)?.lowercase().orEmpty()

    /**
     * Strip leading non-JSON noise (desktop [ApiRequest.getJSONString]).
     */
    fun jsonObjectFromResponse(responseText: String): JsonObject? {
        val start = responseText.indexOf('{')
        if (start < 0) return null
        val slice = if (start == 0) responseText else responseText.substring(start)
        return try {
            json.parseToJsonElement(slice).jsonObject
        } catch (_: Exception) {
            null
        }
    }

    fun parseIdQtyMap(responseText: String): Map<Int, Int> {
        val root = jsonObjectFromResponse(responseText) ?: return emptyMap()
        return root.entries.mapNotNull { (key, value) ->
            val id = key.toIntOrNull() ?: return@mapNotNull null
            val qty = when (value) {
                is JsonPrimitive -> value.contentOrNull?.toIntOrNull() ?: value.intOrNull
                else -> null
            } ?: return@mapNotNull null
            if (qty <= 0) null else id to qty
        }.toMap()
    }

    fun parseResponse(
        url: String,
        responseText: String,
        character: KoLCharacter? = null,
        preferences: Preferences? = null,
        effectManager: EffectManager? = null,
        equipmentManager: EquipmentManager? = null,
        familiarManager: FamiliarManager? = null,
        inventoryManager: InventoryManager? = null,
    ): Boolean {
        if (!url.contains("api.php", ignoreCase = true)) return false
        return when (whatFromUrl(url)) {
            "status", "" -> parseStatus(
                responseText,
                character,
                preferences,
                effectManager,
                equipmentManager,
                familiarManager,
            )
            "inventory" -> parseInventory(responseText, inventoryManager, preferences)
            "closet" -> parseCloset(responseText, preferences)
            "storage" -> parseStorage(responseText, character, preferences)
            else -> false
        }
    }

    fun parseStatus(
        responseText: String,
        character: KoLCharacter?,
        preferences: Preferences?,
        effectManager: EffectManager?,
        equipmentManager: EquipmentManager?,
        familiarManager: FamiliarManager?,
    ): Boolean {
        SpelunkyRequest.parseStatus(responseText, preferences)
        val char = character ?: return false
        return ApiStatusSync.parseStatus(
            responseText = responseText,
            character = char,
            preferences = preferences,
            effectManager = effectManager,
            equipmentManager = equipmentManager,
            familiarManager = familiarManager,
        )
    }

    fun parseInventory(
        responseText: String,
        inventoryManager: InventoryManager?,
        preferences: Preferences?,
    ): Boolean {
        val raw = parseIdQtyMap(responseText)
        if (raw.isEmpty() && !responseText.contains('{')) return false
        inventoryManager?.let { inv ->
            inv.applyParsedInventory(inv.parseInventory(raw.mapKeys { it.key.toString() }))
        }
        return true
    }

    fun parseCloset(responseText: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        CollectionCacheSync.saveCloset(prefs, parseIdQtyMap(responseText))
        return true
    }

    fun parseStorage(
        responseText: String,
        character: KoLCharacter?,
        preferences: Preferences?,
    ): Boolean {
        val prefs = preferences ?: return false
        val raw = parseIdQtyMap(responseText)
        val classified = StoragePullRules.classifyContents(
            raw,
            character?.state?.value,
            prefs,
        )
        CollectionCacheSync.saveStorage(prefs, classified.storage, classified.freepulls)
        return true
    }
}
