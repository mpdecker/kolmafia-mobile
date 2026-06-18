package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.StringModifier

/**
 * ASH-P22 behavioral batch — live current-character modifier lookups via [CurrentModifiers].
 */
internal fun GameRuntimeLibrary.registerAshP22Batch(scope: AshScope) {
    regFn(scope, "numeric_modifier", AshType.FLOAT, listOf("modifier" to AshType.STRING)) { _, args ->
        val tag = args[0].toString()
        val dm = DoubleModifier.byTag(tag) ?: return@regFn AshValue.of(0.0)
        AshValue.of(buildCurrentModifiers().values.get(dm))
    }

    regFn(scope, "boolean_modifier", AshType.BOOLEAN, listOf("modifier" to AshType.STRING)) { _, args ->
        val tag = args[0].toString()
        val bm = BooleanModifier.byTag(tag) ?: return@regFn AshValue.FALSE
        AshValue.of(buildCurrentModifiers().values.get(bm))
    }

    regFn(scope, "string_modifier", AshType.STRING, listOf("modifier" to AshType.STRING)) { _, args ->
        val tag = args[0].toString()
        val sm = StringModifier.byTag(tag) ?: return@regFn AshValue.EMPTY_STRING
        AshValue.of(buildCurrentModifiers().values.get(sm).orEmpty())
    }
}
