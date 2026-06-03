package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.adventure.choice.ItemPool

object MiscHandlers {
    val handlers: Map<Int, ChoiceHandler> = buildMap {

        // Case 182 — Random Lack of an Encounter (Model Airship)
        // Preferences 1–3 are a straightforward option index. Preferences ≥ 4 use a
        // bit-flag encoding where bit 2 (value 4) means "prefer option 4 if available":
        //   pref 4 (binary 100) → option 4 when available, else option 4-3=1
        //   pref 5 (binary 101) → option 4 when available, else option 5-3=2
        //   pref 6 (binary 110) → option 4 when available, else option 6-3=3
        // option4Mask collapses to 0 when option 4 is absent, making the AND always false.
        put(182) { ctx ->
            val option4Available = ctx.responseText.contains("Gallivant down to the head")
            val option4Mask = if (option4Available) 4 else 0
            when {
                option4Available && ctx.hasItemGoal(ItemPool.MODEL_AIRSHIP) -> 4
                ctx.preference < 4 -> ctx.preference.takeIf { it > 0 }
                (ctx.preference and option4Mask) > 0 -> 4
                else -> (ctx.preference - 3).takeIf { it > 0 }
            }
        }

        put(690) { ctx -> ctx.preference.takeIf { it > 0 } }
        put(691) { ctx -> ctx.preference.takeIf { it > 0 } }
        put(693) { ctx -> ctx.preference.takeIf { it > 0 } }

        // Case 879 — One Rustic Nightstand
        put(879) { ctx ->
            val sausagesAvailable = ctx.responseText.contains("Check under the nightstand")
            if (ctx.preference == 4) {
                return@put if (sausagesAvailable) 4 else 1
            }
            for (itemId in ItemPool.MISTRESS_ITEM_IDS) {
                if (ctx.hasItemGoal(itemId)) return@put 3
            }
            ctx.preference.takeIf { it > 0 }
        }

        // Case 914 — Louvre reset + goal check
        put(914) { ctx -> if (ctx.prefInt("louvreGoal") != 0) 1 else 2 }

        // Case 988 — The Containment Unit (EVE directions)
        put(988) { ctx ->
            val containment = ctx.prefString("EVEDirections")
            if (containment.length != 6) return@put ctx.preference.takeIf { it > 0 }
            val progress = containment.last().digitToIntOrNull()
                ?: return@put ctx.preference.takeIf { it > 0 }
            if (progress !in 0..5) return@put ctx.preference.takeIf { it > 0 }
            when (containment[progress]) {
                'L' -> 1;  'R' -> 2
                else -> ctx.preference.takeIf { it > 0 }
            }
        }

        // Case 989 — Paranormal Test Lab
        put(989) { ctx ->
            when {
                ctx.responseText.contains("ever-changing constellation") -> 1
                ctx.responseText.contains("card in the circle of light") -> 2
                ctx.responseText.contains("waves a fly away")            -> 3
                ctx.responseText.contains("back to square one")          -> 4
                ctx.responseText.contains("adds to your anxiety")        -> 5
                else -> null
            }
        }
    }

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
