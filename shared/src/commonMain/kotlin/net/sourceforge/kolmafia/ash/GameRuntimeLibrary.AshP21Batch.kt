package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.ModifierEntry

/**
 * ASH-P21 behavioral batch — live type:name modifier lookups (Outfit, Loc, Path, etc.).
 */
internal fun GameRuntimeLibrary.registerAshP21Batch(scope: AshScope) {
    val typeNameModifierParams = listOf("type" to AshType.STRING, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, typeNameModifierParams) { _, args ->
        val entry = resolveModifierByTypeNameArg(args[0].toString())
        AshValue.of(numericFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, typeNameModifierParams) { _, args ->
        val entry = resolveModifierByTypeNameArg(args[0].toString())
        AshValue.of(booleanFromEntry(entry, args[1].toString()))
    }
    regFn(scope, "string_modifier", AshType.STRING, typeNameModifierParams) { _, args ->
        val entry = resolveModifierByTypeNameArg(args[0].toString())
        AshValue.of(stringFromEntry(entry, args[1].toString()))
    }
}

internal fun GameRuntimeLibrary.resolveModifierByTypeNameArg(typeNameArg: String): ModifierEntry? {
    val colon = typeNameArg.indexOf(':')
    if (colon == -1) {
        return gameDatabase?.itemModifier(typeNameArg)
            ?: typeNameArg.toIntOrNull()?.let { gameDatabase?.itemModifier(it) }
    }
    val type = typeNameArg.substring(0, colon)
    val name = typeNameArg.substring(colon + 1)
    return resolveModifierByTypeName(type, name)
}

internal fun GameRuntimeLibrary.resolveModifierByTypeName(type: String, name: String): ModifierEntry? {
    val db = gameDatabase
    return when (type.lowercase()) {
        "item" -> db?.itemModifier(name) ?: name.toIntOrNull()?.let { db?.itemModifier(it) }
            ?: ModifierDatabase.getItem(name)
        "effect" -> db?.effectModifier(name) ?: ModifierDatabase.getEffect(name)
        "skill" -> db?.skillModifier(name) ?: name.toIntOrNull()?.let { db?.skillModifier(it) }
            ?: ModifierDatabase.getSkill(name)
        "familiar" -> db?.familiarModifier(name) ?: name.toIntOrNull()?.let { db?.familiarModifier(it) }
            ?: ModifierDatabase.getFamiliar(name)
        "loc", "location" -> db?.locationModifier(name) ?: ModifierDatabase.getLocation(name)
        "zone" -> db?.zoneModifier(name) ?: ModifierDatabase.get("Zone", name)
        "path" -> db?.pathModifier(name) ?: ModifierDatabase.get("Path", name)
        "thrall" -> db?.thrallModifier(name) ?: ModifierDatabase.get("Thrall", name)
        "outfit" -> db?.outfitModifier(name) ?: ModifierDatabase.get("Outfit", name)
        "sign" -> db?.modifier("Sign", name) ?: ModifierDatabase.get("Sign", name)
        "generated" -> ModifierDatabase.get("Generated", name)
        "monster" -> db?.modifier("Monster", name) ?: ModifierDatabase.get("Monster", name)
        else -> db?.modifier(type, name)
            ?: type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                .let { normalized -> db?.modifier(normalized, name) ?: ModifierDatabase.get(normalized, name) }
            ?: ModifierDatabase.get(type, name)
    }
}
