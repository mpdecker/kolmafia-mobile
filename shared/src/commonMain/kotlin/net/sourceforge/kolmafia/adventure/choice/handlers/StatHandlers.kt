package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.adventure.choice.EffectPool
import net.sourceforge.kolmafia.adventure.choice.OutfitPool
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.MainStat

object StatHandlers {
    val handlers: Map<Int, ChoiceHandler> = buildMap {

        // Case 89 — Out in the Garden (Maidens)
        put(89) { ctx ->
            val hasMaiden = ctx.hasEffect(EffectPool.MAIDEN_EFFECT)
            when (ctx.preference) {
                0    -> (1..2).random()
                1, 2 -> ctx.preference
                3    -> if (hasMaiden) (1..2).random() else 3
                4    -> if (hasMaiden) 1 else 3
                5    -> if (hasMaiden) 2 else 3
                6    -> 4
                else -> ctx.preference.takeIf { it > 0 }
            }
        }

        // Case 162 — Between a Rock and Some Other Rocks (mining)
        // The desktop always auto-selects this choice: option 1 (drill for ore) requires
        // a mining outfit/path boost, option 3 requires Axecore, else pick up ore manually (2).
        // Preference 0 (manual) is intentionally not distinguished; falling through to option 2
        // is always safe because picking up ore by hand never costs a turn.
        put(162) { ctx ->
            when {
                ctx.preference == 2                                              -> 2
                ctx.isWearingOutfit(OutfitPool.MINING_OUTFIT)                   -> 1
                ctx.isFistcore && ctx.hasEffect(EffectPool.EARTHEN_FIST)        -> 1
                ctx.isAxecore                                                    -> 3
                else                                                             -> 2
            }
        }

        // Case 184 — That Explains All The Eyepatches
        // The "best" stat-gain option rotates based on prime stat. Preferences 4/5/6 encode
        // the player's priority order (4=first choice, 5=second, 6=third). The when-key
        // encodes (primeIndex * 10 + preference):
        //   primeIndex: Muscle=0, Mysticality=1, Moxie=2
        //   Muscle:     pref 4→opt 3 (Mus), 5→opt 2 (Mys), 6→opt 1 (Mox)
        //   Myst:       pref 4→opt 1 (Mox), 5→opt 2 (Mys), 6→opt 3 (Mus)
        //   Moxie:      pref 4→opt 2 (Mys), 5→opt 3 (Mus), 6→opt 1 (Mox)
        put(184) { ctx ->
            val prime = when (ctx.mainStat) {
                MainStat.MUSCLE      -> 0
                MainStat.MYSTICALITY -> 1
                MainStat.MOXIE       -> 2
            }
            when (prime * 10 + ctx.preference) {
                4  -> 3;  5  -> 2;  6  -> 1
                14 -> 1;  15 -> 2;  16 -> 3
                24 -> 2;  25 -> 3;  26 -> 1
                else -> ctx.preference.takeIf { it > 0 }
            }
        }

        // Case 700 — Delirium in the Cafeterium
        put(700) { ctx ->
            if (ctx.preference == 1) when {
                ctx.hasEffect(EffectPool.JOCK_EFFECT) -> 1
                ctx.hasEffect(EffectPool.NERD_EFFECT) -> 2
                else                                  -> 3
            } else ctx.preference.takeIf { it > 0 }
        }

        // Case 1049 — Tomb of the Unknown Your Class Here
        put(1049) { ctx ->
            if (ctx.options.size == 1) return@put 1
            val answer = when (ctx.characterClass) {
                CharacterClass.SEAL_CLUBBER    -> "Boredom."
                CharacterClass.TURTLE_TAMER    -> "Friendship."
                CharacterClass.PASTAMANCER     -> "Binding pasta thralls."
                CharacterClass.SAUCEROR        -> "Power."
                CharacterClass.DISCO_BANDIT    -> "Me. Duh."
                CharacterClass.ACCORDION_THIEF -> "Music."
                else -> return@put null
            }
            ctx.options.entries.firstOrNull { (_, v) -> v.contains(answer) }?.key
        }

        // Case 1087 — The Dark and Dank and Sinister Cave Entrance
        put(1087) { ctx ->
            if (ctx.options.size == 1) return@put 1
            val answer = when (ctx.characterClass) {
                CharacterClass.SEAL_CLUBBER    -> "Freak the hell out like a wrathful wolverine."
                CharacterClass.TURTLE_TAMER    -> "Sympathize with an amphibian."
                CharacterClass.PASTAMANCER     -> "Entangle the wall with noodles."
                CharacterClass.SAUCEROR        -> "Shoot a stream of sauce at the wall."
                CharacterClass.DISCO_BANDIT    -> "Focus on your disco state of mind."
                CharacterClass.ACCORDION_THIEF -> "Bash the wall with your accordion."
                else -> return@put null
            }
            ctx.options.entries.firstOrNull { (_, v) -> v.contains(answer) }?.key
        }
    }

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
