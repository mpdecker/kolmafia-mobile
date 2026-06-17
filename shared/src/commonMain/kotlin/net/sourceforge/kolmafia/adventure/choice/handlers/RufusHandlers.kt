package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.RufusManager
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

object RufusHandlers {

    fun registerAll(registry: ChoiceHandlerRegistry, rufusManager: RufusManager) {
        registry.register(1498) { ctx ->
            val progress = ctx.questDatabase.getProgress(Quest.RUFUS)
            when (progress) {
                QuestDatabase.UNSTARTED, QuestDatabase.STARTED ->
                    rufusManager.chooseQuestOption(ctx.responseText)
                        ?: rufusManager.specialChoiceDecision(1498, ctx.responseText, ctx.questDatabase)
                else ->
                    rufusManager.specialChoiceDecision(1498, ctx.responseText, ctx.questDatabase)
            }
        }
        registry.register(1499) { ctx ->
            rufusManager.shadowLabyrinthChoiceDecision(ctx.responseText, ctx.questDatabase)
        }
    }
}
