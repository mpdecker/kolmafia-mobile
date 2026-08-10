package net.sourceforge.kolmafia.equipment

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.ModifierEntry

/**
 * Equipment with user-selectable modes (desktop [net.sourceforge.kolmafia.Modeable]).
 * Modifier rows live under [modifierType] in modifiers.txt, not under "Item".
 */
enum class Modeable(
    val itemId: Int,
    val itemName: String,
    val command: String,
    val statePref: String?,
    val modifierType: String,
    val slot: EquipmentSlot,
    val modes: Set<String>,
    val mustEquipAfterChange: Boolean = false,
) {
    BACKUPCAMERA(
        itemId = 10749,
        itemName = "backup camera",
        command = "backupcamera",
        statePref = "backupCameraMode",
        modifierType = "BackupCamera",
        slot = EquipmentSlot.ACC1,
        modes = setOf("ml", "meat", "init"),
        mustEquipAfterChange = true,
    ),
    EDPIECE(
        itemId = 8185,
        itemName = "The Crown of Ed the Undying",
        command = "edpiece",
        statePref = "edPiece",
        modifierType = "Edpiece",
        slot = EquipmentSlot.HAT,
        modes = setOf("bear", "owl", "puma", "hyena", "mouse", "weasel", "fish"),
        mustEquipAfterChange = false,
    ),
    PARKA(
        itemId = 10952,
        itemName = "Jurassic Parka",
        command = "parka",
        statePref = "parkaMode",
        modifierType = "JurassicParka",
        slot = EquipmentSlot.SHIRT,
        modes = setOf("ghostasaurus", "dilophosaur", "pterodactyl", "kachungasaur", "spikolodon"),
        mustEquipAfterChange = true,
    ),
    REPLICA_PARKA(
        itemId = 11249,
        itemName = "replica Jurassic Parka",
        command = "parka",
        statePref = "parkaMode",
        modifierType = "JurassicParka",
        slot = EquipmentSlot.SHIRT,
        modes = PARKA.modes,
        mustEquipAfterChange = true,
    ),
    RETROCAPE(
        itemId = 10647,
        itemName = "unwrapped knock-off retro superhero cape",
        command = "retrocape",
        statePref = null,
        modifierType = "RetroCape",
        slot = EquipmentSlot.CONTAINER,
        modes = setOf(
            "vampire hold", "vampire thrill", "vampire kiss", "vampire kill",
            "heck hold", "heck thrill", "heck kiss", "heck kill",
            "robot hold", "robot thrill", "robot kiss", "robot kill",
        ),
        mustEquipAfterChange = false,
    ),
    SNOWSUIT(
        itemId = 6150,
        itemName = "Snow Suit",
        command = "snowsuit",
        statePref = "snowsuit",
        modifierType = "Snowsuit",
        slot = EquipmentSlot.FAMILIAR,
        modes = setOf("goatee", "hat", "nose"),
        mustEquipAfterChange = false,
    ),
    UMBRELLA(
        itemId = 10899,
        itemName = "unbreakable umbrella",
        command = "umbrella",
        statePref = "umbrellaState",
        modifierType = "UnbreakableUmbrella",
        slot = EquipmentSlot.OFFHAND,
        modes = setOf(
            "broken",
            "forward-facing",
            "bucket style",
            "pitchfork style",
            "constantly twirling",
            "cocoon",
        ),
        mustEquipAfterChange = true,
    ),
    LED_CANDLE(
        itemId = 11336,
        itemName = "LED candle",
        command = "ledcandle",
        statePref = "ledCandleMode",
        modifierType = "LedCandle",
        slot = EquipmentSlot.FAMILIAR,
        modes = setOf("disco", "ultraviolet", "reading", "red light"),
        mustEquipAfterChange = true,
    ),
    ;

    fun modifiersForMode(mode: String): ModifierEntry? =
        ModifierDatabase.getModeable(modifierType, mode)

    fun normalizeMode(mode: String): String? {
        val trimmed = mode.trim()
        if (trimmed.isEmpty()) return null
        return modes.firstOrNull { it.equals(trimmed, ignoreCase = true) }
            ?: modes.firstOrNull { it.startsWith(trimmed, ignoreCase = true) }
    }

    companion object {
        private val byItemId = entries.associateBy { it.itemId }
        private val byItemName = entries.associateBy { it.itemName.lowercase() }
        private val byCommand = entries.groupBy { it.command.lowercase() }

        fun find(itemId: Int): Modeable? = byItemId[itemId]

        fun find(itemName: String): Modeable? =
            byItemName[itemName.trim().lowercase()]

        fun findByCommand(command: String): Modeable? =
            byCommand[command.trim().lowercase()]?.firstOrNull()

        fun all(): List<Modeable> = entries
    }
}
