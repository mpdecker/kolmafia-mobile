package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ZoneCombatCalculator
import net.sourceforge.kolmafia.data.ZoneCombatData

/**
 * AshP38 — live location monster queries from [CombatDatabase] /
 * [ZoneCombatCalculator].
 *
 * Phase 5051–5070: banish/rejection/superlikely-aware rates when
 * `appearance_rates(loc, includeQueue=true)`.
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
        buildLocationMonsters(resolveLocationQueryName(args[0].toString()), booleanMonsterType, false)
    }

    regFn(
        scope,
        "get_location_monsters",
        booleanMonsterType,
        listOf("location" to AshType.STRING),
    ) { _, args ->
        buildLocationMonsters(resolveLocationQueryName(args[0].toString()), booleanMonsterType, false)
    }

    regFn(
        scope,
        "get_location_monsters",
        booleanMonsterType,
        listOf("location" to AshType.LOCATION, "includeQueue" to AshType.BOOLEAN),
    ) { _, args ->
        buildLocationMonsters(
            resolveLocationQueryName(args[0].toString()),
            booleanMonsterType,
            args[1].toBoolean(),
        )
    }

    regFn(
        scope,
        "get_location_monsters",
        booleanMonsterType,
        listOf("location" to AshType.STRING, "includeQueue" to AshType.BOOLEAN),
    ) { _, args ->
        buildLocationMonsters(
            resolveLocationQueryName(args[0].toString()),
            booleanMonsterType,
            args[1].toBoolean(),
        )
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

private fun GameRuntimeLibrary.zoneCombatContext(): ZoneCombatCalculator.Context {
    val state = character?.state?.value
    val currentRun = state?.currentRun ?: 0
    val mods = buildCurrentModifiers()
    val loc = lastLocationName()
    val famItem = state?.equipment?.get(net.sourceforge.kolmafia.character.EquipmentSlot.FAMILIAR)
    val hatName = state?.equipment?.get(net.sourceforge.kolmafia.character.EquipmentSlot.HAT).orEmpty()
    val pantsName = state?.equipment?.get(net.sourceforge.kolmafia.character.EquipmentSlot.PANTS).orEmpty()
    val inv = inventoryManager?.state?.value?.items
    return ZoneCombatCalculator.Context(
        preferences = preferences,
        banishManager = banishManager,
        adventureSpent = adventureSpentTracker,
        questDatabase = questDatabase,
        characterState = state,
        turnsPlayed = currentRun,
        ascensions = state?.ascensionNumber ?: 0,
        combatRateAdjustment = CombatAdjustment.combatRateModifier(mods, loc),
        initiativeAdjustment = mods.values.get(net.sourceforge.kolmafia.modifiers.DoubleModifier.INITIATIVE),
        crystalBallEquipped = net.sourceforge.kolmafia.session.CrystalBallManager.isEquipped(famItem),
        monsterLevel = CombatAdjustment.monsterLevelAdjustment(mods, state, loc),
        familiarId = state?.familiarId ?: 0,
        hatItemId = ItemDatabase.getByName(hatName)?.id ?: 0,
        pantsItemId = ItemDatabase.getByName(pantsName)?.id ?: 0,
        hasMultiPass = (inv?.get(4074)?.quantity ?: 0) > 0,
    )
}

private fun GameRuntimeLibrary.buildAppearanceRates(
    locationName: String,
    type: AggregateType,
    includeQueue: Boolean,
): AggregateValue {
    val result = AggregateValue(type)
    val rates = ZoneCombatCalculator.appearanceRates(
        locationName = locationName,
        includeQueue = includeQueue,
        ctx = zoneCombatContext(),
    )
    for ((monster, rate) in rates) {
        result[AshValue(AshType.MONSTER, monster)] = AshValue.of(rate)
    }
    return result
}

private fun GameRuntimeLibrary.buildLocationMonsters(
    locationName: String,
    type: AggregateType,
    includeQueue: Boolean,
): AggregateValue {
    val result = AggregateValue(type)
    val rates = ZoneCombatCalculator.appearanceRates(
        locationName = locationName,
        includeQueue = includeQueue,
        ctx = zoneCombatContext(),
    )
    for ((monster, rate) in rates) {
        if (monster.isEmpty()) continue
        if (rate > 0) result[AshValue(AshType.MONSTER, monster)] = AshValue.TRUE
    }
    return result
}
