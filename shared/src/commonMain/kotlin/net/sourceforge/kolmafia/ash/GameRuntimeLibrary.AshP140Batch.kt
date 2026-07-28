package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.NpcStoreDatabase

/**
 * ASH-P140 behavioral batch — coinmaster validate v7 + is_npc_item 1-arg parity.
 */
internal fun GameRuntimeLibrary.registerAshP140Batch(scope: AshScope) {
    regFn(scope, "is_npc_item", AshType.BOOLEAN, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        AshValue.of(
            NpcStoreDatabase.containsItem(
                id,
                validate = false,
                state = craftCharacterState(),
                prefs = preferences,
                accessibleCount = { itemId -> craftAccessibleCount(itemId) },
                hasActiveEffect = { effectId -> hasActiveEffect(effectId) },
                familiarUsable = { familiarId -> npcFamiliarUsable(familiarId) },
            ),
        )
    }
}
