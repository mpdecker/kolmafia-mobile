package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry

object ResponseTextHandlers {
    val handlers: Map<Int, ChoiceHandler> = buildMap {
        put(155) { ctx ->
            if (ctx.preference == 4 && !ctx.responseText.contains("Check the shiny object")) 5
            else ctx.preference.takeIf { it > 0 }
        }
        put(575) { ctx ->
            if (ctx.preference == 2 && !ctx.responseText.contains("Dig deeper")) 3
            else ctx.preference.takeIf { it > 0 }
        }
        put(678) { ctx ->
            if (ctx.preference == 3 && !ctx.responseText.contains("Check behind the trash can")) null
            else ctx.preference.takeIf { it > 0 }
        }
        put(705) { ctx ->
            when {
                ctx.preference == 2 && !ctx.responseText.contains("Go to the janitor's closet")     -> null
                ctx.preference == 3 && !ctx.responseText.contains("Head to the bathroom")           -> null
                ctx.preference == 4 && !ctx.responseText.contains("Check out the teacher's lounge") -> null
                else -> ctx.preference.takeIf { it > 0 }
            }
        }
        put(808) { ctx ->
            if (ctx.preference == 2 && !ctx.responseText.contains("nightstand wasn't here before")) null
            else ctx.preference.takeIf { it > 0 }
        }
        put(919) { ctx ->
            if (ctx.preference == 1 && ctx.responseText.contains("You've already thoroughly")) 6
            else ctx.preference.takeIf { it > 0 }
        }
        put(923) { ctx ->
            when {
                ctx.preference == 2 && !ctx.responseText.contains("Visit the blacksmith's cottage") -> null
                ctx.preference == 3 && !ctx.responseText.contains("Go to the black gold mine")     -> null
                ctx.preference == 4 && !ctx.responseText.contains("Check out the black church")    -> null
                else -> ctx.preference.takeIf { it > 0 }
            }
        }
        put(973) { ctx ->
            if (ctx.preference == 2 && !ctx.responseText.contains("Turn in Hooch")) 6
            else ctx.preference.takeIf { it > 0 }
        }
        put(975) { ctx ->
            if (!ctx.responseText.contains("Stick in the onions")) 2
            else ctx.preference.takeIf { it > 0 }
        }
        put(1026) { ctx ->
            if (ctx.preference == 2 && !ctx.responseText.contains("Investigate the noisy drawer")) 3
            else ctx.preference.takeIf { it > 0 }
        }
        put(1222) { ctx ->
            if (ctx.responseText.contains("You've already gone through the Tunnel once today")) 2
            else ctx.preference.takeIf { it > 0 }
        }
        put(1461) { ctx ->
            if (ctx.responseText.contains("Grab the Cheer Core!")) 5
            else ctx.preference.takeIf { it > 0 }
        }
    }

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
