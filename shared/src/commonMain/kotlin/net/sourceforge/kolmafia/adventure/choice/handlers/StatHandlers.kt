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
