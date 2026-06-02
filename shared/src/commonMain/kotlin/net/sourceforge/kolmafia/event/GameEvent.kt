package net.sourceforge.kolmafia.event

import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureResult
import net.sourceforge.kolmafia.adventure.StopReason
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.inventory.InventoryItem

sealed class GameEvent {
    // Adventure
    data class TurnConsumed(val location: AdventureLocation, val result: AdventureResult) : GameEvent()
    data class CombatFinished(val won: Boolean, val monster: String) : GameEvent()
    data class ChoiceResolved(val choiceId: Int, val option: Int) : GameEvent()
    data class AdventureLoopStopped(val reason: StopReason) : GameEvent()
    // Inventory
    data class ItemObtained(val item: InventoryItem) : GameEvent()
    data class ItemConsumed(val itemId: Int, val quantity: Int) : GameEvent()
    data class ItemEquipped(val item: InventoryItem, val slot: String) : GameEvent()
    data class ItemDiscarded(val itemId: Int, val quantity: Int) : GameEvent()
    data class ItemCrafted(val resultItem: InventoryItem) : GameEvent()
    data class MallPurchase(val item: InventoryItem, val meatSpent: Int) : GameEvent()
    // Familiar
    data class FamiliarSwitched(val familiar: FamiliarData) : GameEvent()
    data class FamiliarEquipped(val familiar: FamiliarData, val item: InventoryItem) : GameEvent()
    data class FamiliarHatched(val familiar: FamiliarData) : GameEvent()
    // Skills + Effects (Phase 3)
    data class SkillCast(val skillId: Int, val skillName: String, val quantity: Int) : GameEvent()
    object EffectsRefreshed : GameEvent()
}
