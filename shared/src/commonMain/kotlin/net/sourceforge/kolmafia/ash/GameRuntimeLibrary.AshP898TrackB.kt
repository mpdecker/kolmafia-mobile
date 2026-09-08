package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarUsability
import net.sourceforge.kolmafia.inventory.EquippedItemCount
import net.sourceforge.kolmafia.maximizer.FamiliarCarryRules
import net.sourceforge.kolmafia.modifiers.SlotNames
import net.sourceforge.kolmafia.session.YouRobotManager

/**
 * AshP898–AshP904 — Equip / familiar gear ASH surface (Track B).
 */

// ── AshP898 — equip(item), equip(item,slot), equip(slot,item) ──────────────

internal fun GameRuntimeLibrary.registerAshP898Batch(scope: AshScope) {
    regFn(scope, "equip", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { rt, args ->
        val itemName = args[0].toString()
        val db = gameDatabase ?: return@regFn AshValue.of(false)
        val item = db.item(itemName) ?: return@regFn AshValue.of(false)
        val req = equipmentRequest
        if (req == null) {
            dispatchCli("equip $itemName", rt)
            return@regFn AshValue.TRUE
        }
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
    val unequip = itemName.equals("none", ignoreCase = true) || itemName.isBlank()
    val item = if (unequip) null else db.item(itemName) ?: return AshValue.of(false)
    if (!unequip && item != null && !familiarCanEquipItem(race, item)) {
        return AshValue.of(false)
    }
    val familiar = fm.state.value.ownedFamiliars
        .firstOrNull { it.race.equals(race, ignoreCase = true) }
        ?: return AshValue.of(false)
    val activeFam = fm.state.value.activeFamiliar
    if (activeFam != null && activeFam.race.equals(race, ignoreCase = true)) {
        val slot = SlotNames.toEquipmentSlot("familiar") ?: return AshValue.of(false)
        val ok = runBlocking {
            if (unequip) equipmentRequest?.unequipSlot(slot)?.isSuccess == true
            else equipmentRequest?.equipItem(item!!.id, slot)?.isSuccess == true
        }
        return AshValue.of(ok)
    }
    val ok = runBlocking {
        if (unequip) fm.equipItem(familiar, 0).isSuccess
        else fm.equipItem(familiar, item!!.id).isSuccess
    }
    return AshValue.of(ok)
}

// ── AshP900 — can_equip(item), can_equip(familiar), can_equip(familiar,item) ─

internal fun GameRuntimeLibrary.registerAshP900Batch(scope: AshScope) {
    regFn(scope, "can_equip", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val db = gameDatabase ?: return@regFn AshValue.of(false)
        val item = db.item(itemName) ?: return@regFn AshValue.of(false)
        if (item.primaryUse !in EQUIPPABLE_USES) return@regFn AshValue.of(false)
        val mgr = equipmentManager
        AshValue.of(mgr?.canEquip(item.id) ?: true)
    }

    regFn(scope, "can_equip", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        AshValue.of(familiarTypeCanEquip(args[0].toString()))
    }

    regFn(scope, "can_equip", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR, "it" to AshType.ITEM)) { _, args ->
        val race = args[0].toString()
        val itemName = args[1].toString()
        if (itemName.equals("none", ignoreCase = true) || itemName.isBlank()) {
            return@regFn AshValue.of(familiarTypeCanEquip(race))
        }
        val db = gameDatabase ?: return@regFn AshValue.of(false)
        val item = db.item(itemName) ?: return@regFn AshValue.of(false)
        AshValue.of(familiarCanEquipItem(race, item))
    }
}

/** Desktop ASH can_equip(familiar): owned + path/limit gates when terrarium is known. */
private fun GameRuntimeLibrary.familiarTypeCanEquip(race: String): Boolean {
    val state = character?.state?.value
    if (state != null) {
        if (state.inPokefam) return false
        if (!state.ascensionPath.canUseFamiliars()) return false
        if (state.inRobocore && !YouRobotManager.canUseFamiliars()) return false
    }
    val fm = familiarManager
    if (fm != null) {
        return FamiliarUsability.usableByRace(fm.state.value, race, state, preferences) != null
    }
    val def = FamiliarDefinitionDatabase.getByName(race) ?: return false
    val fam = FamiliarData(
        id = def.id,
        name = def.name,
        race = def.name,
        weight = 1,
        experience = 0,
        kills = 0,
    )
    return FamiliarUsability.isUsable(fam, state, preferences)
}

/** Desktop FamiliarData.canEquip(item) subset via [FamiliarCarryRules] + FAMILIAR gear. */
private fun GameRuntimeLibrary.familiarCanEquipItem(race: String, item: ItemData): Boolean {
    if (!familiarTypeCanEquip(race)) return false
    if (item.primaryUse == ItemPrimaryUse.FAMILIAR) return true
    return FamiliarCarryRules.canCarryItem(race, item)
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
