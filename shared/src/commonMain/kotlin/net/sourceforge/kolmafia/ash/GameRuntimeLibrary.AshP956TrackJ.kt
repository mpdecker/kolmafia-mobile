package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.CandyDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.request.SweetSynthesisRequest

/**
 * AshP956–959 Track J — Sweet synthesis pair/pairing + live helpers.
 * Effect/item overloads of sweet_synthesis are live in Track F (AshP928).
 */
internal fun GameRuntimeLibrary.registerAshP956TrackJBatch(scope: AshScope) {
    val itemArray = AggregateType(AshType.INT, AshType.ITEM)

    fun invCounts(): (Int) -> Int = { id ->
        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
    }

    fun itemName(id: Int): String =
        gameDatabase?.item(id)?.name ?: ItemDatabase.getItemName(id)

    fun effectIdOf(effectArg: AshValue): Int {
        val name = effectArg.toString()
        return gameDatabase?.effect(name)?.id
            ?: SweetSynthesisRequest.resolveEffectId(name)
            ?: 0
    }

    // ── Phase 958: sweet_synthesis_pair ─────────────────────────────
    regFn(scope, "sweet_synthesis_pair", itemArray, listOf("effect" to AshType.EFFECT)) { _, args ->
        CandyDatabase.loadBlacklist(preferences)
        val pair = CandyDatabase.synthesisPairForEffect(effectIdOf(args[0]), invCounts())
        val result = AggregateValue(AggregateType(AshType.INT, AshType.ITEM, 2))
        if (pair.size >= 2) {
            result[AshValue.of(0)] = AshValue.item(itemName(pair[0]))
            result[AshValue.of(1)] = AshValue.item(itemName(pair[1]))
        }
        result
    }

    regFn(
        scope,
        "sweet_synthesis_pair",
        itemArray,
        listOf("effect" to AshType.EFFECT, "flags" to AshType.INT),
    ) { _, args ->
        CandyDatabase.loadBlacklist(preferences)
        val pair = CandyDatabase.synthesisPairForEffect(effectIdOf(args[0]), invCounts())
        val result = AggregateValue(AggregateType(AshType.INT, AshType.ITEM, 2))
        if (pair.size >= 2) {
            result[AshValue.of(0)] = AshValue.item(itemName(pair[0]))
            result[AshValue.of(1)] = AshValue.item(itemName(pair[1]))
        }
        result
    }

    // ── Phase 959: sweet_synthesis_pairing ──────────────────────────
    regFn(
        scope,
        "sweet_synthesis_pairing",
        itemArray,
        listOf("effect" to AshType.EFFECT, "item" to AshType.ITEM),
    ) { _, args ->
        val itemId = gameDatabase?.item(args[1].toString())?.id
            ?: ItemDatabase.getByName(args[1].toString())?.id
            ?: 0
        CandyDatabase.loadBlacklist(preferences)
        val partners = CandyDatabase.sweetSynthesisPairing(effectIdOf(args[0]), itemId, invCounts())
        val result = AggregateValue(AggregateType(AshType.INT, AshType.ITEM, partners.size))
        partners.forEachIndexed { i, id ->
            result[AshValue.of(i)] = AshValue.item(itemName(id))
        }
        result
    }
}

internal fun GameRuntimeLibrary.runSweetSynthesisEffect(effectQuery: String, count: Int): Boolean {
    if (count <= 0) return false
    val client = httpClient ?: return false
    val choice = choiceRequest ?: return false
    val counts: (Int) -> Int = { id ->
        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
    }
    val hasSkill = skillManager?.state?.value?.skills.orEmpty().any {
        it.id == SweetSynthesisRequest.SKILL_ID ||
            it.name.equals("Sweet Synthesis", ignoreCase = true)
    }
    var ok = true
    runBlocking {
        repeat(count) {
            if (!ok) return@repeat
            SweetSynthesisRequest(client, choice, retrieveItemService)
                .synthesize(
                    effectQuery = effectQuery,
                    preferences = preferences,
                    charState = character?.state?.value,
                    inventoryCounts = counts,
                    hasSkill = hasSkill,
                )
                .onFailure { ok = false }
        }
    }
    return ok
}

internal fun GameRuntimeLibrary.runSweetSynthesisPair(itemId1: Int, itemId2: Int, count: Int): Boolean {
    if (count <= 0 || itemId1 <= 0 || itemId2 <= 0) return false
    val client = httpClient ?: return false
    val choice = choiceRequest ?: return false
    val hasSkill = skillManager?.state?.value?.skills.orEmpty().any {
        it.id == SweetSynthesisRequest.SKILL_ID ||
            it.name.equals("Sweet Synthesis", ignoreCase = true)
    }
    var ok = true
    runBlocking {
        repeat(count) {
            if (!ok) return@repeat
            SweetSynthesisRequest(client, choice, retrieveItemService)
                .synthesizePair(
                    itemId1 = itemId1,
                    itemId2 = itemId2,
                    preferences = preferences,
                    charState = character?.state?.value,
                    hasSkill = hasSkill,
                )
                .onFailure { ok = false }
        }
    }
    return ok
}
