package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry

object SkillUsesHandlers {
    val handlers: Map<Int, ChoiceHandler> = buildMap {
        // Case 600 — Summon Minion
        put(600) { ctx -> if (ctx.skillUses > 0) 1 else 2 }
        // Case 601 — Summon Horde (multi-cast; skillUses decremented per submission in AdventureManager)
        put(601) { ctx -> if (ctx.skillUses > 0) 1 else 2 }
    }

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
