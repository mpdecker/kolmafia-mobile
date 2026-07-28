package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.data.BountyDatabase
import net.sourceforge.kolmafia.data.DescriptionCache
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.modifiers.ClassNames
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ElementNames
import net.sourceforge.kolmafia.modifiers.ModifierNames
import net.sourceforge.kolmafia.modifiers.PathNames
import net.sourceforge.kolmafia.modifiers.PhylumNames
import net.sourceforge.kolmafia.modifiers.ServantData
import net.sourceforge.kolmafia.modifiers.SlotNames
import net.sourceforge.kolmafia.modifiers.StatNames
import net.sourceforge.kolmafia.modifiers.ThrallNames
import net.sourceforge.kolmafia.modifiers.VykeaCompanionData
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

/** Canonical ASH entity display names and numeric ids (AshP109). */
internal fun GameRuntimeLibrary.entityName(type: AshType, ref: String): String {
    val trimmed = ref.trim()
    if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return ""

    val db = gameDatabase
    return when (type) {
        AshType.ITEM -> db?.item(trimmed)?.name ?: trimmed
        AshType.SKILL -> db?.skill(trimmed)?.name ?: trimmed
        AshType.EFFECT -> db?.effect(trimmed)?.name ?: trimmed
        AshType.FAMILIAR -> db?.familiar(trimmed)?.name ?: trimmed
        AshType.MONSTER -> db?.monster(trimmed)?.name ?: trimmed
        AshType.LOCATION -> resolveLocationDisplayName(trimmed).ifEmpty { trimmed }
        AshType.CLASS -> ClassNames.resolve(trimmed) ?: trimmed
        AshType.STAT -> StatNames.resolve(trimmed) ?: trimmed
        AshType.SLOT -> SlotNames.resolve(trimmed) ?: trimmed
        AshType.ELEMENT -> ElementNames.resolve(trimmed) ?: trimmed
        AshType.PHYLUM -> PhylumNames.resolve(trimmed) ?: trimmed
        AshType.PATH -> PathNames.resolve(trimmed) ?: trimmed
        AshType.THRALL -> ThrallNames.resolve(trimmed) ?: trimmed
        AshType.SERVANT -> ServantData.resolve(trimmed)?.type ?: trimmed
        AshType.VYKEA -> VykeaCompanionData.resolve(trimmed) ?: trimmed
        AshType.BOUNTY -> BountyDatabase.resolve(trimmed) ?: trimmed
        AshType.MODIFIER -> ModifierNames.byCaselessName(trimmed) ?: trimmed
        AshType.COINMASTER -> coinmasterDisplayName(trimmed)
        else -> trimmed
    }
}

internal fun GameRuntimeLibrary.entityDesc(type: AshType, ref: String): String {
    val db = gameDatabase ?: return ""
    return when (type) {
        AshType.ITEM -> db.item(ref)?.id?.let { DescriptionCache.itemDescription(it) }.orEmpty()
        AshType.EFFECT -> db.effect(ref)?.id?.let { DescriptionCache.effectDescription(it) }.orEmpty()
        AshType.SKILL -> db.skill(ref)?.id?.let { DescriptionCache.skillDescription(it) }.orEmpty()
        else -> ""
    }
}

internal fun GameRuntimeLibrary.entityToInt(type: AshType, ref: String): Long {
    val trimmed = ref.trim()
    if (trimmed.isEmpty() || trimmed.equals("none", ignoreCase = true)) return 0L

    val db = gameDatabase
    return when (type) {
        AshType.CLASS -> {
            ClassNames.resolve(trimmed)?.let { name ->
                CharacterClass.entries.firstOrNull {
                    it.displayName.equals(name, ignoreCase = true)
                }?.id?.toLong()
            } ?: trimmed.toIntOrNull()?.toLong() ?: 0L
        }
        AshType.STAT -> statToInt(trimmed)
        AshType.SLOT -> slotToInt(trimmed)
        AshType.ELEMENT -> elementToInt(trimmed)
        AshType.PHYLUM -> phylumToInt(trimmed)
        AshType.PATH -> pathToInt(trimmed)
        AshType.THRALL -> thrallToInt(trimmed)
        AshType.SERVANT -> ServantData.resolve(trimmed)?.id?.toLong() ?: 0L
        AshType.VYKEA -> vykeaToInt(trimmed)
        AshType.BOUNTY -> bountyToInt(trimmed)
        AshType.MODIFIER -> modifierToInt(trimmed)
        AshType.COINMASTER -> coinmasterToInt(trimmed)
        AshType.LOCATION -> locationToInt(trimmed)
        AshType.ITEM -> db?.item(trimmed)?.id?.toLong() ?: 0L
        AshType.SKILL -> db?.skill(trimmed)?.id?.toLong() ?: 0L
        AshType.EFFECT -> db?.effect(trimmed)?.id?.toLong() ?: 0L
        AshType.FAMILIAR -> db?.familiar(trimmed)?.id?.toLong() ?: 0L
        AshType.MONSTER -> db?.monster(trimmed)?.id?.toLong() ?: 0L
        else -> 0L
    }
}

private fun GameRuntimeLibrary.locationToInt(ref: String): Long {
    resolveLocation(ref)?.id?.toIntOrNull()?.toLong()?.let { return it }
    return gameDatabase?.zone(ref)?.snarfblat?.toIntOrNull()?.toLong() ?: 0L
}

private fun coinmasterDisplayName(ref: String): String {
    val master = CoinmasterDatabase.findByNickname(ref)
        ?: CoinmasterDatabase.findByMasterName(ref)
    return master?.masterName ?: ref
}

private fun coinmasterToInt(ref: String): Long {
    val master = CoinmasterDatabase.findByNickname(ref)
        ?: CoinmasterDatabase.findByMasterName(ref)
    return master?.shopId?.toIntOrNull()?.toLong() ?: 0L
}

private fun statToInt(ref: String): Long {
    val resolved = StatNames.resolve(ref) ?: return 0L
    return when (resolved) {
        StatNames.MUSCLE -> 0L
        StatNames.MYSTICALITY -> 1L
        StatNames.MOXIE -> 2L
        StatNames.SUBMUSCLE -> 3L
        StatNames.SUBMYSTICALITY -> 4L
        StatNames.SUBMOXIE -> 5L
        else -> 0L
    }
}

/** Desktop [Slot] ordinals for core equipment slots. */
private fun slotToInt(ref: String): Long {
    val resolved = SlotNames.resolve(ref)?.lowercase() ?: return 0L
    return when (resolved) {
        "hat" -> 1L
        "weapon" -> 2L
        "holster" -> 3L
        "off-hand" -> 4L
        "container" -> 5L
        "shirt" -> 6L
        "pants" -> 7L
        "acc1" -> 8L
        "acc2" -> 9L
        "acc3" -> 10L
        "familiar" -> 11L
        else -> 0L
    }
}

/** Desktop [MonsterDatabase.Element] ordinals (NONE=0). */
private fun elementToInt(ref: String): Long {
    val resolved = ElementNames.resolve(ref)?.lowercase() ?: return 0L
    return when (resolved) {
        "cold" -> 1L
        "hot" -> 2L
        "sleaze" -> 3L
        "spooky" -> 4L
        "stench" -> 5L
        "slime" -> 6L
        "supercold" -> 7L
        else -> 0L
    }
}

private fun phylumToInt(ref: String): Long {
    val resolved = PhylumNames.resolve(ref) ?: return 0L
    val phyla = listOf(
        "beast", "bug", "constellation", "construct", "demon", "dude", "elemental",
        "elf", "fish", "goblin", "hippy", "hobo", "horror", "humanoid", "mer-kin",
        "orc", "penguin", "pirate", "plant", "slime", "undead", "weird",
    )
    val index = phyla.indexOfFirst { it.equals(resolved, ignoreCase = true) }
    return if (index >= 0) (index + 1).toLong() else 0L
}

private fun pathToInt(ref: String): Long {
    PathNames.resolve(ref)?.let { resolved ->
        AscensionPath.entries.firstOrNull { it.apiName.equals(resolved, ignoreCase = true) }
            ?.let { return it.pathId.toLong() }
        AscensionPath.fromApiString(resolved).takeIf { it != AscensionPath.UNKNOWN }
            ?.let { return it.pathId.toLong() }
    }
    AscensionPath.fromApiString(ref).takeIf { it != AscensionPath.UNKNOWN }
        ?.let { return it.pathId.toLong() }
    return 0L
}

private fun thrallToInt(ref: String): Long {
    val resolved = ThrallNames.resolve(ref) ?: return 0L
    val thralls = ModifierDatabase.byTypeAndName["Thrall"]?.keys?.sorted() ?: return 0L
    val index = thralls.indexOfFirst { it.equals(resolved, ignoreCase = true) }
    return if (index >= 0) (index + 1).toLong() else 0L
}

private fun vykeaToInt(ref: String): Long {
    val resolved = VykeaCompanionData.resolve(ref) ?: return 0L
    val index = VykeaCompanionData.catalog.indexOfFirst { it.equals(resolved, ignoreCase = true) }
    return if (index >= 0) (index + 1).toLong() else 0L
}

private fun bountyToInt(ref: String): Long {
    val resolved = BountyDatabase.resolve(ref) ?: return 0L
    val names = BountyDatabase.all().map { it.name }.sorted()
    val index = names.indexOfFirst { it.equals(resolved, ignoreCase = true) }
    return if (index >= 0) (index + 1).toLong() else 0L
}

private fun modifierToInt(ref: String): Long {
    val resolved = ModifierNames.byCaselessName(ref) ?: return 0L
    DoubleModifier.entries.forEachIndexed { index, mod ->
        if (mod.tag.equals(resolved, ignoreCase = true)) return index.toLong()
    }
    return 0L
}
