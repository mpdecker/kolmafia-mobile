package net.sourceforge.kolmafia.character

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.EquipmentManager

/**
 * Desktop [ApiRequest.parseStatus] hub subset (Phases 2481–2495):
 * effects / lastadv / nested equipment / coolitems / zoot grafts / familiar feast+pic.
 */
object ApiStatusSync {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val coolItemPrefs = mapOf(
        "airport1" to ("sleazeAirportAlways" to "_sleazeAirportToday"),
        "airport2" to ("spookyAirportAlways" to "_spookyAirportToday"),
        "airport3" to ("stenchAirportAlways" to "_stenchAirportToday"),
        "airport4" to ("hotAirportAlways" to "_hotAirportToday"),
        "airport5" to ("coldAirportAlways" to "_coldAirportToday"),
        "gingerbreadcity" to ("gingerbreadCityAvailable" to "_gingerbreadCityToday"),
        "spacegate" to ("spacegateAlways" to "_spacegateToday"),
        "fantasyrealm" to ("frAlways" to "_frToday"),
        "piraterealm" to ("prAlways" to "_prToday"),
        "cyberrealm" to ("crAlways" to "_crToday"),
        "neverendingparty" to ("neverendingPartyAlways" to "_neverendingPartyToday"),
        "voterregistered" to ("voteAlways" to "_voteToday"),
        "boxingdaycare" to ("daycareOpen" to "_daycareToday"),
        "hascosmicball" to ("hasCosmicBowlingBall" to ""),
        "maydaykit" to ("hasMaydayContract" to ""),
        "autumnaton" to ("hasAutumnaton" to ""),
        "shrunkenhead" to ("hasShrunkenHead" to ""),
        "tunnelofloveiotm" to ("loveTunnelAvailable" to "_loveTunnelToday"),
        "ltt" to ("telegraphOfficeAvailable" to "_telegraphOfficeToday"),
        "floristfriar" to ("ownsFloristFriar" to ""),
    )

    fun parseStatus(
        responseText: String,
        character: KoLCharacter,
        preferences: Preferences? = null,
        effectManager: EffectManager? = null,
        equipmentManager: EquipmentManager? = null,
        familiarManager: FamiliarManager? = null,
    ): Boolean {
        val root = try {
            json.parseToJsonElement(responseText).jsonObject
        } catch (_: Exception) {
            return false
        }
        return parseStatus(root, character, preferences, effectManager, equipmentManager, familiarManager)
    }

    fun parseStatus(
        root: JsonObject,
        character: KoLCharacter,
        preferences: Preferences? = null,
        effectManager: EffectManager? = null,
        equipmentManager: EquipmentManager? = null,
        familiarManager: FamiliarManager? = null,
    ): Boolean {
        refreshEffects(root, effectManager)
        parseLastAdventure(root, preferences)
        parseNoncombatForcers(root, preferences)
        parseCoolItems(root, preferences)
        parseNestedEquipment(root, character, equipmentManager)
        parseFamiliarStatus(root, character, familiarManager)
        parseZootomistGrafts(root, preferences, character)
        CharpaneInteraction.applyInteraction(character, preferences)
        return true
    }

    fun refreshEffects(root: JsonObject, effectManager: EffectManager?) {
        effectManager ?: return
        val visible = mutableListOf<EffectData>()
        parseEffectMap(root["effects"], intrinsic = false, into = visible)
        parseEffectMap(root["intrinsics"], intrinsic = true, into = visible)
        effectManager.replaceEffectsForTest(visible.sortedBy { it.name })
    }

    private fun parseEffectMap(
        element: kotlinx.serialization.json.JsonElement?,
        intrinsic: Boolean,
        into: MutableList<EffectData>,
    ) {
        val obj = element as? JsonObject ?: return
        for ((descId, value) in obj) {
            val arr = value as? JsonArray ?: continue
            val name = arr.getOrNull(0)?.jsonPrimitive?.contentOrNull ?: continue
            val duration = if (intrinsic) {
                -1
            } else {
                arr.getOrNull(1)?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    ?: arr.getOrNull(1)?.jsonPrimitive?.intOrNull
                    ?: 0
            }
            val def = EffectDatabase.getByDescId(descId)
            val id = def?.id ?: descId.toIntOrNull() ?: 0
            into.add(EffectData(id = id, name = name.ifBlank { def?.name ?: "" }, duration = duration))
        }
    }

    fun parseLastAdventure(root: JsonObject, preferences: Preferences?) {
        preferences ?: return
        val lastadv = root["lastadv"] as? JsonObject ?: return
        val name = lastadv["name"]?.jsonPrimitive?.contentOrNull ?: return
        if (name.isBlank() || name == "The Naughty Sorceress' Tower") return
        preferences.setString("lastAdventure", name)
        lastadv["link"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let {
            preferences.setString("lastAdventureUrl", it)
        }
        lastadv["id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let {
            preferences.setString("lastAdventureId", it)
        }
        lastadv["container"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }?.let {
            preferences.setString("lastAdventureContainer", it)
        }
    }

    fun parseNoncombatForcers(root: JsonObject, preferences: Preferences?) {
        preferences ?: return
        val arr = root["noncomforcers"] as? JsonArray
        if (arr == null || arr.isEmpty()) {
            preferences.setBoolean(Preferences.Keys.NONCOMBAT_FORCER_ACTIVE, false)
            preferences.setString("noncombatForcers", "")
            return
        }
        val tokens = arr.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf { t -> t.isNotBlank() } }
        preferences.setBoolean(Preferences.Keys.NONCOMBAT_FORCER_ACTIVE, tokens.isNotEmpty())
        preferences.setString("noncombatForcers", tokens.joinToString("|"))
    }

    fun parseCoolItems(root: JsonObject, preferences: Preferences?) {
        preferences ?: return
        val cool = root["coolitems"]?.jsonPrimitive?.contentOrNull ?: return
        val owned = cool.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        for ((token, prefs) in coolItemPrefs) {
            val (alwaysPref, todayPref) = prefs
            val haveAccess = token in owned
            if (todayPref.isEmpty()) {
                preferences.setBoolean(alwaysPref, haveAccess)
            } else if (haveAccess) {
                val usedDayPass = preferences.getBoolean(todayPref, false)
                preferences.setBoolean(alwaysPref, !usedDayPass)
            } else {
                preferences.setBoolean(todayPref, false)
                preferences.setBoolean(alwaysPref, false)
            }
        }
    }

    fun parseNestedEquipment(
        root: JsonObject,
        character: KoLCharacter,
        equipmentManager: EquipmentManager?,
    ) {
        val equip = root["equipment"] as? JsonObject ?: return
        for ((slotName, value) in equip) {
            if (slotName.equals("fakehands", ignoreCase = true)) continue
            val slot = EquipmentSlot.fromApiKey(slotName)
                ?: when (slotName.lowercase()) {
                    "card sleeve", "cardsleeve" -> EquipmentSlot.CARDSLEEVE
                    else -> null
                }
                ?: continue
            val itemId = when (value) {
                is JsonPrimitive -> value.contentOrNull?.toIntOrNull() ?: value.intOrNull ?: 0
                else -> 0
            }
            if (equipmentManager != null) {
                equipmentManager.setEquipment(slot, itemId, swapInventory = false)
            } else {
                val name = if (itemId > 0) {
                    ItemDatabase.getItemName(itemId).ifBlank { "#$itemId" }
                } else {
                    ""
                }
                character.updateEquipment(slot, name)
            }
        }
        // Stickers / folders / eternitycod when present as top-level arrays
        applyIdArrayToSlots(root["stickers"], EquipmentSlot.STICKER_SLOTS, character, equipmentManager)
        applyFolderArray(root["folder_holder"], character, equipmentManager)
        applyIdArrayToSlots(root["eternitycod"], EquipmentSlot.CODPIECE_SLOTS, character, equipmentManager)
        val hats = root["hats"] as? JsonArray
        if (hats != null) {
            val ids = hats.mapNotNull {
                it.jsonPrimitive.contentOrNull?.toIntOrNull() ?: it.jsonPrimitive.intOrNull
            }
            character.setHatTrickHatIds(ids)
        }
    }

    private fun applyIdArrayToSlots(
        element: kotlinx.serialization.json.JsonElement?,
        slots: List<EquipmentSlot>,
        character: KoLCharacter,
        equipmentManager: EquipmentManager?,
    ) {
        val arr = element as? JsonArray ?: return
        slots.forEachIndexed { index, slot ->
            val itemId = arr.getOrNull(index)?.let {
                it.jsonPrimitive.contentOrNull?.toIntOrNull() ?: it.jsonPrimitive.intOrNull
            } ?: 0
            if (equipmentManager != null) {
                equipmentManager.setEquipment(slot, itemId, swapInventory = false)
            } else {
                val name = if (itemId > 0) ItemDatabase.getItemName(itemId).ifBlank { "#$itemId" } else ""
                character.updateEquipment(slot, name)
            }
        }
    }

    private fun applyFolderArray(
        element: kotlinx.serialization.json.JsonElement?,
        character: KoLCharacter,
        equipmentManager: EquipmentManager?,
    ) {
        val arr = element as? JsonArray ?: return
        EquipmentSlot.FOLDER_SLOTS.forEachIndexed { index, slot ->
            val folderIndex = arr.getOrNull(index)?.let {
                it.jsonPrimitive.contentOrNull?.toIntOrNull() ?: it.jsonPrimitive.intOrNull
            } ?: 0
            val itemId = if (folderIndex <= 0) {
                0
            } else {
                net.sourceforge.kolmafia.maximizer.MaximizerSubSlotItems.folderItemIdFromIndex(folderIndex) ?: 0
            }
            if (equipmentManager != null) {
                equipmentManager.setEquipment(slot, itemId, swapInventory = false)
            } else {
                val name = if (itemId > 0) ItemDatabase.getItemName(itemId).ifBlank { "#$itemId" } else ""
                character.updateEquipment(slot, name)
            }
        }
    }

    fun parseFamiliarStatus(
        root: JsonObject,
        character: KoLCharacter,
        familiarManager: FamiliarManager?,
    ) {
        val wellFed = root["familiar_wellfed"]?.jsonPrimitive?.let {
            it.contentOrNull == "1" || it.intOrNull == 1 || it.contentOrNull.equals("true", true)
        } ?: false
        val pic = root["familiarpic"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val famLevel = root["famlevel"]?.jsonPrimitive?.let {
            it.contentOrNull?.toIntOrNull() ?: it.intOrNull
        }
        val image = when {
            pic.isBlank() -> null
            pic.endsWith(".gif") || pic.endsWith(".png") -> pic
            else -> "$pic.gif"
        }
        character.setFamiliarPane(weight = famLevel, wellFed = wellFed, image = image)
        if (famLevel != null) {
            familiarManager?.applyActiveWeightXpLocally(famLevel, character.state.value.familiarExp)
        }
        familiarManager?.applyActiveFeastedLocally(wellFed)
    }

    fun parseZootomistGrafts(
        root: JsonObject,
        preferences: Preferences?,
        character: KoLCharacter,
    ) {
        preferences ?: return
        val grafts = root["grafts"] as? JsonObject ?: return
        if (character.state.value.ascensionPath != AscensionPath.Z_IS_FOR_ZOOTOMIST &&
            !character.state.value.challengePath.contains("Zootomist", ignoreCase = true)
        ) {
            // Still apply if grafts present (path may not be loaded yet)
        }
        fun graftInt(key: String): Int =
            grafts[key]?.jsonPrimitive?.let {
                it.contentOrNull?.toIntOrNull() ?: it.intOrNull
            } ?: 0
        preferences.setInt("zootGraftedHeadFamiliar", graftInt("1"))
        preferences.setInt("zootGraftedShoulderLeftFamiliar", graftInt("2"))
        preferences.setInt("zootGraftedShoulderRightFamiliar", graftInt("3"))
        preferences.setInt("zootGraftedHandLeftFamiliar", graftInt("4"))
        preferences.setInt("zootGraftedHandRightFamiliar", graftInt("5"))
        preferences.setInt("zootGraftedFootLeftFamiliar", graftInt("6"))
        preferences.setInt("zootGraftedFootRightFamiliar", graftInt("7"))
    }
}
