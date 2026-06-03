package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.adventure.choice.EffectPool

object HiddenCityHandlers {
    val handlers: Map<Int, ChoiceHandler> = buildMap {

        // Case 780 — Action Elevator
        put(780) { ctx ->
            when (ctx.preference) {
                1 -> when {
                    ctx.prefInt("hiddenApartmentProgress") >= 7 -> 6
                    ctx.hasEffect(EffectPool.CURSE3_EFFECT)      -> 1
                    else                                         -> 2
                }
                3 -> if (ctx.prefInt("relocatePygmyLawyer") == ctx.ascensionNumber) 6 else 3
                else -> ctx.preference.takeIf { it > 0 }
            }
        }

        // Case 781 — Idle Hands
        put(781) { ctx ->
            if (ctx.preference != 1) return@put ctx.preference.takeIf { it > 0 }
            when (ctx.prefInt("hiddenApartmentProgress")) {
                7    -> 2
                0    -> 1
                else -> 6
            }
        }

        // Case 783 — The Dripping Trees
        put(783) { ctx ->
            if (ctx.preference != 1) return@put ctx.preference.takeIf { it > 0 }
            when (ctx.prefInt("hiddenHospitalProgress")) {
                7    -> 2
                0    -> 1
                else -> 6
            }
        }

        // Case 785 — Working Holiday (decision tree only; inventory branch handled in InventoryHandlers 786)
        put(785) { ctx ->
            if (ctx.preference != 1) return@put ctx.preference.takeIf { it > 0 }
            when (ctx.prefInt("hiddenOfficeProgress")) {
                7    -> 2
                0    -> 1
                else -> 6
            }
        }

        // Case 787 — Bowling for Gnomes
        put(787) { ctx ->
            if (ctx.preference != 1) return@put ctx.preference.takeIf { it > 0 }
            when (ctx.prefInt("hiddenBowlingAlleyProgress")) {
                7    -> 2
                0    -> 1
                else -> 6
            }
        }

        // Case 789 — Overconfident
        put(789) { ctx ->
            if (ctx.preference == 2 &&
                ctx.prefInt("relocatePygmyJanitor") == ctx.ascensionNumber) 1
            else ctx.preference.takeIf { it > 0 }
        }
    }

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
