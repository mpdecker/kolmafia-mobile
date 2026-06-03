package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

object QuestHandlers {
    val handlers: Map<Int, ChoiceHandler> = buildMap {

        // Case 1060 — Temporarily Out of Skeletons (Meatsmith)
        put(1060) { ctx ->
            if (ctx.preference == 4 &&
                ctx.questDatabase.isQuestLaterThan(Quest.MEATSMITH, QuestDatabase.STARTED)) null
            else ctx.preference.takeIf { it > 0 }
        }

        // Case 1061 — Heart of Madness (Armorer)
        put(1061) { ctx ->
            when {
                ctx.preference == 1 &&
                    ctx.questDatabase.isQuestLaterThan(Quest.ARMORER, "step4") -> null
                ctx.preference == 3 &&
                    !ctx.questDatabase.isQuestFinished(Quest.ARMORER) -> null
                else -> ctx.preference.takeIf { it > 0 }
            }
        }
    }

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
