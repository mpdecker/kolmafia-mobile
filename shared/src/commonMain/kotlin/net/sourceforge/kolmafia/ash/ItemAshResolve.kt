package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ItemDatabase

internal fun GameRuntimeLibrary.resolveAshItemId(arg: AshValue): Int? {
    if (arg.type != AshType.ITEM) return null
    val name = arg.toString()
    name.toIntOrNull()?.let { id ->
        if (id > 0 && ItemDatabase.getById(id) != null) return id
    }
    val asInt = arg.toLong().toInt()
    if (asInt > 0 && ItemDatabase.getById(asInt) != null) return asInt
    return gameDatabase?.item(name)?.id
        ?: ItemDatabase.getByName(name)?.id
}

internal fun GameRuntimeLibrary.itemAshValue(itemId: Int): AshValue {
    val name = ItemDatabase.getById(itemId)?.name ?: itemId.toString()
    return AshValue.item(name)
}
