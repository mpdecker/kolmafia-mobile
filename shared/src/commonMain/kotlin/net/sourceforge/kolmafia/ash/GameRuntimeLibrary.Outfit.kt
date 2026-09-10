package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.OutfitDatabase
import net.sourceforge.kolmafia.equipment.ResolvedOutfit

internal fun GameRuntimeLibrary.registerOutfitFunctions(scope: AshScope) {

    val itemArrayType = AggregateType(AshType.INT, AshType.ITEM)
    val stringArrayType = AggregateType(AshType.INT, AshType.STRING)
    val intStringMapType = AggregateType(AshType.INT, AshType.STRING)
    // Desktop AggregateType(FLOAT, ITEM) → map keyed by ITEM, value FLOAT
    val itemFloatMapType = AggregateType(AshType.ITEM, AshType.FLOAT)

    fun resolveOutfit(name: String): ResolvedOutfit? =
        outfitManager?.getMatchingOutfit(name)
            ?: OutfitDatabase.getByName(name)?.let { ResolvedOutfit(it.id, it.name, it.equipment) }

    fun executeEmbeddedCli(cmd: String, runtime: AshRuntimeContext) {
        dispatchCli(cmd, runtime)
    }

    // Desktop outfit(string) → BOOLEAN continueValue
    regFn(scope, "outfit", AshType.BOOLEAN, listOf("name" to AshType.STRING)) { runtime, args ->
        val name = args[0].toString()
        val ok = kotlinx.coroutines.runBlocking {
            outfitManager?.wearOutfit(name) { cmd -> executeEmbeddedCli(cmd, runtime) } == true
        }
        AshValue.of(ok)
    }

    // Desktop: id < 0 || EquipmentManager.hasOutfit(id)
    regFn(scope, "have_outfit", AshType.BOOLEAN, listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val manager = outfitManager
        val outfit = manager?.getMatchingOutfit(name)
            ?: OutfitDatabase.getByName(name)?.let { ResolvedOutfit(it.id, it.name, it.equipment) }
            ?: return@regFn AshValue.FALSE
        if (outfit.id < 0) return@regFn AshValue.TRUE
        if (manager == null) return@regFn AshValue.FALSE
        val has = kotlinx.coroutines.runBlocking { manager.hasOutfit(outfit.id) }
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
        if (outfit == null) {
            return@regFn AggregateValue(AggregateType(AshType.INT, AshType.ITEM, fixedSize = 0))
        }
        val pieces = outfit.pieces
        val result = AggregateValue(AggregateType(AshType.INT, AshType.ITEM, fixedSize = pieces.size))
        pieces.forEachIndexed { index, piece ->
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

    regFn(scope, "outfit_treats", itemFloatMapType, listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val outfit = resolveOutfit(name)
        val result = AggregateValue(itemFloatMapType)
        if (outfit != null) {
            val treats = outfitManager?.treatChances(outfit)
                ?: OutfitDatabase.getById(outfit.id)?.let { data ->
                    data.halloweenDrops.mapNotNull { drop ->
                        val trimmed = drop.trim()
                        when {
                            trimmed.equals("none", ignoreCase = true) || trimmed.isEmpty() -> null
                            else -> {
                                val paren = Regex("""^(.+?)\s*\(([\d.]+)\)\s*$""").find(trimmed)
                                if (paren != null) {
                                    paren.groupValues[1].trim() to
                                        (paren.groupValues[2].toDoubleOrNull() ?: 1.0)
                                } else {
                                    trimmed to 1.0
                                }
                            }
                        }
                    }
                }.orEmpty()
            for ((treat, chance) in treats) {
                result[AshValue.item(treat)] = AshValue.of(chance)
            }
        }
        result
    }

    // Desktop outfitListToValue is 1-based (skips index 0)
    regFn(scope, "get_outfits", stringArrayType, emptyList()) { _, _ ->
        val result = AggregateValue(stringArrayType)
        val outfits = kotlinx.coroutines.runBlocking {
            outfitManager?.getOutfitsWithPieces() ?: emptyList()
        }
        outfits.forEachIndexed { index, outfit ->
            result[AshValue.of((index + 1).toLong())] = AshValue.of(outfit.name)
        }
        result
    }

    regFn(scope, "get_custom_outfits", stringArrayType, emptyList()) { _, _ ->
        val result = AggregateValue(stringArrayType)
        OutfitDatabase.customOutfits().forEachIndexed { index, outfit ->
            result[AshValue.of((index + 1).toLong())] = AshValue.of(outfit.name)
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
