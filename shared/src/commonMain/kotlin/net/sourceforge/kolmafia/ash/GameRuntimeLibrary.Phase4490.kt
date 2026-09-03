package net.sourceforge.kolmafia.ash

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EffectQuality
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.session.StoreManager
import net.sourceforge.kolmafia.skill.UseSkillSync
import net.sourceforge.kolmafia.utilities.PHPMTRandom

/**
 * Phases 4481–4490 — ASH behavioral deepen VIII (corpus-driven missing / wrong stubs).
 *
 * 4481 last_skill_message · 4482 eight_bit_points · 4483 get_no_pulls · 4484 get_items_hash ·
 * 4485 beret_busking_effects · 4486 image_to_monster · 4487 dart_parts_to_skills (AshP997) ·
 * 4488 extract_items (AshP991) · 4489 sells_skill/sell_cost (AshP985/Coinmaster) ·
 * 4490 path_name_to_id / path_id_to_name
 */
internal fun GameRuntimeLibrary.registerPhase4490(scope: AshScope) {
    // ── 4481: last_skill_message ──────────────────────────────────
    regFn(scope, "last_skill_message", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(UseSkillSync.lastUpdate)
    }

    // ── 4482: eight_bit_points ────────────────────────────────────
    data class EightBitZone(val mod: DoubleModifier, val base: Int, val color: String)
    val eightBitZones = mapOf(
        563 to EightBitZone(DoubleModifier.MEATDROP, 150, "red"),
        564 to EightBitZone(DoubleModifier.ITEMDROP, 100, "green"),
        565 to EightBitZone(DoubleModifier.INITIATIVE, 300, "black"),
        566 to EightBitZone(DoubleModifier.DAMAGE_ABSORPTION, 300, "blue"),
    )
    fun eightBitPoints(zone: EightBitZone, color: String, modValue: Double): Long {
        val isBonus = zone.color.equals(color, ignoreCase = true)
        val base = if (isBonus) 100 else 50
        val divisor = if (isBonus) 10.0 else 20.0
        val bonus = (min(300.0, max(0.0, modValue - zone.base)) / divisor).roundToLong() * 10
        return base + bonus
    }
    fun resolveEightBitZone(locationName: String): EightBitZone? {
        val zone = AdventureDatabase.getByName(locationName) ?: return null
        val snarf = zone.snarfblat?.toIntOrNull() ?: zone.adventureId.toIntOrNull() ?: return null
        return eightBitZones[snarf]
    }
    regFn(scope, "eight_bit_points", AshType.INT, listOf("loc" to AshType.LOCATION)) { _, args ->
        val zone = resolveEightBitZone(args[0].toString()) ?: return@regFn AshValue.ZERO
        val color = preferences?.getString("8BitColor", "").orEmpty()
        val modValue = buildCurrentModifiers().values.get(zone.mod)
        AshValue.of(eightBitPoints(zone, color, modValue))
    }
    regFn(
        scope,
        "eight_bit_points",
        AshType.INT,
        listOf("loc" to AshType.LOCATION, "color" to AshType.STRING, "mod" to AshType.FLOAT),
    ) { _, args ->
        val zone = resolveEightBitZone(args[0].toString()) ?: return@regFn AshValue.ZERO
        AshValue.of(eightBitPoints(zone, args[1].toString(), args[2].toDouble()))
    }

    // ── 4483: get_no_pulls ────────────────────────────────────────
    val itemToInt = AggregateType(AshType.ITEM, AshType.INT)
    regFn(scope, "get_no_pulls", itemToInt, emptyList()) { _, _ ->
        val result = AggregateValue(itemToInt)
        inventoryManager?.state?.value?.items?.values?.forEach { item ->
            if (StoragePullRules.isNoPull(item.itemId)) {
                result[AshValue.item(item.name)] = AshValue.of(item.quantity.toLong())
            }
        }
        // Also include cached storage nopulls when present.
        preferences?.let { prefs ->
            for ((itemId, qty) in CollectionCache.load(prefs, Preferences.CACHED_STORAGE)) {
                if (!StoragePullRules.isNoPull(itemId)) continue
                val name = ItemDatabase.getById(itemId)?.name ?: continue
                val existing = result.map[AshValue.item(name)]?.toLong() ?: 0L
                result[AshValue.item(name)] = AshValue.of(existing + qty)
            }
        }
        result
    }

    // ── 4484: get_items_hash ──────────────────────────────────────
    regFn(scope, "get_items_hash", AshType.INT, listOf("source" to AshType.STRING)) { _, args ->
        AshValue.of(itemsHash(args[0].toString()))
    }

    // ── 4485: beret_busking_effects ───────────────────────────────
    val effectToInt = AggregateType(AshType.EFFECT, AshType.INT)
    fun beretBuskingEffects(power: Long, cast: Long): AggregateValue {
        val result = AggregateValue(effectToInt)
        val capped = min(power, 1100L) +
            floor(max(0.0, (power - 1100).toDouble()).pow(0.8)).toLong()
        // $effect[none] holds meat gained
        result[AshValue.effect("none")] = AshValue.of(ceil(capped / 5.0).toLong() + 1)
        val valid = EffectDatabase.all()
            .asSequence()
            .filter { it.id in 1..2990 }
            .filter { it.quality == EffectQuality.GOOD }
            .filter { !it.attributes.contains("nohookah") || it.id == 549 }
            .filter { !it.attributes.contains("notcrs") }
            .map { it.id }
            .toMutableList()
        if (valid.isEmpty()) return result
        valid.add(valid.last())
        val rng = PHPMTRandom(capped + cast)
        val total = ceil(capped / 100.0).toInt()
        val counts = linkedMapOf<Int, Int>()
        repeat(total) {
            val effectId = rng.pickOne(valid)
            val duration = if (effectId == 549) 1 else 10
            counts[effectId] = (counts[effectId] ?: 0) + duration
        }
        for ((effectId, duration) in counts) {
            val name = EffectDatabase.getById(effectId)?.name ?: continue
            result[AshValue.effect(name)] = AshValue.of(duration.toLong())
        }
        return result
    }
    regFn(scope, "beret_busking_effects", effectToInt, emptyList()) { _, _ ->
        val prefs = preferences
        val power = (
            prefs?.getInt("beretPower", 0)?.takeIf { it > 0 }
                ?: prefs?.getInt("_beretBuskingPower", 0)
                ?: 0
            ).toLong()
        val cast = (prefs?.getInt("_beretBuskingUses", 0) ?: 0).toLong()
        beretBuskingEffects(power, cast)
    }
    regFn(
        scope,
        "beret_busking_effects",
        effectToInt,
        listOf("power" to AshType.INT, "cast" to AshType.INT),
    ) { _, args ->
        beretBuskingEffects(args[0].toLong(), args[1].toLong())
    }

    // ── 4486: image_to_monster ────────────────────────────────────
    regFn(scope, "image_to_monster", AshType.MONSTER, listOf("image" to AshType.STRING)) { _, args ->
        val monster = MonsterDatabase.findByImage(args[0].toString())
        AshValue(AshType.MONSTER, monster?.name ?: "none")
    }

    // ── 4490: path_name_to_id / path_id_to_name ───────────────────
    regFn(scope, "path_name_to_id", AshType.INT, listOf("name" to AshType.STRING)) { _, args ->
        val path = AscensionPath.fromApiString(args[0].toString())
        AshValue.of(
            if (path == AscensionPath.UNKNOWN || path == AscensionPath.NONE) -1L
            else path.pathId.toLong(),
        )
    }
    regFn(scope, "path_id_to_name", AshType.STRING, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        val name = AscensionPath.entries.firstOrNull {
            it.pathId == id && it != AscensionPath.UNKNOWN && it != AscensionPath.NONE && it != AscensionPath.HARDCORE
        }?.apiName.orEmpty()
        AshValue.of(name)
    }
}

private const val FNV_INIT = -0x340d631b7bdddcdbL // Java 0xcbf29ce484222325L
private const val FNV_PRIME = 0x100000001b3L // Java 0x00000100000001b3L

private fun fnvHash(init: Long, n: Int): Long {
    var hash = init
    var v = n
    repeat(4) {
        hash = hash xor (v and 0xFF).toLong()
        hash *= FNV_PRIME
        v = v ushr 8
    }
    return hash
}

private fun GameRuntimeLibrary.itemsHash(source: String): Long {
    var hash = FNV_INIT
    when (source.lowercase()) {
        "shop" -> {
            for (sold in StoreManager.getSoldItemList()) {
                hash = fnvHash(hash, sold.itemId)
                hash = fnvHash(hash, sold.quantity)
                hash = fnvHash(hash, sold.price.toInt())
                hash = fnvHash(hash, (sold.price ushr 32).toInt())
                hash = fnvHash(hash, sold.limit)
            }
        }
        "inventory" -> {
            inventoryManager?.state?.value?.items?.values?.forEach { item ->
                hash = fnvHash(hash, item.itemId)
                hash = fnvHash(hash, item.quantity)
            }
        }
        "closet" -> hash = hashCollection(Preferences.CACHED_CLOSET, hash)
        "storage" -> hash = hashCollection(Preferences.CACHED_STORAGE, hash)
        "display" -> hash = hashCollection(Preferences.CACHED_DISPLAY, hash)
        else -> Unit
    }
    return hash
}

private fun GameRuntimeLibrary.hashCollection(prefKey: String, init: Long): Long {
    var hash = init
    val prefs = preferences ?: return hash
    for ((itemId, qty) in CollectionCache.load(prefs, prefKey)) {
        hash = fnvHash(hash, itemId)
        hash = fnvHash(hash, qty)
    }
    return hash
}
