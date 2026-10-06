package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConditionalExtraAdventureItems
import net.sourceforge.kolmafia.data.ConsumableData
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.HolidayCalendar
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.isLasagna
import net.sourceforge.kolmafia.data.isMartini
import net.sourceforge.kolmafia.data.isWine
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.EquipmentManager

/**
 * Desktop EatItemRequest / DrinkItemRequest headless preflight automation:
 * auto-cast Ode, autoGarish, autoTuxedo, autoPinkyRing.
 * GUI confirm nags (PvP / overdrink / carbo) are intentionally excluded.
 */
object ConsumeAutomation {
    const val ODE_TO_BOOZE = 6014
    const val GARISH_EFFECT_ID = 918
    const val FIELD_GAR_POTION = ConditionalExtraAdventureItems.FIELD_GAR_POTION
    const val TUXEDO_SHIRT = ConditionalExtraAdventureItems.TUXEDO_SHIRT
    const val MAFIA_PINKY_RING = ConditionalExtraAdventureItems.MAFIA_PINKY_RING

    /** Optional DI hooks (wired from GameRuntimeLibrary / tests). */
    var castOde: (suspend (Int) -> Boolean)? = null
    var useItem: (suspend (Int) -> Boolean)? = null
    var equipItem: (suspend (Int, EquipmentSlot) -> Boolean)? = null
    var retrieveItem: (suspend (Int, Int) -> Int)? = null
    var hasSkill: ((Int) -> Boolean)? = null
    var hasAccordion: (() -> Boolean)? = null
    var canInteract: (() -> Boolean)? = null
    var currentMp: (() -> Long)? = null
    var odeMpCost: (() -> Long)? = null
    var odeTurns: (() -> Int)? = null
    var hasGarish: (() -> Boolean)? = null

    fun resetForTest() {
        castOde = null
        useItem = null
        equipItem = null
        retrieveItem = null
        hasSkill = null
        hasAccordion = null
        canInteract = null
        currentMp = null
        odeMpCost = null
        odeTurns = null
        hasGarish = null
    }

    /**
     * @return abort reason, or null when consumption may proceed
     */
    suspend fun prepareDrink(
        itemId: Int,
        count: Int,
        preferences: Preferences?,
        character: KoLCharacter?,
        inventory: InventoryManager?,
        equipmentManager: EquipmentManager?,
        @Suppress("UNUSED_PARAMETER") effectManager: EffectManager?,
    ): String? {
        val itemName = ItemDatabase.getItemName(itemId)
        val consumable = ConsumableDatabase.getConsumableByName(itemName)
        val inebriety = consumable?.amount
            ?: ConsumableDatabase.getInebrietyByName(itemName)
        askAboutOde(itemName, consumable, inebriety, count, preferences, character)
        askAboutTuxedo(consumable, preferences, inventory, equipmentManager)?.let { return it }
        askAboutPinkyRing(consumable, preferences, inventory, equipmentManager)?.let { return it }
        return null
    }

    /**
     * @return abort reason, or null when consumption may proceed
     */
    suspend fun prepareEat(
        itemId: Int,
        preferences: Preferences?,
        inventory: InventoryManager?,
        effectManager: EffectManager?,
    ): String? {
        val itemName = ItemDatabase.getItemName(itemId)
        val consumable = ConsumableDatabase.getConsumableByName(itemName)
        return askAboutGarish(consumable, preferences, inventory, effectManager)
    }

    private suspend fun askAboutOde(
        itemName: String,
        consumable: ConsumableData?,
        inebriety: Int,
        count: Int,
        preferences: Preferences?,
        character: KoLCharacter?,
    ) {
        if (inebriety <= 0) return
        val shotglass =
            if (preferences?.getBoolean("mimeShotglassAvailable", false) == true &&
                preferences.getBoolean("_mimeShotglassUsed", false) != true &&
                inebriety == 1
            ) {
                1
            } else {
                0
            }
        if (inebriety * count - shotglass == 0) return
        val noAdvGain = consumable == null || (consumable.advMin == 0 && consumable.advMax == 0)
        if (noAdvGain) {
            val note = consumable?.notes
            if (note == null || !note.contains("Unspaded")) return
        }
        if (itemName.equals("Temps Tempranillo", ignoreCase = true)) return

        val consumptionTurns = count * inebriety - shotglass
        var turns = odeTurns?.invoke() ?: 0
        if (consumptionTurns <= turns) return

        val canOde = hasSkill?.invoke(ODE_TO_BOOZE) == true &&
            hasAccordion?.invoke() != false &&
            character?.state?.value?.inGLover != true
        val shouldOde = canOde && (canInteract?.invoke() ?: true)
        if (!shouldOde) return
        val odeCost = odeMpCost?.invoke() ?: 50L
        val cast = castOde ?: return
        while (turns < consumptionTurns &&
            (currentMp?.invoke() ?: Long.MAX_VALUE) >= odeCost
        ) {
            if (!cast(ODE_TO_BOOZE)) break
            val newTurns = odeTurns?.invoke() ?: (turns + 1)
            if (newTurns == turns) break
            turns = newTurns
        }
    }

    private suspend fun askAboutGarish(
        consumable: ConsumableData?,
        preferences: Preferences?,
        inventory: InventoryManager?,
        effectManager: EffectManager?,
    ): String? {
        if (consumable == null || !consumable.isLasagna()) return null
        val hasEffect = hasGarish?.invoke()
            ?: effectManager?.state?.value?.effects?.any { it.id == GARISH_EFFECT_ID } == true
        if (hasEffect || HolidayCalendar.isMonday()) return null
        if (preferences?.getBoolean("autoGarish", false) != true) return null
        val available = (inventory?.getCount(FIELD_GAR_POTION) ?: 0) > 0
        if (!available) {
            retrieveItem?.invoke(FIELD_GAR_POTION, 1)
        }
        val use = useItem ?: return null
        if (!use(FIELD_GAR_POTION)) {
            return "Failed to use Potion of the Field Gar."
        }
        val nowHas = hasGarish?.invoke()
            ?: effectManager?.state?.value?.effects?.any { it.id == GARISH_EFFECT_ID } == true
        return if (nowHas) null else "Failed to use Potion of the Field Gar."
    }

    private suspend fun askAboutTuxedo(
        consumable: ConsumableData?,
        preferences: Preferences?,
        inventory: InventoryManager?,
        equipmentManager: EquipmentManager?,
    ): String? {
        if (consumable == null || !consumable.isMartini()) return null
        if (equipmentManager?.hasEquipped(TUXEDO_SHIRT) == true) return null
        if (equipmentManager?.canEquip(TUXEDO_SHIRT) == false) return null
        val available = (inventory?.getCount(TUXEDO_SHIRT) ?: 0) > 0
        if (!available && retrieveItem == null) return null
        if (preferences?.getBoolean("autoTuxedo", false) != true) return null
        if (!available) {
            retrieveItem?.invoke(TUXEDO_SHIRT, 1)
        }
        val equip = equipItem ?: return null
        if (!equip(TUXEDO_SHIRT, EquipmentSlot.SHIRT)) {
            return "Failed to equip Tuxedo Shirt."
        }
        return if (equipmentManager?.hasEquipped(TUXEDO_SHIRT) == true) null
        else "Failed to equip Tuxedo Shirt."
    }

    private suspend fun askAboutPinkyRing(
        consumable: ConsumableData?,
        preferences: Preferences?,
        inventory: InventoryManager?,
        equipmentManager: EquipmentManager?,
    ): String? {
        if (consumable == null || !consumable.isWine()) return null
        if (equipmentManager?.hasEquipped(MAFIA_PINKY_RING) == true) return null
        if (equipmentManager?.canEquip(MAFIA_PINKY_RING) == false) return null
        val available = (inventory?.getCount(MAFIA_PINKY_RING) ?: 0) > 0
        if (!available && retrieveItem == null) return null
        if (preferences?.getBoolean("autoPinkyRing", false) != true) return null
        if (!available) {
            retrieveItem?.invoke(MAFIA_PINKY_RING, 1)
        }
        val slot = listOf(EquipmentSlot.ACC1, EquipmentSlot.ACC2, EquipmentSlot.ACC3)
            .firstOrNull { (equipmentManager?.getEquipmentId(it) ?: -1) <= 0 }
            ?: EquipmentSlot.ACC3
        val equip = equipItem ?: return null
        if (!equip(MAFIA_PINKY_RING, slot)) {
            return "Failed to equip mafia pinky ring."
        }
        return if (equipmentManager?.hasEquipped(MAFIA_PINKY_RING) == true) null
        else "Failed to equip mafia pinky ring."
    }
}
