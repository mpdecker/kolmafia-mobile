package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AlliedRadioRequest

/** Orchestrates desktop-style `alliedradio` CLI / Allied Radio HTTP. */
open class AlliedRadioManager(
    private val preferences: Preferences,
    private val request: AlliedRadioRequest,
    private val inventoryManager: InventoryManager?,
    private val segmentSync: DemonInCombatNameSync?,
) {

    fun usesRemaining(inventoryState: InventoryState, characterState: CharacterState?): Int {
        var uses = handheldCount(inventoryState)
        if (backpackUsesRemaining(inventoryState, characterState) > 0) {
            uses += backpackUsesRemaining(inventoryState, characterState)
        }
        return uses
    }

    fun lacksRadioAndBackpack(inventoryState: InventoryState, characterState: CharacterState?): Boolean =
        handheldCount(inventoryState) == 0 && backpackUsesRemaining(inventoryState, characterState) <= 0

    fun backpackUsesRemaining(inventoryState: InventoryState, characterState: CharacterState?): Int {
        if (!hasBackpack(inventoryState, characterState)) return 0
        val used = preferences.getInt(Preferences.ALLIED_RADIO_DROPS_USED, 0)
        return (3 - used).coerceAtLeast(0)
    }

    fun handheldCount(inventoryState: InventoryState): Int =
        inventoryState.items[BreakfastItemIds.HANDHELD_ALLIED_RADIO_ID]?.quantity ?: 0

    fun hasBackpack(inventoryState: InventoryState, characterState: CharacterState?): Boolean {
        if (inventoryState.items.containsKey(BreakfastItemIds.ALLIED_RADIO_BACKPACK_ID)) return true
        val container = characterState?.equippedItem(EquipmentSlot.CONTAINER).orEmpty()
        return container.contains("Allied Radio Backpack", ignoreCase = true)
    }

    suspend fun run(parameters: String, inventoryState: InventoryState, characterState: CharacterState?, print: (String) -> Unit): Result<Unit> {
        val trimmed = parameters.trim()
        if (trimmed.isEmpty()) {
            print("Usage: alliedradio effect [ ellipsoidtine | intel | boon ] | item [ fuel | ordnance | rations | radio | chroner ] | misc [ sniper ] | request [ request ]")
            return Result.failure(IllegalArgumentException("missing subcommand"))
        }

        val split = trimmed.split(" ", limit = 2)
        val subcommand = split[0].lowercase()
        val args = split.getOrNull(1).orEmpty()

        val requestText = when (subcommand) {
            "effect" -> resolveEffectRequest(args, print) ?: return Result.failure(IllegalArgumentException("unknown effect"))
            "item" -> resolveItemRequest(args, print) ?: return Result.failure(IllegalArgumentException("unknown item"))
            "misc" -> resolveMiscRequest(args, print) ?: return Result.failure(IllegalArgumentException("unknown misc"))
            "request" -> args.trim().ifEmpty {
                print("Which request do you want?")
                return Result.failure(IllegalArgumentException("missing request"))
            }
            else -> {
                print("Usage: alliedradio effect [ ellipsoidtine | intel | boon ] | item [ fuel | ordnance | rations | radio | chroner ] | misc [ sniper ] | request [ request ]")
                return Result.failure(IllegalArgumentException("unknown subcommand"))
            }
        }

        return submitRequest(requestText, inventoryState, characterState, print)
    }

    suspend fun submitRequest(
        requestText: String,
        inventoryState: InventoryState,
        characterState: CharacterState?,
        print: (String) -> Unit,
    ): Result<Unit> {
        if (lacksRadioAndBackpack(inventoryState, characterState)) {
            print("You need a handheld radio, or a charged backpack.")
            return Result.failure(IllegalStateException("no radio"))
        }

        val handheld = backpackUsesRemaining(inventoryState, characterState) <= 0
        val result = request.requestRadioCall(requestText, handheld)
        return result.fold(
            onSuccess = { radioResult ->
                val parsed = AlliedRadioRequest.parsePostChoice(
                    radioResult.responseText,
                    radioResult.handheld,
                    requestText,
                    preferences,
                    segmentSync,
                )
                parsed.logMessages.forEach(print)
                if (radioResult.handheld) {
                    inventoryManager?.consumeItemLocally(BreakfastItemIds.HANDHELD_ALLIED_RADIO_ID, 1)
                }
                Result.success(Unit)
            },
            onFailure = { Result.failure(it) },
        )
    }

    internal fun resolveEffectRequest(args: String, print: (String) -> Unit): String? {
        val parameter = args.trim().lowercase()
        if (parameter.isEmpty()) {
            print("Which effect do you want?")
            return null
        }
        return when {
            parameter.startsWith("ell") -> "ellipsoidtine"
            parameter.contains("intel") || parameter.startsWith("mat") || parameter == "item" -> "materiel intel"
            parameter.contains("sun") || parameter.contains("boon") -> "wildsun boon"
            else -> {
                print("I don't understand what effect $parameter is.")
                null
            }
        }
    }

    internal fun resolveItemRequest(args: String, print: (String) -> Unit): String? {
        val parameter = args.trim().lowercase()
        if (parameter.isEmpty()) {
            print("Which item do you want?")
            return null
        }
        return when {
            parameter.contains("fuel") || parameter == "booze" -> "fuel"
            parameter == "ordnance" || parameter.contains("grenade") -> "ordnance"
            parameter.contains("ration") || parameter == "food" -> "rations"
            parameter.contains("radio") -> "radio"
            parameter.contains("chroner") || parameter.contains("salary") -> "salary"
            else -> {
                print("I don't understand what item $parameter is.")
                null
            }
        }
    }

    internal fun resolveMiscRequest(args: String, print: (String) -> Unit): String? {
        val parameter = args.trim().lowercase()
        if (parameter.isEmpty()) {
            print("Which miscellaneous supplies do you want?")
            return null
        }
        return when {
            parameter.startsWith("sniper") || parameter.contains("support") -> "sniper support"
            else -> {
                print("I don't understand what supplies $parameter is.")
                null
            }
        }
    }
}
