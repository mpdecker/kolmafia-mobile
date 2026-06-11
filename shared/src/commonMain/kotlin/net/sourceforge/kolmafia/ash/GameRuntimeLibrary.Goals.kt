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
    // type: "item" | "meat" | "level"
    regFn(scope, "goal_exists", AshType.BOOLEAN,
        listOf("type" to AshType.STRING)) { _, args ->
        val type = args[0].toString().lowercase().trim()
        val result = when (type) {
            "item"  -> goalManager?.hasItemGoals() ?: false
            "meat"  -> goalManager?.hasMeatGoalSet() ?: false
            "level" -> goalManager?.hasLevelGoalSet() ?: false
            "factoid", "autostop" -> goalManager?.hasFactoidGoalSet() ?: false
            else    -> false
        }
        AshValue.of(result)
    }

    // get_goals() → string[int]
    regFn(scope, "get_goals", stringIntType, emptyList()) { _, _ ->
        val result = AggregateValue(stringIntType)
        val goals = goalManager?.allGoalsAsStrings() ?: emptyList()
        goals.forEachIndexed { i, s -> result[AshValue.of(i)] = AshValue.of(s) }
        result
    }
}
