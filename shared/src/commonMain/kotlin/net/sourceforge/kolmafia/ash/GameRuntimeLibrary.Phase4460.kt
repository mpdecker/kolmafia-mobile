package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ModifierDatabase

/** Phases 4451–4460 — ASH behavioral deepen VI (familiar weighted modifier overload). */
internal fun GameRuntimeLibrary.registerPhase4460(scope: AshScope) {
    regFn(
        scope,
        "numeric_modifier",
        AshType.FLOAT,
        listOf(
            "fa" to AshType.FAMILIAR,
            "modifier" to AshType.STRING,
            "weight" to AshType.INT,
            "it" to AshType.ITEM,
        ),
    ) { _, args ->
        val familiarRef = args[0].toString()
        val tag = args[1].toString()
        val weight = args[2].toLong().toInt().coerceAtLeast(1)
        val itemId = resolveAshItemId(args[3])
        val entry = gameDatabase?.familiarModifier(familiarRef)
            ?: familiarRef.toIntOrNull()?.let { gameDatabase?.familiarModifier(it) }
            ?: ModifierDatabase.getFamiliar(familiarRef)
        val base = numericFromEntry(entry, tag)
        val itemEntry = itemId?.let { id ->
            gameDatabase?.itemModifier(id)
                ?: ModifierDatabase.getItem(id.toString())
        }
        val itemBonus = numericFromEntry(itemEntry, tag)
        AshValue.of(base * weight + itemBonus)
    }
}
