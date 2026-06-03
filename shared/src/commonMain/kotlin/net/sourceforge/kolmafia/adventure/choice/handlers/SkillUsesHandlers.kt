package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry

object SkillUsesHandlers {
    // Cases 600 (Summon Minion) and 601 (Summon Horde) share identical decision logic:
    // submit option 1 for each pending skill cast, option 2 (abort) when done.
    // skillUses is decremented by AdventureManager after each submission.
    private val summonHandler = ChoiceHandler { ctx -> if (ctx.skillUses > 0) 1 else 2 }

    val handlers: Map<Int, ChoiceHandler> = mapOf(
        600 to summonHandler,
        601 to summonHandler,
    )

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
