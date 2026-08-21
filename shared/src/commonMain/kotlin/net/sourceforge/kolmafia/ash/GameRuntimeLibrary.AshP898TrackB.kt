package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.inventory.EquippedItemCount
import net.sourceforge.kolmafia.modifiers.SlotNames

/**
 * AshP898–AshP904 — Equip / familiar gear ASH surface (Track B).
 */

// ── AshP898 — equip(item), equip(item,slot), equip(slot,item) ──────────────

internal fun GameRuntimeLibrary.registerAshP898Batch(scope: AshScope) {
    regFn(scope, "equip", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val req = equipmentRequest ?: return@regFn AshValue.of(false)
        val db = gameDatabase ?: return@regFn AshValue.of(false)
        val item = db.item(itemName) ?: return@regFn AshValue.of(false)
        val slot = SlotNames.toEquipmentSlot(
            when (item.primaryUse) {
                ItemPrimaryUse.HAT -> "hat"
                ItemPrimaryUse.WEAPON -> "weapon"
                ItemPrimaryUse.SIXGUN -> "holster"
                ItemPrimaryUse.OFFHAND -> "off-hand"
                ItemPrimaryUse.CONTAINER -> "container"
                ItemPrimaryUse.SHIRT -> "shirt"
                ItemPrimaryUse.PANTS -> "pants"
                ItemPrimaryUse.ACCESSORY -> "acc1"
                ItemPrimaryUse.FAMILIAR -> "familiar"
                else -> return@regFn AshValue.of(false)
            }
        ) ?: return@regFn AshValue.of(false)
        val ok = runBlocking { req.equipItem(item.id, slot).isSuccess }
        AshValue.of(ok)
    }

    regFn(scope, "equip", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "slot" to AshType.SLOT)) { _, args ->
        val itemName = args[0].toString()
        val slotName = args[1].toString()
        val req = equipmentRequest ?: return@regFn AshValue.of(false)
        val db = gameDatabase ?: return@regFn AshValue.of(false)
        val item = db.item(itemName) ?: return@regFn AshValue.of(false)
        val slot = SlotNames.toEquipmentSlot(slotName) ?: return@regFn AshValue.of(false)
        val ok = runBlocking { req.equipItem(item.id, slot).isSuccess }
        AshValue.of(ok)
    }

    regFn(scope, "equip", AshType.BOOLEAN,
        listOf("slot" to AshType.SLOT, "it" to AshType.ITEM)) { _, args ->
        val slotName = args[0].toString()
        val itemName = args[1].toString()
        val req = equipmentRequest ?: return@regFn AshValue.of(false)
        val db = gameDatabase ?: return@regFn AshValue.of(false)
        val item = db.item(itemName) ?: return@regFn AshValue.of(false)
        val slot = SlotNames.toEquipmentSlot(slotName) ?: return@regFn AshValue.of(false)
        if (itemName.equals("none", ignoreCase = true) || item.id == 0) {
            val ok = runBlocking { req.unequipSlot(slot).isSuccess }
            return@regFn AshValue.of(ok)
        }
        val ok = runBlocking { req.equipItem(item.id, slot).isSuccess }
        AshValue.of(ok)
    }
}

// ── AshP899 — equip(familiar,item), equip(item,familiar) ───────────────────

internal fun GameRuntimeLibrary.registerAshP899Batch(scope: AshScope) {
    regFn(scope, "equip", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR, "it" to AshType.ITEM)) { _, args ->
        val race = args[0].toString()
        val itemName = args[1].toString()
        equipFamiliarItem(race, itemName)
    }

    regFn(scope, "equip", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "fam" to AshType.FAMILIAR)) { _, args ->
        val itemName = args[0].toString()
        val race = args[1].toString()
        equipFamiliarItem(race, itemName)
    }
}

private fun GameRuntimeLibrary.equipFamiliarItem(race: String, itemName: String): AshValue {
    val fm = familiarManager ?: return AshValue.of(false)
    val db = gameDatabase ?: return AshValue.of(false)
    val item = db.item(itemName) ?: return AshValue.of(false)
    val familiar = fm.state.value.ownedFamiliars
        .firstOrNull { it.race.equals(race, ignoreCase = true) }
        ?: return AshValue.of(false)
    val activeFam = fm.state.value.activeFamiliar
    if (activeFam != null && activeFam.race.equals(race, ignoreCase = true)) {
        val slot = SlotNames.toEquipmentSlot("familiar") ?: return AshValue.of(false)
        val ok = runBlocking { equipmentRequest?.equipItem(item.id, slot)?.isSuccess ?: false }
        return AshValue.of(ok)
    }
    val ok = runBlocking { fm.equipItem(familiar, item.id).isSuccess }
    return AshValue.of(ok)
}

// ── AshP900 — can_equip(item), can_equip(familiar), can_equip(familiar,item) ─

internal fun GameRuntimeLibrary.registerAshP900Batch(scope: AshScope) {
    regFn(scope, "can_equip", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val db = gameDatabase ?: return@regFn AshValue.of(false)
        val item = db.item(itemName) ?: return@regFn AshValue.of(false)
        val isEquip = item.primaryUse in EQUIPPABLE_USES
        AshValue.of(isEquip)
    }

    regFn(scope, "can_equip", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val race = args[0].toString()
        val fm = familiarManager ?: return@regFn AshValue.of(false)
        val owned = fm.state.value.ownedFamiliars
            .any { it.race.equals(race, ignoreCase = true) }
        AshValue.of(owned)
    }

    regFn(scope, "can_equip", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR, "it" to AshType.ITEM)) { _, args ->
        val race = args[0].toString()
        val itemName = args[1].toString()
        val db = gameDatabase ?: return@regFn AshValue.of(false)
        val item = db.item(itemName) ?: return@regFn AshValue.of(false)
        AshValue.of(item.primaryUse == ItemPrimaryUse.FAMILIAR)
    }
}

private val EQUIPPABLE_USES = setOf(
    ItemPrimaryUse.HAT,
    ItemPrimaryUse.WEAPON,
    ItemPrimaryUse.SIXGUN,
    ItemPrimaryUse.OFFHAND,
    ItemPrimaryUse.CONTAINER,
    ItemPrimaryUse.SHIRT,
    ItemPrimaryUse.PANTS,
    ItemPrimaryUse.ACCESSORY,
    ItemPrimaryUse.FAMILIAR,
)

// ── AshP901 — equipped_amount(item[, includeAllFamiliars]) ──────────────────

internal fun GameRuntimeLibrary.registerAshP901Batch(scope: AshScope) {
    regFn(scope, "equipped_amount", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val db = gameDatabase ?: return@regFn AshValue.of(0L)
        val item = db.item(itemName) ?: return@regFn AshValue.of(0L)
        val equip = character?.state?.value?.equipment ?: emptyMap()
        val count = OutfitManager.equippedCount(itemName, equip)
        AshValue.of(count.toLong())
    }

    regFn(scope, "equipped_amount", AshType.INT,
        listOf("it" to AshType.ITEM, "includeAllFamiliars" to AshType.BOOLEAN)) { _, args ->
        val itemName = args[0].toString()
        val includeAll = args[1].toLong() == 1L
        val db = gameDatabase ?: return@regFn AshValue.of(0L)
        val item = db.item(itemName) ?: return@regFn AshValue.of(0L)
        val equip = character?.state?.value?.equipment ?: emptyMap()
        val charState = character?.state?.value
        val count = if (includeAll) {
            EquippedItemCount.totalEquippedCount(
                item.id, itemName, equip, charState, db, familiarManager,
            )
        } else {
            OutfitManager.equippedCount(itemName, equip)
        }
        AshValue.of(count.toLong())
    }
}

// ── AshP902 — familiar_weight(familiar) ─────────────────────────────────────

internal fun GameRuntimeLibrary.registerAshP902Batch(scope: AshScope) {
    regFn(scope, "familiar_weight", AshType.INT,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val race = args[0].toString()
        val fm = familiarManager ?: return@regFn AshValue.of(0L)
        val familiar = fm.state.value.ownedFamiliars
            .firstOrNull { it.race.equals(race, ignoreCase = true) }
        AshValue.of((familiar?.weight ?: 0).toLong())
    }
}

// ── AshP903 — is_familiar_equipment_locked, lock_familiar_equipment ─────────

internal fun GameRuntimeLibrary.registerAshP903Batch(scope: AshScope) {
    regFn(scope, "is_familiar_equipment_locked", AshType.BOOLEAN, emptyList()) { _, _ ->
        val locked = preferences?.getBoolean("familiarEquipmentLocked", false) ?: false
        AshValue.of(locked)
    }

    regFn(scope, "lock_familiar_equipment", AshType.VOID,
        listOf("lock" to AshType.BOOLEAN)) { _, args ->
        val lock = args[0].toBoolean()
        val current = preferences?.getBoolean("familiarEquipmentLocked", false) ?: false
        if (lock != current) {
            // Desktop FamiliarRequest.lockFamiliarItem → familiar.php?action=lockequip
            visitKolPage("familiar.php?action=lockequip")
            preferences?.setBoolean("familiarEquipmentLocked", lock)
        }
        AshValue(AshType.VOID, null)
    }
}

// ── AshP904 — my_effective_familiar, my_bjorned_familiar, my_companion ──────

internal fun GameRuntimeLibrary.registerAshP904Batch(scope: AshScope) {
    regFn(scope, "my_effective_familiar", AshType.FAMILIAR, emptyList()) { _, _ ->
        val fm = familiarManager
        val activeName = fm?.state?.value?.activeFamiliar?.race
        val charName = character?.state?.value?.familiarName
        val name = (activeName ?: charName)?.takeIf { it.isNotBlank() } ?: "none"
        AshValue.familiar(name)
    }

    regFn(scope, "my_bjorned_familiar", AshType.FAMILIAR, emptyList()) { _, _ ->
        val name = character?.state?.value?.bjornedFamiliarName?.takeIf { it.isNotBlank() }
            ?: "none"
        AshValue.familiar(name)
    }

    regFn(scope, "my_companion", AshType.STRING, emptyList()) { _, _ ->
        val companion = preferences?.getString("_jpieFamiliar", "")
            ?: ""
        AshValue.of(companion)
    }
}
