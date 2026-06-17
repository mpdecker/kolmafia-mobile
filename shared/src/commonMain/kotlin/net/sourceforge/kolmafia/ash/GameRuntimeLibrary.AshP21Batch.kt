package net.sourceforge.kolmafia.ash

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
    val db = gameDatabase ?: return null
    return when (type.lowercase()) {
        "item" -> db.itemModifier(name) ?: name.toIntOrNull()?.let { db.itemModifier(it) }
        "effect" -> db.effectModifier(name)
        "skill" -> db.skillModifier(name) ?: name.toIntOrNull()?.let { db.skillModifier(it) }
        "familiar" -> db.familiarModifier(name) ?: name.toIntOrNull()?.let { db.familiarModifier(it) }
        "loc", "location" -> db.locationModifier(name)
        "zone" -> db.zoneModifier(name)
        "path" -> db.pathModifier(name)
        "thrall" -> db.thrallModifier(name)
        "outfit" -> db.outfitModifier(name)
        "sign" -> db.modifier("Sign", name)
        else -> db.modifier(type, name)
            ?: type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                .let { normalized -> db.modifier(normalized, name) }
    }
}
