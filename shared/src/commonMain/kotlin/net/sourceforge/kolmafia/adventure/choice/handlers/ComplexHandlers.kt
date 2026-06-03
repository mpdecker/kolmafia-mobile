package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.modifiers.DoubleModifier

object ComplexHandlers {
    val handlers: Map<Int, ChoiceHandler> = buildMap {

        put(304) { ctx ->
            if (ctx.preference == 1 &&
                (ctx.prefInt("tempuraSummons") == 3 || ctx.currentMp < 200)) 2
            else ctx.preference.takeIf { it > 0 }
        }

        put(309) { ctx ->
            if (ctx.preference == 1 && ctx.prefInt("seaodesFound") == 3) 2
            else ctx.preference.takeIf { it > 0 }
        }

        // Case 496 — Crate Expectations (hot damage threshold)
        put(496) { ctx ->
            if (ctx.preference == 2 &&
                ctx.currentNumericModifier(DoubleModifier.HOT_DAMAGE) < 20.0) 1
            else ctx.preference.takeIf { it > 0 }
        }

        put(502) { ctx ->
            if (ctx.preference == 2 && ctx.prefString("choiceAdventure505") == "2") {
                if (ctx.hasItem(ItemPool.TREE_HOLED_COIN)) 3 else ctx.preference
            } else ctx.preference.takeIf { it > 0 }
        }

        // Case 513 — Staring Down the Barrel (cold damage)
        put(513) { ctx ->
            if (ctx.preference == 2 &&
                ctx.currentNumericModifier(DoubleModifier.COLD_DAMAGE) < 20.0) 1
            else ctx.preference.takeIf { it > 0 }
        }

        // Case 514 — 1984 Had Nothing on This Cellar (stench damage)
        put(514) { ctx ->
            if (ctx.preference == 2 &&
                ctx.currentNumericModifier(DoubleModifier.STENCH_DAMAGE) < 20.0) 1
            else ctx.preference.takeIf { it > 0 }
        }

        // Case 515 — A Rat's Home (spooky damage)
        put(515) { ctx ->
            if (ctx.preference == 2 &&
                ctx.currentNumericModifier(DoubleModifier.SPOOKY_DAMAGE) < 20.0) 1
            else ctx.preference.takeIf { it > 0 }
        }

        // Case 549 — Dark in the Attic
        put(549) { ctx ->
            val boomboxOn = ctx.responseText.contains("sets your heart pounding and pulse racing")
            val hasShotgun = ctx.hasItem(ItemPool.SILVER_SHOTGUN_SHELL)
            when (ctx.preference) {
                0    -> null;  1, 2 -> ctx.preference;  3 -> 5
                4    -> if (!boomboxOn) 3 else 1
                5    -> if (!boomboxOn) 3 else 2
                6    -> if (!boomboxOn) 3 else 5
                7    -> if (!boomboxOn) 3 else if (hasShotgun) 5 else 2
                8    -> if (boomboxOn) 4 else 1
                9    -> if (boomboxOn) 4 else 2
                10   -> if (boomboxOn) 4 else 5
                11   -> if (boomboxOn) 4 else if (hasShotgun) 5 else 2
                else -> ctx.preference.takeIf { it > 0 }
            }
        }

        // Case 550 — The Unliving Room
        put(550) { ctx ->
            val closed   = ctx.responseText.contains("covered all their windows")
            val chainsaw = ctx.getCount(ItemPool.CHAINSAW_CHAIN)
            val mirror   = ctx.getCount(ItemPool.FUNHOUSE_MIRROR)
            when (ctx.preference) {
                0  -> null;  1 -> 3;  2 -> 4;  3 -> 5
                4  -> if (!closed) 1 else 3
                5  -> if (!closed) 1 else 4
                6  -> if (!closed) 1 else if (chainsaw > mirror) 3 else 4
                7  -> if (!closed) 1 else 5
                8  -> if (closed) 2 else 3
                9  -> if (closed) 2 else 4
                10 -> if (closed) 2 else if (chainsaw > mirror) 3 else 4
                11 -> if (closed) 2 else 5
                else -> ctx.preference.takeIf { it > 0 }
            }
        }

        // Case 551 — Debasement
        put(551) { ctx ->
            val fogOn = ctx.responseText.contains("white clouds of artificial fog")
            when (ctx.preference) {
                0, 1, 2 -> ctx.preference.takeIf { it > 0 }
                3 -> if (fogOn) 1 else 3;  4 -> if (fogOn) 2 else 3
                5 -> if (fogOn) 4 else 1;  6 -> if (fogOn) 4 else 2
                else -> ctx.preference.takeIf { it > 0 }
            }
        }

        // Case 552 — Prop Deportment
        put(552) { ctx ->
            val chainsaw = ctx.getCount(ItemPool.CHAINSAW_CHAIN)
            val mirror   = ctx.getCount(ItemPool.FUNHOUSE_MIRROR)
            when (ctx.preference) {
                4    -> if (chainsaw < mirror) 1 else 3
                else -> ctx.preference.takeIf { it > 0 }
            }
        }
    }

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
