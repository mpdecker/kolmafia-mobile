package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.WeaponStat

/**
 * ASH-P121 behavioral batch — item property queries from bundled data.
 */
internal fun GameRuntimeLibrary.registerAshP121Batch(scope: AshScope) {
    fun itemId(arg: AshValue): Int? = resolveAshItemId(arg)

    regFn(scope, "is_tradeable", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val id = itemId(args[0]) ?: return@regFn AshValue.FALSE
        AshValue.of(ItemDatabase.isTradeable(id))
    }

    regFn(scope, "is_giftable", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val id = itemId(args[0]) ?: return@regFn AshValue.FALSE
        AshValue.of(ItemDatabase.isGiftable(id))
    }

    regFn(scope, "is_discardable", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val id = itemId(args[0]) ?: return@regFn AshValue.FALSE
        AshValue.of(ItemDatabase.isDiscardable(id))
    }

    regFn(scope, "is_displayable", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        val id = itemId(args[0]) ?: return@regFn AshValue.FALSE
        AshValue.of(ItemDatabase.isDisplayable(id))
    }

    regFn(scope, "to_plural", AshType.STRING, listOf("it" to AshType.ITEM)) { _, args ->
        val id = itemId(args[0]) ?: return@regFn AshValue.of("")
        AshValue.of(ItemDatabase.getPluralName(id))
    }

    regFn(scope, "get_power", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        val id = itemId(args[0]) ?: return@regFn AshValue.ZERO
        AshValue.of(EquipmentDatabase.getPower(id).toLong())
    }

    regFn(scope, "weapon_hands", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        val id = itemId(args[0]) ?: return@regFn AshValue.ZERO
        AshValue.of(EquipmentDatabase.getHands(id).toLong())
    }

    regFn(scope, "item_type", AshType.STRING, listOf("it" to AshType.ITEM)) { _, args ->
        val id = itemId(args[0]) ?: return@regFn AshValue.of("")
        AshValue.of(EquipmentDatabase.getItemType(id))
    }

    regFn(scope, "weapon_type", AshType.STAT, listOf("it" to AshType.ITEM)) { _, args ->
        val id = itemId(args[0]) ?: return@regFn AshValue(AshType.STAT, "")
        val statName = when (EquipmentDatabase.getWeaponStat(id)) {
            WeaponStat.MUSCLE -> "Muscle"
            WeaponStat.MYSTICALITY -> "Mysticality"
            WeaponStat.MOXIE -> "Moxie"
            WeaponStat.NONE -> ""
        }
        AshValue(AshType.STAT, statName)
    }
}
