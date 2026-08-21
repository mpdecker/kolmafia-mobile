package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.modifiers.DoubleModifier

/**
 * AshP911–AshP918 — Clan / shop / session ASH surface (Track D).
 */

// ── AshP911 — get_clan_lounge, get_clan_rumpus, get_chateau ────────────────

internal fun GameRuntimeLibrary.registerAshP911Batch(scope: AshScope) {
    val itemToInt = AggregateType(AshType.ITEM, AshType.INT)
    val stringToInt = AggregateType(AshType.STRING, AshType.INT)

    regFn(scope, "get_clan_lounge", itemToInt, emptyList()) { _, _ ->
        val result = AggregateValue(itemToInt)
        val loungeItems = preferences?.getString("clanLounge", "")?.takeIf { it.isNotBlank() }
        if (loungeItems != null) {
            for (entry in loungeItems.split("|").filter { it.isNotBlank() }) {
                val parts = entry.split(":")
                val name = parts.getOrNull(0) ?: continue
                val count = parts.getOrNull(1)?.toIntOrNull() ?: 1
                result[AshValue.item(name)] = AshValue.of(count.toLong())
            }
        }
        result
    }

    regFn(scope, "get_clan_rumpus", stringToInt, emptyList()) { _, _ ->
        val result = AggregateValue(stringToInt)
        val rumpus = preferences?.getString("clanRumpus", "")?.takeIf { it.isNotBlank() }
        if (rumpus != null) {
            for (entry in rumpus.split("|").filter { it.isNotBlank() }) {
                val countIdx = entry.indexOf(" (")
                if (countIdx != -1) {
                    val name = entry.substring(0, countIdx)
                    val count = entry.substring(countIdx + 2, entry.length - 1).toIntOrNull() ?: 1
                    result[AshValue.of(name)] = AshValue.of(count.toLong())
                } else {
                    result[AshValue.of(entry)] = AshValue.of(1L)
                }
            }
        }
        result
    }

    regFn(scope, "get_chateau", itemToInt, emptyList()) { _, _ ->
        val result = AggregateValue(itemToInt)
        val chateau = preferences?.getString("chateauMonster", "")?.takeIf { it.isNotBlank() }
        if (chateau != null) {
            for (entry in chateau.split("|").filter { it.isNotBlank() }) {
                val parts = entry.split(":")
                val name = parts.getOrNull(0) ?: continue
                val count = parts.getOrNull(1)?.toIntOrNull() ?: 1
                result[AshValue.item(name)] = AshValue.of(count.toLong())
            }
        }
        result
    }
}

// ── AshP912 — get_shop, shop_amount, shop_price, shop_limit ────────────────

internal fun GameRuntimeLibrary.registerAshP912Batch(scope: AshScope) {
    val itemToInt = AggregateType(AshType.ITEM, AshType.INT)

    regFn(scope, "get_shop", itemToInt, emptyList()) { _, _ ->
        val result = AggregateValue(itemToInt)
        val hasStore = character?.state?.value?.hasStore ?: false
        if (!hasStore) return@regFn result
        val shopItems = preferences?.getString("shopInventory", "")?.takeIf { it.isNotBlank() }
        if (shopItems != null) {
            for (entry in shopItems.split("|").filter { it.isNotBlank() }) {
                val parts = entry.split(":")
                val name = parts.getOrNull(0) ?: continue
                val qty = parts.getOrNull(1)?.toIntOrNull() ?: 0
                result[AshValue.item(name)] = AshValue.of(qty.toLong())
            }
        }
        result
    }

    regFn(scope, "shop_amount", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val hasStore = character?.state?.value?.hasStore ?: false
        if (!hasStore) return@regFn AshValue.of(0L)
        val shopItems = preferences?.getString("shopInventory", "")?.takeIf { it.isNotBlank() }
            ?: return@regFn AshValue.of(0L)
        val qty = findShopEntry(shopItems, itemName)?.second ?: 0
        AshValue.of(qty.toLong())
    }

    regFn(scope, "shop_price", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val hasStore = character?.state?.value?.hasStore ?: false
        if (!hasStore) return@regFn AshValue.of(0L)
        val shopPrices = preferences?.getString("shopPrices", "")?.takeIf { it.isNotBlank() }
            ?: return@regFn AshValue.of(0L)
        val price = findShopEntry(shopPrices, itemName)?.second ?: 0
        AshValue.of(price.toLong())
    }

    regFn(scope, "shop_limit", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val hasStore = character?.state?.value?.hasStore ?: false
        if (!hasStore) return@regFn AshValue.of(0L)
        val shopLimits = preferences?.getString("shopLimits", "")?.takeIf { it.isNotBlank() }
            ?: return@regFn AshValue.of(0L)
        val limit = findShopEntry(shopLimits, itemName)?.second ?: 0
        AshValue.of(limit.toLong())
    }
}

private fun findShopEntry(serialized: String, itemName: String): Pair<String, Int>? {
    for (entry in serialized.split("|").filter { it.isNotBlank() }) {
        val parts = entry.split(":")
        val name = parts.getOrNull(0) ?: continue
        val value = parts.getOrNull(1)?.toIntOrNull() ?: 0
        if (name.equals(itemName, ignoreCase = true)) return name to value
    }
    return null
}

// ── AshP913 — my_session_adv ────────────────────────────────────────────────

internal fun GameRuntimeLibrary.registerAshP913Batch(scope: AshScope) {
    regFn(scope, "my_session_adv", AshType.INT, emptyList()) { _, _ ->
        val adv = preferences?.getInt("_sessionAdventuresUsed", 0) ?: 0
        AshValue.of(adv.toLong())
    }
}

// ── AshP914 — my_session_items(item), my_session_items() ────────────────────

internal fun GameRuntimeLibrary.registerAshP914Batch(scope: AshScope) {
    val intByItem = AggregateType(AshType.ITEM, AshType.INT)

    regFn(scope, "my_session_items", intByItem, emptyList()) { _, _ ->
        val result = AggregateValue(intByItem)
        val tally = preferences?.getString("_sessionItemTally", "")?.takeIf { it.isNotBlank() }
            ?: return@regFn result
        for (entry in tally.split("|").filter { it.isNotBlank() }) {
            val sep = entry.lastIndexOf(':')
            if (sep < 0) continue
            val name = entry.substring(0, sep)
            val count = entry.substring(sep + 1).toIntOrNull() ?: 0
            result[AshValue.item(name)] = AshValue.of(count.toLong())
        }
        result
    }

    regFn(scope, "my_session_items", AshType.INT,
        listOf("it" to AshType.ITEM)) { _, args ->
        val itemName = args[0].toString()
        val tally = preferences?.getString("_sessionItemTally", "")?.takeIf { it.isNotBlank() }
            ?: return@regFn AshValue.of(0L)
        for (entry in tally.split("|").filter { it.isNotBlank() }) {
            val sep = entry.lastIndexOf(':')
            if (sep < 0) continue
            val name = entry.substring(0, sep)
            val count = entry.substring(sep + 1).toIntOrNull() ?: 0
            if (name.equals(itemName, ignoreCase = true)) return@regFn AshValue.of(count.toLong())
        }
        AshValue.of(0L)
    }
}

// ── AshP915 — my_session_results ────────────────────────────────────────────

internal fun GameRuntimeLibrary.registerAshP915Batch(scope: AshScope) {
    val stringToInt = AggregateType(AshType.STRING, AshType.INT)

    regFn(scope, "my_session_results", stringToInt, emptyList()) { _, _ ->
        val result = AggregateValue(stringToInt)
        val tally = preferences?.getString("_sessionResultTally", "")?.takeIf { it.isNotBlank() }
            ?: return@regFn result
        for (entry in tally.split("|").filter { it.isNotBlank() }) {
            val sep = entry.lastIndexOf(':')
            if (sep < 0) continue
            val label = entry.substring(0, sep)
            val count = entry.substring(sep + 1).toIntOrNull() ?: 0
            result[AshValue.of(label)] = AshValue.of(count.toLong())
        }
        result
    }
}

// ── AshP916 — current_mcd, change_mcd ───────────────────────────────────────

internal fun GameRuntimeLibrary.registerAshP916Batch(scope: AshScope) {
    regFn(scope, "current_mcd", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.mindControlLevel ?: 0).toLong())
    }

    regFn(scope, "change_mcd", AshType.BOOLEAN,
        listOf("level" to AshType.INT)) { _, args ->
        val level = args[0].toLong().toInt()
        val req = mindControlRequest ?: return@regFn AshValue.of(false)
        val ok = runBlocking { req.setLevel(level).isSuccess }
        AshValue.of(ok)
    }
}

// ── AshP917 — get_fuel ──────────────────────────────────────────────────────

internal fun GameRuntimeLibrary.registerAshP917Batch(scope: AshScope) {
    regFn(scope, "get_fuel", AshType.INT, emptyList()) { _, _ ->
        val fuel = CampgroundItemSync.asdonMartinFuel(preferences)
        AshValue.of(fuel.toLong())
    }
}

// ── AshP918 — total_free_rests ──────────────────────────────────────────────

internal fun GameRuntimeLibrary.registerAshP918Batch(scope: AshScope) {
    regFn(scope, "total_free_rests", AshType.INT, emptyList()) { _, _ ->
        val mods = buildCurrentModifiers()
        val freeRests = mods.values.get(DoubleModifier.FREE_RESTS).toInt()
        AshValue.of(freeRests.toLong())
    }
}
