package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.NpcStoreDatabase

/**
 * ASH-P132 behavioral batch — NPC shop validate v3 depth (NpcPurchaseAccessibility).
 */
internal fun GameRuntimeLibrary.registerAshP132Batch(scope: AshScope) {
    fun itemId(arg: AshValue): Int? = resolveAshItemId(arg)

    regFn(scope, "is_npc_item", AshType.BOOLEAN, listOf("id" to AshType.INT, "validate" to AshType.BOOLEAN)) { _, args ->
        val id = args[0].toLong().toInt()
        val validate = args[1].toBoolean()
        AshValue.of(
            NpcStoreDatabase.containsItem(
                id,
                validate = validate,
                state = craftCharacterState(),
                prefs = preferences,
                accessibleCount = { itemId -> craftAccessibleCount(itemId) },
                hasActiveEffect = { effectId -> hasActiveEffect(effectId) },
                familiarUsable = { familiarId -> npcFamiliarUsable(familiarId) },
            ),
        )
    }

    regFn(
        scope,
        "is_npc_item",
        AshType.BOOLEAN,
        listOf("it" to AshType.ITEM, "validate" to AshType.BOOLEAN),
    ) { _, args ->
        val id = itemId(args[0]) ?: return@regFn AshValue.FALSE
        val validate = args[1].toBoolean()
        AshValue.of(
            NpcStoreDatabase.containsItem(
                id,
                validate = validate,
                state = craftCharacterState(),
                prefs = preferences,
                accessibleCount = { itemId -> craftAccessibleCount(itemId) },
                hasActiveEffect = { effectId -> hasActiveEffect(effectId) },
                familiarUsable = { familiarId -> npcFamiliarUsable(familiarId) },
            ),
        )
    }
}
