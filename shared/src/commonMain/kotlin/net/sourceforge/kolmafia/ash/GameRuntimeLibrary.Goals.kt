package net.sourceforge.kolmafia.ash

internal fun GameRuntimeLibrary.registerGoalQueries(scope: AshScope) {

    val stringIntType = AggregateType(AshType.INT, AshType.STRING)

    // add_item_condition(int qty, item it) → void
    // qty is ignored; the item name is registered as a goal.
    regFn(scope, "add_item_condition", AshType.VOID,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        goalManager?.addItemGoalByName(args[1].toString())
        AshValue.VOID
    }

    // remove_item_condition(int qty, item it) → void
    regFn(scope, "remove_item_condition", AshType.VOID,
        listOf("qty" to AshType.INT, "it" to AshType.ITEM)) { _, args ->
        goalManager?.removeGoal(args[1].toString())
        AshValue.VOID
    }

    // goal_exists(string type) → boolean
    regFn(scope, "goal_exists", AshType.BOOLEAN,
        listOf("type" to AshType.STRING)) { _, args ->
        AshValue.of(goalManager?.matchesConditionType(args[0].toString()) ?: false)
    }

    // goal_count(string type) → int — remaining count for count-based goals
    regFn(scope, "goal_count", AshType.INT, listOf("type" to AshType.STRING)) { _, args ->
        val type = args[0].toString()
        val state = character?.state?.value
        val inventoryCount: (Int) -> Int = { id -> inventoryManager?.getCount(id) ?: 0 }
        AshValue.of(goalManager?.goalCount(type, preferences, state, inventoryCount) ?: 0)
    }

    // get_goals() → string[int]
    regFn(scope, "get_goals", stringIntType, emptyList()) { _, _ ->
        val result = AggregateValue(stringIntType)
        val goals = goalManager?.allGoalsAsStrings() ?: emptyList()
        goals.forEachIndexed { i, s -> result[AshValue.of(i)] = AshValue.of(s) }
        result
    }
}
