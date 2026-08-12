package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.EffectDefinitionProxy
import net.sourceforge.kolmafia.maximizer.SkillRequiredItemForEffect
import net.sourceforge.kolmafia.skill.SkillData

/** Desktop UseSkillCommand cast target after count / ^ effect / on-player strip. */
data class CliCastTarget(
    val count: Int,
    val skillName: String,
    val effectName: String?,
)

/**
 * Desktop UseSkillCommand parameter parse:
 * comma list → strip ` on player` → split ` ^ ` → count+skill; `ode` shorthand.
 */
internal fun parseCliCastList(parameters: String): List<CliCastTarget> {
    val rest = parameters.trim()
    if (rest.isEmpty()) return emptyList()
    return rest.split(Regex("""\s*,\s*""")).mapNotNull { parseCliCastSegment(it) }
}

internal fun parseCliCastSegment(segment: String): CliCastTarget? {
    var buff = segment.trim()
    if (buff.isEmpty()) return null
    // Desktop: replaceFirst(" [oO][nN] ", " => ").split(" => ")
    buff = buff.replaceFirst(Regex("""\s+[oO][nN]\s+"""), " => ")
    val skillSide = buff.split(" => ", limit = 2).first().trim()
    if (skillSide.isEmpty()) return null
    val skillEffect = skillSide.split(Regex("""\s+\^\s+"""), limit = 2)
    val skillPart = skillEffect[0].trim()
    val effectName = skillEffect.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    val (count, rawSkill) = parseCastCountAndName(skillPart) ?: return null
    val skillName = expandCastSkillShorthand(rawSkill)
    if (skillName.isEmpty()) return null
    return CliCastTarget(count = count, skillName = skillName, effectName = effectName)
}

/** Desktop UseSkillCommand.expandRecognizedShorthand. */
internal fun expandCastSkillShorthand(skillName: String): String =
    when (skillName.lowercase()) {
        "ode" -> "The Ode to Booze"
        else -> skillName
    }

/**
 * Desktop AbstractCommand.splitCountAndName for cast:
 * `N name`, `* name` (v13: treat as 1), or bare name.
 */
internal fun parseCastCountAndName(parameters: String): Pair<Int, String>? {
    val rest = parameters.trim()
    if (rest.isEmpty()) return null
    if (rest.startsWith("* ")) {
        val name = rest.substring(2).trim()
        return if (name.isEmpty()) null else 1 to name
    }
    return parseConsumeQtyName(rest)
}

/**
 * Desktop UseSkillCommand cast loop — resolve skill by name (not full ^ string),
 * equip remaster item when desired effect requires it, then cast.
 *
 * @param echoUnknown when true (bare `cast skill` form), print unknown skills;
 *   count-prefixed form stays silent on miss.
 */
internal fun GameRuntimeLibrary.cliCast(
    parameters: String,
    print: (String) -> Unit,
    echoUnknown: Boolean,
) {
    val targets = parseCliCastList(parameters)
    if (targets.isEmpty()) {
        if (echoUnknown) print("[cli] cast")
        return
    }
    for (target in targets) {
        val skill = skillManager?.state?.value?.skills
            ?.find { it.name.equals(target.skillName, ignoreCase = true) }
        if (skill == null) {
            if (echoUnknown) print("[cli] cast ${target.skillName}")
            continue
        }
        equipRemasterItemIfNeeded(skill, target.effectName, print)
        runBlocking { skillManager?.cast(skill, target.count) }
    }
}

/**
 * Desktop UseSkillRequest.setDesiredEffect → forcedItem equip before cast.
 */
internal fun GameRuntimeLibrary.equipRemasterItemIfNeeded(
    skill: SkillData,
    effectName: String?,
    print: (String) -> Unit,
) {
    if (effectName.isNullOrBlank()) return
    val effectId = EffectDefinitionProxy.getByIdOrName(effectName)?.id ?: return
    val itemId = SkillRequiredItemForEffect.requiredItem(skill.id, effectId)
    if (itemId == -1) return
    val inv = inventoryManager?.state?.value ?: return
    if (inv.equipped.values.any { it.itemId == itemId }) return
    val item = inv.items[itemId] ?: return
    runBlocking {
        val slot = remasterEquipSlot(itemId)
        val result = if (slot != null && equipmentRequest != null) {
            equipmentRequest.equipItem(itemId, slot)
        } else {
            inventoryManager?.equipItem(item, "default") ?: Result.success(Unit)
        }
        result.onFailure { print(it.message ?: "Failed to equip remaster item.") }
    }
}

private fun remasterEquipSlot(itemId: Int): EquipmentSlot? {
    // Legendary pasta wand / velour weapons → weapon; shield → offhand.
    // Prefer inventory default when slot unknown.
    return when (itemId) {
        11884 -> EquipmentSlot.OFFHAND // April Shower Thoughts shield
        10114, 10117, 10120, 12223 -> EquipmentSlot.WEAPON
        else -> null
    }
}
