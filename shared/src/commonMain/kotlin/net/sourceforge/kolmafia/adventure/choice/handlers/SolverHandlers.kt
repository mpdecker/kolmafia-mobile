package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry

object SolverHandlers {
    val handlers: Map<Int, ChoiceHandler> = buildMap {

        // Case 486 — Dungeon Fist! (arcade game)
        put(486) { ctx -> ctx.solvers.arcadeGame.autoDungeonFist(ctx.stepCount, ctx.responseText) }

        // Case 535 — Ronald Safety Shelter
        put(535) { ctx ->
            ctx.solvers.safetyShelter.autoRonald(ctx.preference, ctx.stepCount, ctx.responseText)
        }
        // Case 536 — Grimace Safety Shelter
        put(536) { ctx ->
            ctx.solvers.safetyShelter.autoGrimace(ctx.preference, ctx.stepCount, ctx.responseText)
        }

        // Case 546 — Interview With You (vampire)
        put(546) { ctx -> ctx.solvers.vampOut.autoVampOut(ctx.preference, ctx.stepCount, ctx.responseText) }

        // Case 594 — A Lost Room
        put(594) { ctx -> ctx.solvers.lostKey.autoKey(ctx.preference, ctx.stepCount, ctx.responseText) }

        // Case 665 — A Gracious Maze (Gamepro)
        put(665) { ctx -> ctx.solvers.gamepro.autoSolve(ctx.stepCount) }

        // Case 702 — No Corn, Only Thorns (swamp navigation — pure response-text parse)
        put(702) { ctx ->
            when {
                ctx.responseText.contains("facing north") ||
                ctx.responseText.contains("face north")  ||
                ctx.responseText.contains("indicate north") -> 1
                ctx.responseText.contains("facing east")  ||
                ctx.responseText.contains("face east")   ||
                ctx.responseText.contains("indicate east")  -> 2
                ctx.responseText.contains("facing south") ||
                ctx.responseText.contains("face south")  ||
                ctx.responseText.contains("indicate south") -> 3
                ctx.responseText.contains("facing west")  ||
                ctx.responseText.contains("face west")   ||
                ctx.responseText.contains("indicate west")  -> 4
                else -> null
            }
        }

        // Cases 890–903 — Lights Out adventures
        for (i in 890..903) {
            put(i) { ctx -> ctx.solvers.lightsOut.autoLightsOut(ctx.choiceId, ctx.responseText) }
        }

        // Cases 1260, 1262 — Villain Lair: requires VillainLairSolver (separate plan). Falls through to user preference.
        // Cases 1498, 1499 — Rufus / Shadow Rift: requires RufusManager (separate plan). Falls through to user preference.
    }

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
