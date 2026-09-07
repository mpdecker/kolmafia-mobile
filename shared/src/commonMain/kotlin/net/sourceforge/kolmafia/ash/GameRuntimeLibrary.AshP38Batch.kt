package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.AdventureQueueDatabase
import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.ZoneCombatData

/**
 * AshP38 — live location monster queries from [CombatDatabase].
 * Mirrors desktop [RuntimeLibrary.get_monsters] / [RuntimeLibrary.appearance_rates].
 *
 * Phase 5006–5017: [AdventureQueueDatabase] stateful rates when includeQueue=true.
 */
internal fun GameRuntimeLibrary.registerAshP38Batch(scope: AshScope) {
    val monsterIntType = AggregateType(AshType.INT, AshType.MONSTER)
    val floatMonsterType = AggregateType(AshType.MONSTER, AshType.FLOAT)
    val booleanMonsterType = AggregateType(AshType.MONSTER, AshType.BOOLEAN)

    regFn(scope, "get_monsters", monsterIntType, listOf("location" to AshType.LOCATION)) { _, args ->
        buildGetMonsters(resolveLocationQueryName(args[0].toString()), monsterIntType)
    }

    regFn(scope, "get_monsters", monsterIntType, listOf("location" to AshType.STRING)) { _, args ->
        buildGetMonsters(resolveLocationQueryName(args[0].toString()), monsterIntType)
    }

    regFn(scope, "appearance_rates", floatMonsterType, listOf("location" to AshType.LOCATION)) { _, args ->
        buildAppearanceRates(resolveLocationQueryName(args[0].toString()), floatMonsterType, false)
    }

    regFn(scope, "appearance_rates", floatMonsterType, listOf("location" to AshType.STRING)) { _, args ->
        buildAppearanceRates(resolveLocationQueryName(args[0].toString()), floatMonsterType, false)
    }

    regFn(
        scope,
        "appearance_rates",
        floatMonsterType,
        listOf("location" to AshType.LOCATION, "includeQueue" to AshType.BOOLEAN),
    ) { _, args ->
        buildAppearanceRates(
            resolveLocationQueryName(args[0].toString()),
            floatMonsterType,
            args[1].toBoolean(),
        )
    }

    regFn(
        scope,
        "appearance_rates",
        floatMonsterType,
        listOf("location" to AshType.STRING, "includeQueue" to AshType.BOOLEAN),
    ) { _, args ->
        buildAppearanceRates(
            resolveLocationQueryName(args[0].toString()),
            floatMonsterType,
            args[1].toBoolean(),
        )
    }

    regFn(
        scope,
        "get_location_monsters",
        booleanMonsterType,
        listOf("location" to AshType.LOCATION),
    ) { _, args ->
        buildLocationMonsters(resolveLocationQueryName(args[0].toString()), booleanMonsterType)
    }

    regFn(
        scope,
        "get_location_monsters",
        booleanMonsterType,
        listOf("location" to AshType.STRING),
    ) { _, args ->
        buildLocationMonsters(resolveLocationQueryName(args[0].toString()), booleanMonsterType)
    }
}

private fun positiveWeightMonsters(data: ZoneCombatData?) =
    data?.monsters?.filter { it.weight > 0 }.orEmpty()

private fun buildGetMonsters(locationName: String, type: AggregateType): AggregateValue {
    val result = AggregateValue(type)
    val monsters = positiveWeightMonsters(CombatDatabase.getByLocation(locationName))
    monsters.forEachIndexed { i, mw ->
        result[AshValue.of(i)] = AshValue(AshType.MONSTER, mw.name)
    }
    return result
}

private fun GameRuntimeLibrary.buildAppearanceRates(
    locationName: String,
    type: AggregateType,
    includeQueue: Boolean,
): AggregateValue {
    val result = AggregateValue(type)
    val data = CombatDatabase.getByLocation(locationName) ?: return result
    val combatPercent = data.combatPercent
    val noneRate = if (combatPercent < 0) -1.0 else (100.0 - combatPercent)
    // Desktop MONSTER_INIT prints as "none"; mobile to_monster("none") yields empty content.
    result[AshValue(AshType.MONSTER, "")] = AshValue.of(noneRate)

    val weighted = positiveWeightMonsters(data)
    val totalWeight = weighted.sumOf { it.weight }
    if (totalWeight <= 0 || combatPercent < 0) return result

    val weightByName = weighted.associate { it.name.lowercase() to it.weight }
    val turns = character?.state?.value?.turnsPlayed ?: 0
    for (mw in weighted) {
        val rate = if (includeQueue) {
            val numerator = combatPercent.toDouble() * mw.weight
            AdventureQueueDatabase.applyQueueEffects(
                numerator = numerator,
                monsterName = mw.name,
                locationName = locationName,
                totalWeighting = totalWeight,
                weightOf = { name -> weightByName[name.lowercase()] ?: 0 },
                preferences = preferences,
                turnsPlayed = turns,
            )
        } else {
            mw.weight.toDouble() / totalWeight * combatPercent
        }
        result[AshValue(AshType.MONSTER, mw.name)] = AshValue.of(rate)
    }
    return result
}

private fun buildLocationMonsters(locationName: String, type: AggregateType): AggregateValue {
    val result = AggregateValue(type)
    for (mw in positiveWeightMonsters(CombatDatabase.getByLocation(locationName))) {
        result[AshValue(AshType.MONSTER, mw.name)] = AshValue.TRUE
    }
    return result
}
