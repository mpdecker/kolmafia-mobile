package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.RufusManager
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry

object RufusHandlers {

    fun registerAll(registry: ChoiceHandlerRegistry, rufusManager: RufusManager) {
        registry.register(1498) { ctx ->
            rufusManager.chooseQuestOption(ctx.responseText)
        }
        registry.register(1499) { _ ->
            rufusManager.confirmChoice()
        }
    }
}
