package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ItemDatabase

internal fun GameRuntimeLibrary.resolveAshItemId(arg: AshValue): Int? {
    if (arg.type != AshType.ITEM) return null
    val asInt = arg.toLong().toInt()
    if (asInt > 0 && ItemDatabase.getById(asInt) != null) return asInt
    return gameDatabase?.item(arg.toString())?.id
        ?: ItemDatabase.getByName(arg.toString())?.id
}

internal fun GameRuntimeLibrary.itemAshValue(itemId: Int): AshValue {
    val name = ItemDatabase.getById(itemId)?.name ?: itemId.toString()
    return AshValue.item(name)
}
