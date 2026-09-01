package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.ModeableChoiceSync

/** HTTP mode switching for modeable equipment (desktop ModeCommand run paths). */
open class ModeableRequest(
    private val client: HttpClient,
    private val choiceRequest: ChoiceRequest,
    private val equipmentRequest: EquipmentRequest? = null,
    private val character: KoLCharacter? = null,
    private val preferences: Preferences? = null,
    private val inventoryManager: InventoryManager? = null,
    private val equipmentManager: EquipmentManager? = null,
) {
    open suspend fun setMode(modeable: Modeable, mode: String): Result<Unit> {
        val resolvedMode = when (modeable) {
            Modeable.PARKA, Modeable.REPLICA_PARKA -> normalizeParkaParameter(mode)
            Modeable.UMBRELLA -> normalizeUmbrellaParameter(mode)
            Modeable.LED_CANDLE -> normalizeLedCandleParameter(mode)
            else -> mode.trim()
        }
        val normalized = modeable.normalizeMode(resolvedMode)
            ?: return Result.failure(IllegalArgumentException("Unknown mode: $mode"))
        return try {
            when (modeable) {
                Modeable.UMBRELLA -> setUmbrella(normalized)
                Modeable.PARKA, Modeable.REPLICA_PARKA -> setParka(normalized)
                Modeable.BACKUPCAMERA -> setBackupCamera(normalized)
                Modeable.EDPIECE -> setEdpiece(normalized)
                Modeable.RETROCAPE -> setRetroCape(normalized)
                Modeable.SNOWSUIT -> setSnowsuit(normalized)
                Modeable.LED_CANDLE -> setLedCandle(normalized)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun visitInventoryAction(action: String): Result<String> = try {
        val body = client.get("$KOL_BASE_URL/inventory.php?action=$action").bodyAsText()
        Result.success(body)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun choose(
        choiceId: Int,
        option: Int,
        extraFormFields: Map<String, String> = emptyMap(),
    ): Result<Pair<String, String>> {
        val result = choiceRequest.choose(choiceId, option, extraFormFields)
        result.onSuccess { (body, url) ->
            ModeableChoiceSync.applyFromChoiceUrl(url, body, preferences)
        }
        return result
    }

    private suspend fun setUmbrella(mode: String): Result<Unit> {
        if (missingOwnership(Modeable.UMBRELLA)) {
            return Result.failure(IllegalStateException("You need an Unbreakable Umbrella first."))
        }
        val option = UMBRELLA_OPTIONS[mode.lowercase()]
            ?: return Result.failure(IllegalArgumentException("Unknown umbrella mode: $mode"))
        visitInventoryAction("useumbrella").getOrElse { return Result.failure(it) }
        val (html, _) = choiceRequest.choose(1466, option).getOrElse { return Result.failure(it) }
        if (!ModeableChoiceSync.applyUmbrellaMode(html, mode, preferences)) {
            return Result.failure(IllegalStateException("Umbrella mode change was not successful."))
        }
        return Result.success(Unit)
    }

    private suspend fun setParka(mode: String): Result<Unit> {
        val option = PARKA_OPTIONS[mode.lowercase()]
            ?: return Result.failure(IllegalArgumentException("Unknown parka mode: $mode"))
        visitInventoryAction("jparka").getOrElse { return Result.failure(it) }
        choose(1481, option).getOrElse { return Result.failure(it) }
        choose(1481, 6).onFailure { /* best-effort exit */ }
        ModeableChoiceSync.writeModePref(preferences, Modeable.PARKA, mode)
        return Result.success(Unit)
    }

    private suspend fun setBackupCamera(mode: String): Result<Unit> {
        val option = BACKUP_CAMERA_OPTIONS[mode.lowercase()]
            ?: return Result.failure(IllegalArgumentException("Unknown backup camera mode: $mode"))
        visitInventoryAction("bcmode").getOrElse { return Result.failure(it) }
        choose(1449, option).getOrElse { return Result.failure(it) }
        choose(1449, 6).onFailure { /* best-effort exit */ }
        ModeableChoiceSync.writeModePref(preferences, Modeable.BACKUPCAMERA, mode)
        return Result.success(Unit)
    }

    private suspend fun setEdpiece(mode: String): Result<Unit> {
        val option = EDPIECE_OPTIONS[mode.lowercase()]
            ?: return Result.failure(IllegalArgumentException("Unknown edpiece mode: $mode"))
        ensureEquipped(Modeable.EDPIECE).getOrElse { return Result.failure(it) }
        visitInventoryAction("activateedhat").getOrElse { return Result.failure(it) }
        choose(1063, option).getOrElse { return Result.failure(it) }
        ModeableChoiceSync.writeModePref(preferences, Modeable.EDPIECE, mode)
        return Result.success(Unit)
    }

    private suspend fun setRetroCape(mode: String): Result<Unit> {
        val parts = mode.trim().split(Regex("\\s+"), limit = 2)
        if (parts.size != 2) {
            return Result.failure(IllegalArgumentException("Retro cape mode must be '<hero> <wash>'"))
        }
        val washOption = RETRO_WASH_OPTIONS[parts[1].lowercase()]
            ?: return Result.failure(IllegalArgumentException("Unknown retro cape wash: ${parts[1]}"))
        val heroOption = RETRO_HERO_OPTIONS[parts[0].lowercase()]
            ?: return Result.failure(IllegalArgumentException("Unknown retro cape hero: ${parts[0]}"))
        ensureEquipped(Modeable.RETROCAPE).getOrElse { return Result.failure(it) }
        visitInventoryAction("hmtmkmkm").getOrElse { return Result.failure(it) }
        choose(1437, washOption).getOrElse { return Result.failure(it) }
        choose(1437, 1).getOrElse { return Result.failure(it) }
        choose(1438, heroOption).getOrElse { return Result.failure(it) }
        choose(1438, 4).getOrElse { return Result.failure(it) }
        choose(1437, 6).onFailure { /* best-effort exit */ }
        ModeableChoiceSync.writeModePref(preferences, Modeable.RETROCAPE, mode)
        return Result.success(Unit)
    }

    private suspend fun setSnowsuit(mode: String): Result<Unit> {
        val option = SNOWSUIT_OPTIONS[mode.lowercase()]
            ?: return Result.failure(IllegalArgumentException("Unknown snowsuit mode: $mode"))
        ensureEquipped(Modeable.SNOWSUIT).getOrElse { return Result.failure(it) }
        visitInventoryAction("decorate").getOrElse { return Result.failure(it) }
        choose(640, option).getOrElse { return Result.failure(it) }
        ModeableChoiceSync.writeModePref(preferences, Modeable.SNOWSUIT, mode)
        return Result.success(Unit)
    }

    private suspend fun setLedCandle(mode: String): Result<Unit> {
        val option = LED_CANDLE_OPTIONS[mode.lowercase()]
            ?: return Result.failure(IllegalArgumentException("Unknown LED candle mode: $mode"))
        visitInventoryAction("tweakjill").getOrElse { return Result.failure(it) }
        choose(1509, option).getOrElse { return Result.failure(it) }
        ModeableChoiceSync.writeModePref(preferences, Modeable.LED_CANDLE, mode)
        return Result.success(Unit)
    }

    private suspend fun ensureEquipped(modeable: Modeable): Result<Unit> {
        val slot = modeable.slot
        val itemName = modeable.itemName
        val equipped = character?.state?.value?.equipment?.get(slot)
        if (equipped.equals(itemName, ignoreCase = true)) {
            return Result.success(Unit)
        }
        val request = equipmentRequest
            ?: return Result.failure(IllegalStateException("EquipmentRequest unavailable"))
        return request.equipItem(modeable.itemId, slot)
    }

    private fun missingOwnership(modeable: Modeable): Boolean {
        val canCheck = inventoryManager != null || equipmentManager != null || character != null
        if (!canCheck) return false
        if ((inventoryManager?.getCount(modeable.itemId) ?: 0) > 0) return false
        if (equipmentManager?.hasEquipped(modeable.itemId) == true) return false
        val itemName = modeable.itemName
        return character?.state?.value?.equipment?.values
            ?.any { it.equals(itemName, ignoreCase = true) } != true
    }

    companion object {
        private val UMBRELLA_OPTIONS = mapOf(
            "broken" to 1,
            "forward-facing" to 2,
            "bucket style" to 3,
            "pitchfork style" to 4,
            "constantly twirling" to 5,
            "cocoon" to 6,
        )

        private val PARKA_OPTIONS = mapOf(
            "kachungasaur" to 1,
            "dilophosaur" to 2,
            "spikolodon" to 3,
            "ghostasaurus" to 4,
            "pterodactyl" to 5,
        )

        private val BACKUP_CAMERA_OPTIONS = mapOf(
            "ml" to 1,
            "meat" to 2,
            "init" to 3,
        )

        private val EDPIECE_OPTIONS = mapOf(
            "bear" to 1,
            "owl" to 2,
            "puma" to 3,
            "hyena" to 4,
            "mouse" to 5,
            "weasel" to 6,
            "fish" to 7,
        )

        private val RETRO_HERO_OPTIONS = mapOf(
            "vampire" to 1,
            "heck" to 2,
            "robot" to 3,
        )

        private val RETRO_WASH_OPTIONS = mapOf(
            "hold" to 2,
            "thrill" to 3,
            "kiss" to 4,
            "kill" to 5,
        )

        private val SNOWSUIT_OPTIONS = mapOf(
            "nose" to 3,
            "goatee" to 4,
            "hat" to 5,
        )

        private val LED_CANDLE_OPTIONS = mapOf(
            "disco" to 1,
            "ultraviolet" to 2,
            "reading" to 3,
            "red light" to 4,
        )

        /** Resolve CLI parameter to a modeable (parka matches both parka entries). */
        fun modeableForCommand(command: String): Modeable? = Modeable.findByCommand(command)

        /** Normalize parka CLI aliases (desktop JurassicParkaCommand.ALIASES). */
        fun normalizeParkaParameter(parameter: String): String {
            val trimmed = parameter.trim().lowercase()
            return PARKA_ALIASES[trimmed] ?: trimmed
        }

        private val PARKA_ALIASES = mapOf(
            "spooky" to "ghostasaurus",
            "mp" to "ghostasaurus",
            "dr" to "ghostasaurus",
            "stench" to "dilophosaur",
            "acid" to "dilophosaur",
            "hot" to "pterodactyl",
            "init" to "pterodactyl",
            "nc" to "pterodactyl",
            "cold" to "kachungasaur",
            "hp" to "kachungasaur",
            "meat" to "kachungasaur",
            "sleaze" to "spikolodon",
            "ml" to "spikolodon",
            "spikes" to "spikolodon",
        )

        /** Normalize umbrella CLI shorthands (desktop UmbrellaMode.findByShortHand). */
        fun normalizeUmbrellaParameter(parameter: String): String {
            val trimmed = parameter.trim().lowercase()
            return when (trimmed) {
                "ml" -> "broken"
                "dr" -> "forward-facing"
                "item" -> "bucket style"
                "weapon" -> "pitchfork style"
                "spell" -> "constantly twirling"
                "nc" -> "cocoon"
                "forward" -> "forward-facing"
                "bucket" -> "bucket style"
                "pitchfork" -> "pitchfork style"
                "twirling" -> "constantly twirling"
                else -> trimmed
            }
        }

        /** Normalize LED candle CLI shorthands. */
        fun normalizeLedCandleParameter(parameter: String): String {
            val trimmed = parameter.trim().lowercase()
            return when {
                trimmed == "item" -> "disco"
                trimmed.startsWith("ultra") || trimmed == "meat" -> "ultraviolet"
                trimmed == "stats" -> "reading"
                trimmed.startsWith("red") || trimmed.startsWith("attack") -> "red light"
                else -> trimmed
            }
        }
    }
}
