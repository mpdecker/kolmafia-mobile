package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.craftTypeDescription

/**
 * ASH-P117 behavioral batch — craft type description from concoctions database.
 */
internal fun GameRuntimeLibrary.registerAshP117Batch(scope: AshScope) {
    fun craftTypeForItemName(name: String): String {
        val itemName = gameDatabase?.item(name)?.name ?: name
        return ConcoctionDatabase.getByResult(itemName)?.craftTypeDescription() ?: "none"
    }

    regFn(scope, "craft_type", AshType.STRING, listOf("it" to AshType.ITEM)) { _, args ->
        AshValue.of(craftTypeForItemName(args[0].toString()))
    }

    regFn(scope, "craft_type", AshType.STRING, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        val name = ItemDatabase.getById(id)?.name ?: return@regFn AshValue.of("none")
        AshValue.of(craftTypeForItemName(name))
    }
}
