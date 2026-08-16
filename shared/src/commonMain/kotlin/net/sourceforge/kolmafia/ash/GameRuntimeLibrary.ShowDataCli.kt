package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap

/**
 * Desktop ShowDataCommand `effects [filter]`, `inv`/`inventory [filter]`,
 * `closet`/`closet list [filter]`, `storage [filter]`, `display [filter]`,
 * and `familiars [filter]` (plus FamiliarCommand `familiar` / `familiar list [filter]`).
 * Type-less leftover is a case-insensitive substring of the formatted line.
 */
internal fun GameRuntimeLibrary.cliEffects(filter: String, rt: AshRuntimeContext) {
    runBlocking { effectManager?.fetchEffects() }
    val effects = effectManager?.state?.value?.effects.orEmpty()
    var nBuffs = 0
    for (effect in effects) {
        val skillName = UneffectSkillEffectMap.effectToSkill(effect.name) ?: continue
        val skillId = SkillDefinitionDatabase.getByName(skillName)?.id ?: continue
        if (SkillDefinitionProxy.isAccordionThiefSong(skillId)) nBuffs++
    }
    rt.print("$nBuffs of ${songLimit()} AT buffs active.")
    val leftover = filter.trim()
    for (effect in effects) {
        val line = formatEffectLine(effect)
        if (leftover.isNotEmpty() && !line.contains(leftover, ignoreCase = true)) continue
        rt.print(line)
    }
}

internal fun GameRuntimeLibrary.cliInventory(filter: String, rt: AshRuntimeContext) {
    runBlocking {
        inventoryManager?.fetchInventory()
        inventoryManager?.syncCharacterEquipment()
    }
    val leftover = filter.trim()
    val items = inventoryManager?.state?.value?.items?.values.orEmpty()
    for (item in items) {
        val line = formatInventoryLine(item)
        if (leftover.isNotEmpty() && !line.contains(leftover, ignoreCase = true)) continue
        rt.print(line)
    }
}

internal fun GameRuntimeLibrary.songLimit(): Int {
    val values = buildCurrentModifiers().values
    var limit = 3
    if (values.get(BooleanModifier.FOUR_SONGS)) limit++
    limit += values.getInt(DoubleModifier.ADDITIONAL_SONG)
    return limit
}

internal fun GameRuntimeLibrary.formatEffectLine(effect: EffectData): String {
    var name = effect.name
    if (name.equals("On the Trail", ignoreCase = true)) {
        val monster = preferences?.getString("olfactedMonster", "").orEmpty()
        if (monster.isNotEmpty() && !monster.equals("unknown", ignoreCase = true)) {
            name = "$name [$monster]"
        }
    } else {
        if (EffectDatabase.getByName(effect.name)?.isSong() == true) {
            name = "♫ $name"
        }
        val skillName = UneffectSkillEffectMap.effectToSkill(effect.name)
        val skillId = skillName?.let { SkillDefinitionDatabase.getByName(it)?.id }
        if (skillId != null && SkillDefinitionProxy.isExpression(skillId)) {
            name = "☺ $name"
        }
    }
    return when {
        effect.duration == 1 -> name
        effect.duration < 0 -> "$name (∞)"
        else -> "$name (${effect.duration})"
    }
}

internal fun GameRuntimeLibrary.cliCloset(filter: String, rt: AshRuntimeContext) {
    val contents = runBlocking {
        val req = closetRequest ?: return@runBlocking emptyMap()
        val fetched = req.fetchContents()
        preferences?.let { CollectionCacheSync.saveCloset(it, fetched) }
        fetched
    }
    printItemMap(contents, filter, rt)
}

internal fun GameRuntimeLibrary.cliStorage(filter: String, rt: AshRuntimeContext) {
    val contents = runBlocking {
        val req = storageRequest ?: return@runBlocking emptyMap()
        val classified = req.fetchClassifiedContents(character?.state?.value, preferences)
        preferences?.let { CollectionCacheSync.saveStorage(it, classified.storage, classified.freepulls) }
        classified.storage
    }
    printItemMap(contents, filter, rt)
}

internal fun GameRuntimeLibrary.cliDisplay(filter: String, rt: AshRuntimeContext) {
    val contents = runBlocking {
        val req = displayCaseRequest ?: return@runBlocking emptyMap()
        val fetched = req.fetchContents()
        preferences?.let { CollectionCacheSync.saveDisplay(it, fetched) }
        fetched
    }
    printItemMap(contents, filter, rt)
}

internal fun GameRuntimeLibrary.cliFamiliars(filter: String, rt: AshRuntimeContext) {
    runBlocking { familiarManager?.fetchFamiliars() }
    val leftover = filter.trim()
    for (fam in familiarManager?.state?.value?.ownedFamiliars.orEmpty()) {
        val line = formatFamiliarLine(fam)
        if (leftover.isNotEmpty() && !line.contains(leftover, ignoreCase = true)) continue
        rt.print(line)
    }
}

internal fun GameRuntimeLibrary.printItemMap(
    contents: Map<Int, Int>,
    filter: String,
    rt: AshRuntimeContext,
) {
    val leftover = filter.trim()
    for ((id, qty) in contents) {
        val name = gameDatabase?.item(id)?.name
            ?: ItemDatabase.getById(id)?.name
            ?: "Item #$id"
        val line = formatInventoryLine(InventoryItem(id, name, qty, ItemType.OTHER))
        if (leftover.isNotEmpty() && !line.contains(leftover, ignoreCase = true)) continue
        rt.print(line)
    }
}

internal fun formatInventoryLine(item: InventoryItem): String =
    if (item.quantity == 1) item.name else "${item.name} (${item.quantity})"

internal fun formatFamiliarLine(fam: FamiliarData): String =
    "${fam.race} (${fam.weight} lbs)"
