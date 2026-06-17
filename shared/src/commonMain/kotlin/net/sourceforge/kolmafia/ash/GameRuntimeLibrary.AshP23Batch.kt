package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ModifierEntry
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser

/**
 * ASH-P23 behavioral batch — live ELEMENT entity modifiers and no-arg numerics_modifier.
 */
internal fun GameRuntimeLibrary.registerAshP23Batch(scope: AshScope) {
    val elementModifierParams = listOf("value" to AshType.ELEMENT, "modifier" to AshType.STRING)
    regFn(scope, "numeric_modifier", AshType.FLOAT, elementModifierParams) { _, args ->
        val resistance = elementalResistanceModifier(args[0].toString())
        AshValue.of(
            if (resistance != null) buildCurrentModifiers().values.get(resistance) else 0.0,
        )
    }
    regFn(scope, "boolean_modifier", AshType.BOOLEAN, elementModifierParams) { _, _ ->
        AshValue.FALSE
    }
    regFn(scope, "string_modifier", AshType.STRING, elementModifierParams) { _, _ ->
        AshValue.EMPTY_STRING
    }

    regFn(scope, "numerics_modifier", floatAggregateType, listOf("modifier" to AshType.STRING)) { _, args ->
        val tag = args[0].toString()
        val dm = DoubleModifier.byTag(tag)
        if (dm == null || !dm.multiple) {
            return@regFn buildEmptyFloatAggregate()
        }
        buildFloatAggregate(currentMultiDoubleValues(dm))
    }
}

internal fun elementalResistanceModifier(elementName: String): DoubleModifier? =
    when (elementName.lowercase().trim()) {
        "cold" -> DoubleModifier.COLD_RESISTANCE
        "hot" -> DoubleModifier.HOT_RESISTANCE
        "sleaze" -> DoubleModifier.SLEAZE_RESISTANCE
        "spooky" -> DoubleModifier.SPOOKY_RESISTANCE
        "stench" -> DoubleModifier.STENCH_RESISTANCE
        "slime" -> DoubleModifier.SLIME_RESISTANCE
        "supercold" -> DoubleModifier.SUPERCOLD_RESISTANCE
        else -> null
    }

internal fun GameRuntimeLibrary.currentMultiDoubleValues(modifier: DoubleModifier): List<Double> =
    when (modifier) {
        DoubleModifier.EFFECT_DURATION ->
            effectManager?.state?.value?.effects
                ?.filter { it.duration >= 0 }
                ?.map { it.duration.toDouble() }
                ?: emptyList()
        DoubleModifier.ROLLOVER_EFFECT_DURATION ->
            effectManager?.state?.value?.effects
                ?.mapNotNull { effect ->
                    rolloverDurationFromEffect(effect.name)
                }
                ?: emptyList()
        else -> emptyList()
    }

private fun GameRuntimeLibrary.rolloverDurationFromEffect(effectName: String): Double? {
    val entry: ModifierEntry = gameDatabase?.effectModifier(effectName) ?: return null
    return ModifierParser.parse(entry.modifiers).get(DoubleModifier.ROLLOVER_EFFECT_DURATION)
        .takeIf { it != 0.0 }
}

private val floatAggregateType = AggregateType(AshType.INT, AshType.FLOAT)

internal fun buildFloatAggregate(values: List<Double>): AggregateValue {
    val result = AggregateValue(floatAggregateType)
    values.forEachIndexed { index, value ->
        result[AshValue.of(index.toLong())] = AshValue.of(value)
    }
    return result
}

internal fun buildEmptyFloatAggregate(): AggregateValue = AggregateValue(floatAggregateType)
