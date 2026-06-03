package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry

object DreadsylvaniaHandlers {

    private fun ghostPencil(prefKey: String): ChoiceHandler = ChoiceHandler { ctx ->
        if (ctx.preference == 5 &&
            (!ctx.responseText.contains("Use a ghost pencil") || ctx.prefBool(prefKey))) 6
        else ctx.preference.takeIf { it > 0 }
    }

    val handlers: Map<Int, ChoiceHandler> = mapOf(
        721 to ghostPencil("ghostPencil1"),
        725 to ghostPencil("ghostPencil2"),
        729 to ghostPencil("ghostPencil3"),
        733 to ghostPencil("ghostPencil4"),
        737 to ghostPencil("ghostPencil5"),
        741 to ghostPencil("ghostPencil6"),
        745 to ghostPencil("ghostPencil7"),
        749 to ghostPencil("ghostPencil8"),
        753 to ghostPencil("ghostPencil9"),
    )

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
