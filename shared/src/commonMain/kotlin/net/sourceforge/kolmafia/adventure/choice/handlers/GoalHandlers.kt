package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.adventure.choice.ItemPool

object GoalHandlers {
    val handlers: Map<Int, ChoiceHandler> = buildMap {
        // Cases 26, 27 — A Three-Tined Fork / Footprints
        // Desktop mapping: choice 26 → option (i/4 + 1), choice 27 → option (i%4/2 + 1)
        val handler = ChoiceHandler { ctx ->
            for (i in 0 until 12) {
                if (ctx.hasItemGoal(ItemPool.WOODS_ITEM_IDS[i])) {
                    return@ChoiceHandler if (ctx.choiceId == 26) i / 4 + 1 else i % 4 / 2 + 1
                }
            }
            ctx.preference.takeIf { it > 0 }
        }
        put(26, handler)
        put(27, handler)
    }

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
