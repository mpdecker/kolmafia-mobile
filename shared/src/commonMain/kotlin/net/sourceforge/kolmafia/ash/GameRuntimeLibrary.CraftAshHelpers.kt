package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.item.CreatableTurns
import net.sourceforge.kolmafia.item.FreeCraftingTurns

internal fun GameRuntimeLibrary.craftCharacterState(): CharacterState =
    character?.state?.value ?: CharacterState()

internal fun GameRuntimeLibrary.craftSkills() =
    skillManager?.state?.value?.skills ?: emptyList()

internal fun GameRuntimeLibrary.craftEffects() =
    effectManager?.state?.value?.effects ?: emptyList()

internal fun GameRuntimeLibrary.inventoryItemCount(itemId: Int): Int =
    inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

internal fun GameRuntimeLibrary.craftAccessibleCount(itemId: Int): Int {
    val name = ItemDatabase.getById(itemId)?.name ?: return 0
    return kotlinx.coroutines.runBlocking { physicalAccessibleCount(itemId, name) }
}

internal fun GameRuntimeLibrary.isCraftPermitted(itemId: Int): Boolean {
    val itemName = ItemDatabase.getById(itemId)?.name ?: return false
    val concoction = net.sourceforge.kolmafia.data.ConcoctionDatabase.getByResult(itemName)
        ?: return false
    return ConcoctionPermitted.isPermittedMethod(
        concoction,
        craftCharacterState(),
        craftSkills(),
        accessibleCount = { ingId -> craftAccessibleCount(ingId) },
        prefs = preferences,
    )
}

internal fun GameRuntimeLibrary.craftItemCount(itemId: Int): Int {
    var count = inventoryItemCount(itemId)
    val itemName = ItemDatabase.getById(itemId)?.name ?: return count
    val equipment = character?.state?.value?.equipment
    if (equipment != null) {
        count += OutfitManager.equippedCount(itemName, equipment)
    }
    return count
}

internal fun GameRuntimeLibrary.buildFreeCraftingContext(): FreeCraftingTurns.Context {
    return FreeCraftingTurns.Context(
        preferences = preferences,
        state = craftCharacterState(),
        skills = craftSkills(),
        effects = craftEffects(),
        itemCount = { itemId -> craftItemCount(itemId) },
        ownedFamiliar = { name ->
            familiarManager?.state?.value?.ownedFamiliars?.any {
                it.name.equals(name, ignoreCase = true)
            } == true
        },
    )
}

internal fun GameRuntimeLibrary.creatableTurnsFor(
    itemId: Int,
    count: Int,
    considerFreeCrafting: Boolean = false,
): Long {
    val initial = inventoryItemCount(itemId)
    return CreatableTurns.adventuresNeeded(
        itemId = itemId,
        quantityNeeded = initial + count,
        context = CreatableTurns.Context(
            inventoryCount = { id -> inventoryItemCount(id) },
            isPermitted = { id -> isCraftPermitted(id) },
            considerFreeCrafting = considerFreeCrafting,
            freeCrafting = buildFreeCraftingContext(),
        ),
    ).toLong()
}
