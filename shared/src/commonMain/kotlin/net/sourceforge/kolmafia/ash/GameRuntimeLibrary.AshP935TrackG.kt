package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.FactDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.modifiers.ModifierExpression
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.StringModifier

/**
 * AshP935–942 Track G — Modifiers / expression eval / facts.
 *
 * Phase 935: monster_modifier (effect+string, effect+modifier)
 * Phase 936: effect_modifier / class_modifier / skill_modifier / stat_modifier
 * Phase 937: expression_eval / modifier_eval / monster_eval
 * Phase 938–942: fact_type / item_fact / effect_fact / numeric_fact / string_fact
 */
internal fun GameRuntimeLibrary.registerAshP935TrackGBatch(scope: AshScope) {
    // ── Phase 935: monster_modifier ────────────────────────────────
    regFn(scope, "monster_modifier", AshType.MONSTER,
        listOf("effect" to AshType.EFFECT, "modifier" to AshType.STRING)) { _, args ->
        val effectName = args[0].toString()
        val modTag = args[1].toString()
        val entry = ModifierDatabase.getEffect(effectName) ?: return@regFn AshValue(AshType.MONSTER, "")
        val monsterName = resolveStringModifierValue(entry, modTag)
        AshValue(AshType.MONSTER, monsterName)
    }

    regFn(scope, "monster_modifier", AshType.MONSTER,
        listOf("effect" to AshType.EFFECT, "modifier" to AshType.MODIFIER)) { _, args ->
        val effectName = args[0].toString()
        val modTag = args[1].toString()
        val entry = ModifierDatabase.getEffect(effectName) ?: return@regFn AshValue(AshType.MONSTER, "")
        val monsterName = resolveStringModifierValue(entry, modTag)
        AshValue(AshType.MONSTER, monsterName)
    }

    // ── Phase 936: typed modifier lookups ──────────────────────────
    regFn(scope, "effect_modifier", AshType.EFFECT,
        listOf("item" to AshType.ITEM, "modifier" to AshType.STRING)) { _, args ->
        val itemName = args[0].toString()
        val modTag = args[1].toString()
        val entry = ModifierDatabase.getItem(itemName) ?: return@regFn AshValue.effect("")
        AshValue.effect(resolveStringModifierValue(entry, modTag))
    }

    regFn(scope, "effect_modifier", AshType.EFFECT,
        listOf("item" to AshType.ITEM, "modifier" to AshType.MODIFIER)) { _, args ->
        val itemName = args[0].toString()
        val modTag = args[1].toString()
        val entry = ModifierDatabase.getItem(itemName) ?: return@regFn AshValue.effect("")
        AshValue.effect(resolveStringModifierValue(entry, modTag))
    }

    regFn(scope, "class_modifier", AshType.CLASS,
        listOf("item" to AshType.ITEM, "modifier" to AshType.STRING)) { _, args ->
        val itemName = args[0].toString()
        val modTag = args[1].toString()
        val entry = ModifierDatabase.getItem(itemName) ?: return@regFn AshValue(AshType.CLASS, "")
        AshValue(AshType.CLASS, resolveStringModifierValue(entry, modTag))
    }

    regFn(scope, "skill_modifier", AshType.SKILL,
        listOf("item" to AshType.ITEM, "modifier" to AshType.STRING)) { _, args ->
        val itemName = args[0].toString()
        val modTag = args[1].toString()
        val entry = ModifierDatabase.getItem(itemName) ?: return@regFn AshValue.skill("")
        AshValue.skill(resolveStringModifierValue(entry, modTag))
    }

    regFn(scope, "skill_modifier", AshType.SKILL,
        listOf("item" to AshType.ITEM, "modifier" to AshType.MODIFIER)) { _, args ->
        val itemName = args[0].toString()
        val modTag = args[1].toString()
        val entry = ModifierDatabase.getItem(itemName) ?: return@regFn AshValue.skill("")
        AshValue.skill(resolveStringModifierValue(entry, modTag))
    }

    regFn(scope, "stat_modifier", AshType.STAT,
        listOf("effect" to AshType.EFFECT, "modifier" to AshType.STRING)) { _, args ->
        val effectName = args[0].toString()
        val modTag = args[1].toString()
        val entry = ModifierDatabase.getEffect(effectName) ?: return@regFn AshValue(AshType.STAT, "")
        AshValue(AshType.STAT, resolveStringModifierValue(entry, modTag))
    }

    regFn(scope, "stat_modifier", AshType.STAT,
        listOf("effect" to AshType.EFFECT, "modifier" to AshType.MODIFIER)) { _, args ->
        val effectName = args[0].toString()
        val modTag = args[1].toString()
        val entry = ModifierDatabase.getEffect(effectName) ?: return@regFn AshValue(AshType.STAT, "")
        AshValue(AshType.STAT, resolveStringModifierValue(entry, modTag))
    }

    // ── Phase 937: expression_eval / modifier_eval / monster_eval ──
    regFn(scope, "expression_eval", AshType.FLOAT, listOf("expr" to AshType.STRING)) { _, args ->
        val expr = args[0].toString()
        val ctx = liveExpressionContext()
        val result = ModifierExpression(expr).evaluate(ctx)
        AshValue.of(result)
    }

    regFn(scope, "modifier_eval", AshType.FLOAT, listOf("modifier" to AshType.STRING)) { _, args ->
        val expr = args[0].toString()
        val ctx = liveExpressionContext()
        val result = ModifierExpression.evaluate("[$expr]", ctx)
        AshValue.of(result)
    }

    regFn(scope, "monster_eval", AshType.FLOAT, listOf("expr" to AshType.STRING)) { _, args ->
        val expr = args[0].toString()
        val ctx = liveExpressionContext()
        val result = ModifierExpression(expr).evaluate(ctx)
        AshValue.of(result)
    }

    // ── Phase 938–942: Fact free functions ──────────────────────────
    regFn(scope, "fact_type", AshType.STRING, listOf("monster" to AshType.MONSTER)) { _, args ->
        val monsterName = args[0].toString()
        val monsterDef = MonsterDatabase.getByName(monsterName) ?: return@regFn AshValue.EMPTY_STRING
        val state = character?.state?.value ?: CharacterState()
        AshValue.of(FactDatabase.factTypeString(monsterDef, state.characterClassEnum, state.ascensionPath, null))
    }

    regFn(scope, "item_fact", AshType.ITEM, listOf("monster" to AshType.MONSTER)) { _, args ->
        val monsterName = args[0].toString()
        val monsterDef = MonsterDatabase.getByName(monsterName) ?: return@regFn AshValue.item("")
        val state = character?.state?.value ?: CharacterState()
        val fact = FactDatabase.getFact(monsterDef, state.characterClassEnum, state.ascensionPath, true, null)
        if (fact.type.toString() == "item") AshValue.item(fact.display) else AshValue.item("")
    }

    regFn(scope, "effect_fact", AshType.EFFECT, listOf("monster" to AshType.MONSTER)) { _, args ->
        val monsterName = args[0].toString()
        val monsterDef = MonsterDatabase.getByName(monsterName) ?: return@regFn AshValue.effect("")
        val state = character?.state?.value ?: CharacterState()
        val fact = FactDatabase.getFact(monsterDef, state.characterClassEnum, state.ascensionPath, true, null)
        if (fact.type.toString() == "effect") AshValue.effect(fact.display) else AshValue.effect("")
    }

    regFn(scope, "numeric_fact", AshType.INT, listOf("monster" to AshType.MONSTER)) { _, args ->
        val monsterName = args[0].toString()
        val monsterDef = MonsterDatabase.getByName(monsterName) ?: return@regFn AshValue.ZERO
        val state = character?.state?.value ?: CharacterState()
        val fact = FactDatabase.getFact(monsterDef, state.characterClassEnum, state.ascensionPath, true, null)
        val typeStr = fact.type.toString()
        if (typeStr == "meat" || typeStr == "stat" || typeStr == "hp") {
            AshValue.of(fact.display.filter { it.isDigit() || it == '-' }.toLongOrNull() ?: 0L)
        } else AshValue.ZERO
    }

    regFn(scope, "string_fact", AshType.STRING, listOf("monster" to AshType.MONSTER)) { _, args ->
        val monsterName = args[0].toString()
        val monsterDef = MonsterDatabase.getByName(monsterName) ?: return@regFn AshValue.EMPTY_STRING
        val state = character?.state?.value ?: CharacterState()
        AshValue.of(FactDatabase.factString(monsterDef, state.characterClassEnum, state.ascensionPath, null))
    }
}

private fun GameRuntimeLibrary.liveExpressionContext(): ExpressionContext {
    val state = character?.state?.value ?: return ExpressionContext.EMPTY
    val effects = effectManager?.state?.value?.effects ?: emptyList()
    return ExpressionContext.from(state, effects)
}

private fun resolveStringModifierValue(entry: ModifierEntry, modTag: String): String {
    val sm = StringModifier.byTag(modTag)
    if (sm != null) {
        val parsed = ModifierParser.parse(entry.modifiers)
        return parsed.strings[sm]?.firstOrNull().orEmpty()
    }
    return ""
}
