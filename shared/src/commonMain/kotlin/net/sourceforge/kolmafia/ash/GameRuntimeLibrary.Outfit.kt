package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.equipment.ResolvedOutfit

internal fun GameRuntimeLibrary.registerOutfitFunctions(scope: AshScope) {

    val itemArrayType = AggregateType(AshType.INT, AshType.ITEM)
    val stringArrayType = AggregateType(AshType.INT, AshType.STRING)
    val intStringMapType = AggregateType(AshType.INT, AshType.STRING)
    val floatItemMapType = AggregateType(AshType.FLOAT, AshType.ITEM)

    fun resolveOutfit(name: String): ResolvedOutfit? =
        outfitManager?.getMatchingOutfit(name)
            ?: OutfitDatabase.getByName(name)?.let { ResolvedOutfit(it.id, it.name, it.equipment) }

    fun executeEmbeddedCli(cmd: String, runtime: AshRuntimeContext) {
        dispatchCli(cmd, runtime)
    }

    regFn(scope, "outfit", AshType.VOID, listOf("name" to AshType.STRING)) { runtime, args ->
        val name = args[0].toString()
        kotlinx.coroutines.runBlocking {
            outfitManager?.wearOutfit(name) { cmd -> executeEmbeddedCli(cmd, runtime) }
        }
        AshValue.VOID
    }

    regFn(scope, "have_outfit", AshType.BOOLEAN, listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val manager = outfitManager
        if (manager == null) return@regFn AshValue.FALSE
        val outfit = manager.getMatchingOutfit(name) ?: return@regFn AshValue.FALSE
        if (outfit.isCustom) {
            return@regFn AshValue.TRUE
        }
        val has = kotlinx.coroutines.runBlocking { manager.hasAllPieces(outfit) }
        AshValue.of(has)
    }

    regFn(scope, "retrieve_outfit", AshType.BOOLEAN, listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val manager = outfitManager ?: return@regFn AshValue.FALSE
        val outfit = manager.getMatchingOutfit(name) ?: return@regFn AshValue.FALSE
        val ok = kotlinx.coroutines.runBlocking { manager.retrieveOutfit(outfit) }
        AshValue.of(ok)
    }

    regFn(scope, "is_wearing_outfit", AshType.BOOLEAN, listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val manager = outfitManager ?: return@regFn AshValue.FALSE
        val outfit = manager.getMatchingOutfit(name) ?: return@regFn AshValue.FALSE
        AshValue.of(manager.isWearingOutfit(outfit))
    }

    regFn(scope, "outfit_pieces", itemArrayType, listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val outfit = resolveOutfit(name)
        val result = AggregateValue(itemArrayType)
        outfit?.pieces?.forEachIndexed { index, piece ->
            result[AshValue.of(index.toLong())] = AshValue.item(piece)
        }
        result
    }

    regFn(scope, "outfit_tattoo", AshType.STRING, listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val outfit = resolveOutfit(name)
        val image = outfit?.let { OutfitDatabase.getById(it.id)?.image }.orEmpty()
        AshValue.of(image)
    }

    regFn(scope, "outfit_treats", floatItemMapType, listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val outfit = resolveOutfit(name)
        val result = AggregateValue(floatItemMapType)
        if (outfit != null) {
            for ((treat, chance) in outfitManager?.treatChances(outfit).orEmpty()) {
                result[AshValue.of(chance)] = AshValue.item(treat)
            }
        }
        result
    }

    regFn(scope, "get_outfits", stringArrayType, emptyList()) { _, _ ->
        val result = AggregateValue(stringArrayType)
        val outfits = kotlinx.coroutines.runBlocking {
            outfitManager?.getOutfitsWithPieces() ?: emptyList()
        }
        outfits.forEachIndexed { index, outfit ->
            result[AshValue.of(index.toLong())] = AshValue.of(outfit.name)
        }
        result
    }

    regFn(scope, "get_custom_outfits", stringArrayType, emptyList()) { _, _ ->
        val result = AggregateValue(stringArrayType)
        OutfitDatabase.customOutfits().forEachIndexed { index, outfit ->
            result[AshValue.of(index.toLong())] = AshValue.of(outfit.name)
        }
        result
    }

    regFn(scope, "all_normal_outfits", intStringMapType, emptyList()) { _, _ ->
        val result = AggregateValue(intStringMapType)
        for (outfit in OutfitDatabase.all()) {
            result[AshValue.of(outfit.id.toLong())] = AshValue.of(outfit.name)
        }
        result
    }
}
