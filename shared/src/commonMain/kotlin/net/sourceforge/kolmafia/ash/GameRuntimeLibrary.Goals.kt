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
            "factoid", "factoids", "manuel" -> goalManager?.hasFactoidGoalSet() ?: false ||
                (goalManager?.hasFactoidCountGoal() == true)
            "autostop" -> goalManager?.hasAutostopGoal() ?: false
            "outfit" -> goalManager?.hasItemGoals() ?: false
            "choice" -> (goalManager?.hasChoiceGoalSet() == true) ||
                (goalManager?.hasChoiceAdventureGoal() == true)
            "floundry" -> goalManager?.hasFloundryGoal() ?: false
            "leprecondo" -> goalManager?.hasLeprecondoGoal() ?: false
            "substats" -> goalManager?.hasSubstatsGoal() ?: false
            "pseudo", "pirate insult", "pirate insults" -> goalManager?.hasPseudoGoal() ?: false
            "health", "hp" -> goalManager?.hasHealthGoal() ?: false
            "mana", "mp" -> goalManager?.hasManaGoal() ?: false
            else    -> false
        }
        AshValue.of(result)
    }

    // goal_count(string type) → int — remaining count for count-based goals
    regFn(scope, "goal_count", AshType.INT, listOf("type" to AshType.STRING)) { _, args ->
        val type = args[0].toString()
        val state = character?.state?.value
        AshValue.of(goalManager?.goalCount(type, preferences, state) ?: 0)
    }

    // get_goals() → string[int]
    regFn(scope, "get_goals", stringIntType, emptyList()) { _, _ ->
        val result = AggregateValue(stringIntType)
        val goals = goalManager?.allGoalsAsStrings() ?: emptyList()
        goals.forEachIndexed { i, s -> result[AshValue.of(i)] = AshValue.of(s) }
        result
    }
}
