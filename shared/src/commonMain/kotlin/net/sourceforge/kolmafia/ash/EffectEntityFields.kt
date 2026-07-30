package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.CandyEffectTier
import net.sourceforge.kolmafia.data.EffectDefinitionProxy
import net.sourceforge.kolmafia.data.GameDatabase

/**
 * Resolves `$effect[field]` bracket access. Mirrors desktop EffectProxy metadata
 * and default-action helpers.
 */
internal object EffectEntityFields {

    private val stringArrayType = AggregateType(AshType.INT, AshType.STRING)

    fun resolve(
        effectRef: String,
        fieldName: String,
        gameDatabase: GameDatabase?,
    ): AshValue {
        val effect = EffectDefinitionProxy.getByIdOrName(effectRef)
            ?: gameDatabase?.effect(effectRef)
        val effectId = effect?.id ?: EffectDefinitionProxy.resolveEffectId(effectRef)
        val effectName = effect?.name ?: effectRef

        return when (fieldName.lowercase()) {
            "id" -> AshValue.of(effectId.toLong())
            "name" -> AshValue.of(effectName)
            "default" -> AshValue.of(EffectDefinitionProxy.getDefaultAction(effectId) ?: "")
            "note" -> AshValue.of(EffectDefinitionProxy.getActionNote(effectId) ?: "")
            "all" -> stringListAggregate(EffectDefinitionProxy.getAllActions(effectId))
            "image" -> AshValue.of(effect?.image ?: "")
            "descid" -> AshValue.of(effect?.descId ?: "")
            "candy_tier" -> AshValue.of(CandyEffectTier.getEffectTier(effectId).toLong())
            "quality" -> AshValue.of(effect?.qualityDescription() ?: "")
            "attributes" -> AshValue.of(effect?.combinedAttributes() ?: "")
            "song" -> AshValue.of(effect?.isSong() ?: false)
            else -> throw ScriptException("effect has no field '$fieldName'")
        }
    }

    private fun stringListAggregate(values: List<String>): AggregateValue {
        val result = AggregateValue(stringArrayType)
        values.forEachIndexed { i, value ->
            result[AshValue.of(i)] = AshValue.of(value)
        }
        return result
    }
}
