package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.shop.CoinmasterSyncedTokenCount
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionCreationCost
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionPermitted
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.familiar.FamiliarUsability
import net.sourceforge.kolmafia.familiar.FamiliarState
import net.sourceforge.kolmafia.item.CreatableTurns
import net.sourceforge.kolmafia.item.FreeCraftingTurns
import net.sourceforge.kolmafia.modifiers.VykeaCompanionData

internal fun GameRuntimeLibrary.craftCharacterState(): CharacterState =
    character?.state?.value ?: CharacterState()

internal fun GameRuntimeLibrary.craftSkills() =
    skillManager?.state?.value?.skills ?: emptyList()

internal fun GameRuntimeLibrary.craftEffects() =
    effectManager?.state?.value?.effects ?: emptyList()

internal fun GameRuntimeLibrary.craftFamiliarUsable(familiarId: Int): Boolean {
    val race = when (familiarId) {
        162 -> "Reagnimated Gnome"
        else -> return false
    }
    val familiarState = familiarManager?.state?.value ?: FamiliarState()
    return FamiliarUsability.usableByRace(
        familiarState,
        race,
        craftCharacterState(),
        preferences,
    ) != null
}

internal fun GameRuntimeLibrary.hasActiveEffect(effectId: Int): Boolean =
    effectManager?.state?.value?.effects?.any { it.id == effectId } == true

internal fun GameRuntimeLibrary.npcFamiliarUsable(familiarId: Int): Boolean {
    val race = when (familiarId) {
        206 -> "Trick-or-Treating Tot"
        else -> return false
    }
    val familiarState = familiarManager?.state?.value ?: FamiliarState()
    return FamiliarUsability.usableByRace(
        familiarState,
        race,
        craftCharacterState(),
        preferences,
    ) != null
}

internal fun GameRuntimeLibrary.inventoryItemCount(itemId: Int): Int =
    inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

internal fun GameRuntimeLibrary.craftAccessibleCount(itemId: Int): Int {
    val name = ItemDatabase.getById(itemId)?.name ?: return 0
    val physical = kotlinx.coroutines.runBlocking { physicalAccessibleCount(itemId, name) }
    return CoinmasterSyncedTokenCount.accessibleCount(itemId, preferences, physical)
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
        familiarUsable = { familiarId -> craftFamiliarUsable(familiarId) },
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

internal fun GameRuntimeLibrary.concoctionPriceForConcoction(concoction: ConcoctionData): Long {
    var cost = 0L
    for (ingredient in concoction.ingredients) {
        val ingId = ItemDatabase.getByName(ingredient.name)?.id ?: continue
        if (ingId < 0) continue
        val mallPrice = kotlinx.coroutines.runBlocking {
            mallManager?.cheapestPrice(ingredient.name) ?: -1L
        }
        val unitPrice = if (mallPrice < 0) Int.MAX_VALUE.toLong() else mallPrice
        cost += unitPrice * ingredient.quantity
    }
    cost += ConcoctionCreationCost.creationCost(concoction.methods)
    return minOf(cost, Int.MAX_VALUE.toLong())
}

internal fun GameRuntimeLibrary.concoctionPriceForItem(itemId: Int): Long {
    val itemName = ItemDatabase.getById(itemId)?.name ?: return 0L
    val concoction = ConcoctionDatabase.getByResult(itemName) ?: return 0L
    return concoctionPriceForConcoction(concoction)
}

internal fun GameRuntimeLibrary.concoctionPriceForVykea(vykeaString: String): Long {
    val companion = VykeaCompanionData.companionFor(vykeaString) ?: return 0L
    val resultName = VykeaCompanionData.concoctionResultName(companion)
    val concoction = ConcoctionDatabase.getByResult(resultName) ?: return 0L
    return concoctionPriceForConcoction(concoction)
}
