package net.sourceforge.kolmafia.item

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.effect.EffectData
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.skill.SkillData

/** Desktop ConcoctionDatabase.getFree*Turns parity. */
object FreeCraftingTurns {

    private const val EFFECT_INIGOS = 716
    private const val EFFECT_CRAFT_TEA = 1989
    private const val EFFECT_COOKING_CONCENTRATE = 2837

    private const val SKILL_RAPID_PROTOTYPING = 125
    private const val SKILL_EXPERT_CORNER_CUTTER = 177
    private const val SKILL_ELF_GUARD_COOKING = 229
    private const val SKILL_OLD_SCHOOL_COCKTAIL = 230
    private const val SKILL_HOLIDAY_MULTITASKING = 240

    private const val ITEM_WARBEAR_AUTO_ANVIL = 6965
    private const val ITEM_LEGION_JACKHAMMER = 4927
    private const val ITEM_THORS_PLIERS = 7709

    private const val FAMILIAR_COOKBOOKBAT = "Cookbookbat"

    data class Context(
        val preferences: Preferences? = null,
        val state: CharacterState = CharacterState(),
        val skills: List<SkillData> = emptyList(),
        val effects: List<EffectData> = emptyList(),
        val itemCount: (Int) -> Int = { 0 },
        val ownedFamiliar: (String) -> Boolean = { false },
    ) {
        fun prefInt(key: String): Int = preferences?.getInt(key, 0) ?: 0

        fun hasSkill(skillId: Int): Boolean = skills.any { it.id == skillId }

        fun effectDuration(effectId: Int): Int =
            effects.filter { it.id == effectId }.sumOf { it.duration.coerceAtLeast(0) }

        fun standardAllowed(type: RestrictedItemType, key: String): Boolean =
            StandardRequest.isAllowed(type, key, state)
    }

    fun freeCraftingTurns(context: Context = Context()): Int {
        var total = context.effectDuration(EFFECT_INIGOS) / 5
        if (context.hasSkill(SKILL_RAPID_PROTOTYPING) &&
            context.standardAllowed(RestrictedItemType.SKILLS, "Rapid Prototyping")
        ) {
            total += 5 - context.prefInt("_rapidPrototypingUsed")
        }
        if (context.hasSkill(SKILL_EXPERT_CORNER_CUTTER) &&
            context.standardAllowed(RestrictedItemType.SKILLS, "Expert Corner-Cutter") &&
            context.state.adventuresLeft > 0
        ) {
            total += 5 - context.prefInt("_expertCornerCutterUsed")
        }
        total += context.effectDuration(EFFECT_CRAFT_TEA) / 5
        if (context.standardAllowed(RestrictedItemType.ITEMS, "Cold Medicine Cabinet")) {
            total += context.prefInt("homebodylCharges")
        }
        if (context.hasSkill(SKILL_HOLIDAY_MULTITASKING) &&
            context.standardAllowed(RestrictedItemType.SKILLS, "Holiday Multitasking")
        ) {
            total += 3 - context.prefInt("_holidayMultitaskingUsed")
        }
        if (context.standardAllowed(RestrictedItemType.ITEMS, "Leprecondo")) {
            total += context.prefInt("craftingPlansCharges")
        }
        return total.coerceAtLeast(0)
    }

    fun freeCookingTurns(context: Context = Context()): Int {
        var total = 0
        if (context.standardAllowed(RestrictedItemType.FAMILIARS, FAMILIAR_COOKBOOKBAT) &&
            context.ownedFamiliar(FAMILIAR_COOKBOOKBAT)
        ) {
            total += 5 - context.prefInt("_cookbookbatCrafting")
        }
        total += context.effectDuration(EFFECT_COOKING_CONCENTRATE) / 5
        if (context.hasSkill(SKILL_ELF_GUARD_COOKING) &&
            context.standardAllowed(RestrictedItemType.SKILLS, "Elf Guard Cooking")
        ) {
            total += 3 - context.prefInt("_elfGuardCookingUsed")
        }
        return total.coerceAtLeast(0)
    }

    fun freeCocktailcraftingTurns(context: Context = Context()): Int {
        if (!context.hasSkill(SKILL_OLD_SCHOOL_COCKTAIL) ||
            !context.standardAllowed(RestrictedItemType.SKILLS, "Old-School Cocktailcrafting")
        ) {
            return 0
        }
        return (3 - context.prefInt("_oldSchoolCocktailCraftingUsed")).coerceAtLeast(0)
    }

    fun freeSmithingTurns(context: Context = Context()): Int {
        var total = 0
        if (context.itemCount(ITEM_WARBEAR_AUTO_ANVIL) > 0) {
            total += 5 - context.prefInt("_warbearAutoAnvilCrafting")
        }
        if (context.itemCount(ITEM_LEGION_JACKHAMMER) > 0) {
            total += 3 - context.prefInt("_legionJackhammerCrafting")
        }
        if (context.itemCount(ITEM_THORS_PLIERS) > 0) {
            total += 10 - context.prefInt("_thorsPliersCrafting")
        }
        return total.coerceAtLeast(0)
    }

    fun freeTurnsForMethod(method: String?, context: Context = Context()): Int {
        if (method == null) return 0
        var freeCrafts = freeCraftingTurns(context)
        when (method) {
            "SMITH", "SSMITH" -> freeCrafts += freeSmithingTurns(context)
            "COOK_FANCY" -> freeCrafts += freeCookingTurns(context)
            "MIX_FANCY" -> freeCrafts += freeCocktailcraftingTurns(context)
        }
        return freeCrafts
    }
}
