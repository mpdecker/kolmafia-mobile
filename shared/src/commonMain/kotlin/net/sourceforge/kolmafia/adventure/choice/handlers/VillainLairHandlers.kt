package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceContext
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.preferences.Preferences

object VillainLairHandlers {

    private fun colorDecide(ctx: ChoiceContext): Int? {
        val color = ctx.preferences.getString(Preferences.VILLAIN_LAIR_COLOR, "").lowercase()
        if (color.isEmpty()) return null
        // Scan options 1–6; find the one whose surrounding HTML fragment mentions the color
        return (1..6).firstOrNull { option ->
            val marker   = "name=whichchoice value=$option"
            val afterMarker = ctx.responseText.substringAfter(marker, "")
            val fragment = if (afterMarker.contains("name=whichchoice"))
                afterMarker.substringBefore("name=whichchoice")
            else
                afterMarker
            fragment.contains(color, ignoreCase = true)
        }
    }

    val handlers: Map<Int, ChoiceHandler> = buildMap {
        val handler = ChoiceHandler { ctx -> colorDecide(ctx) }
        put(1260, handler)
        put(1262, handler)
    }

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
