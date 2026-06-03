package net.sourceforge.kolmafia.adventure.choice.handlers

import net.sourceforge.kolmafia.adventure.choice.ChoiceContext
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandler
import net.sourceforge.kolmafia.adventure.choice.ChoiceHandlerRegistry
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.MainStat

object InventoryHandlers {
    val handlers: Map<Int, ChoiceHandler> = buildMap {

        // Case 5 — Heart of Very, Very Dark Darkness
        put(5) { ctx -> if (ctx.hasItem(ItemPool.INEXPLICABLY_GLOWING_ROCK)) 1 else 2 }

        // Case 7 — How Depressing
        put(7) { ctx -> if (ctx.hasEquipped(ItemPool.SPOOKY_GLOVE)) 1 else 2 }

        // Case 127 — No sir, away! A papaya war is on!
        put(127) { ctx ->
            when (ctx.preference) {
                4    -> if (ctx.getCount(ItemPool.PAPAYA) >= 3) 2 else 1
                5    -> if (ctx.getCount(ItemPool.PAPAYA) >= 3) 2 else 3
                else -> ctx.preference.takeIf { it > 0 }
            }
        }

        // Case 161 — Bureaucracy of the Damned (Azazel)
        put(161) { ctx ->
            val hasAll = listOf(ItemPool.AZAZEL_OBJECT_1, ItemPool.AZAZEL_OBJECT_2,
                                ItemPool.AZAZEL_OBJECT_3).all { ctx.hasItem(it) }
            if (hasAll) 1 else 4
        }

        // Case 191 — Chatterboxing
        put(191) { ctx ->
            val hasTrinket = ctx.hasItem(ItemPool.VALUABLE_TRINKET)
            when (ctx.preference) {
                5 -> if (hasTrinket) 2 else 1
                6 -> if (hasTrinket) 2 else 3
                7 -> if (hasTrinket) 2 else 4
                8 -> if (hasTrinket) 2 else when (ctx.mainStat) {
                    MainStat.MUSCLE      -> 3
                    MainStat.MYSTICALITY -> 4
                    MainStat.MOXIE       -> 1
                }
                else -> ctx.preference.takeIf { it > 0 }
            }
        }

        // Case 298 — In the Shade
        put(298) { ctx ->
            if (ctx.preference == 1 &&
                (ctx.getCount(ItemPool.SEED_PACKET) < 1 || ctx.getCount(ItemPool.GREEN_SLIME) < 1)) 2
            else ctx.preference.takeIf { it > 0 }
        }

        // Case 305 — There is Sauce at the Bottom of the Ocean
        put(305) { ctx ->
            if (ctx.preference == 1 && !ctx.hasItem(ItemPool.MERKIN_PRESSUREGLOBE)) 2
            else ctx.preference.takeIf { it > 0 }
        }

        // Case 504 — Tree's Last Stand
        put(504) { ctx ->
            if (!ctx.hasItem(ItemPool.SPOOKY_SAPLING)
                && !ctx.prefBool("spookyTempleUnlocked")
                && ctx.availableMeat >= 100) 3 else 4
        }

        // Case 553 — Relocked and Reloaded
        put(553) { ctx ->
            when (ctx.preference) {
                0 -> null
                6 -> 6
                else -> {
                    val itemId = when (ctx.preference) {
                        1 -> ItemPool.MAXWELL_HAMMER
                        2 -> ItemPool.TONGUE_BRACELET
                        3 -> ItemPool.SILVER_CHEESE_SLICER
                        4 -> ItemPool.SILVER_SHRIMP_FORK
                        5 -> ItemPool.SILVER_PATE_KNIFE
                        else -> 0
                    }
                    if (itemId == 0) 6 else if (ctx.hasItem(itemId)) ctx.preference else 6
                }
            }
        }

        // Case 558 — Tool Time
        put(558) { ctx ->
            when (ctx.preference) {
                0, 6 -> ctx.preference.takeIf { it > 0 }
                else -> if (ctx.getCount(ItemPool.LOLLIPOP_STICK) >= 3 + ctx.preference) ctx.preference else 6
            }
        }

        // Case 692 — I Wanna Be a Door
        put(692) { ctx ->
            when (ctx.preference) {
                11 -> when {
                    ctx.hasItem(ItemPool.EXPRESS_CARD)         -> 7
                    ctx.hasItem(ItemPool.PICKOMATIC_LOCKPICKS) -> 3
                    ctx.hasItem(ItemPool.SKELETON_KEY)         -> 2
                    else                                        -> null
                }
                12 -> when {
                    ctx.buffedMusc >= ctx.buffedMyst && ctx.buffedMusc >= ctx.buffedMoxie -> 4
                    ctx.buffedMyst >= ctx.buffedMusc && ctx.buffedMyst >= ctx.buffedMoxie -> 5
                    else -> 6
                }
                else -> ctx.preference.takeIf { it > 0 }
            }
        }

        // Case 786 — Working Holiday (Hidden Office)
        put(786) { ctx ->
            if (ctx.preference != 1) return@put ctx.preference.takeIf { it > 0 }
            val progress      = ctx.prefInt("hiddenOfficeProgress")
            val hasFile       = ctx.hasItem(ItemPool.MCCLUSKY_FILE)
            val hasPage5      = ctx.hasItem(ItemPool.MCCLUSKY_FILE_PAGE5)
            val hasBinderClip = ctx.hasItem(ItemPool.BINDER_CLIP)
            when {
                progress >= 7  -> 3
                hasFile        -> 1
                !hasBinderClip -> 2
                !hasPage5      -> 3
                else           -> null
            }
        }

        // Case 791 — Legend of the Temple in the Hidden City
        put(791) { ctx ->
            if (ctx.preference == 1 && ctx.getCount(ItemPool.STONE_TRIANGLE) < 4) 6
            else ctx.preference.takeIf { it > 0 }
        }

        // Case 1091 — The Floor Is Yours (LavaCo)
        put(1091) { ctx ->
            val required = mapOf(
                1 to ItemPool.GOLD_1970, 2 to ItemPool.NEW_AGE_HEALING_CRYSTAL,
                3 to ItemPool.EMPTY_LAVA_BOTTLE, 4 to ItemPool.VISCOUS_LAVA_GLOBS,
                5 to ItemPool.GLOWING_NEW_AGE_CRYSTAL,
            )
            val r = required[ctx.preference]
            if (r != null && !ctx.hasItem(r)) null
            else ctx.preference.takeIf { it > 0 }
        }

        // Case 1489 — Slagging Off (Crimbo crystal)
        put(1489) { ctx ->
            if (!ctx.hasItem(ItemPool.CRIMBO_CRYSTAL_SHARDS)) return@put 3
            when (ctx.preference) {
                1, 2 -> ctx.preference
                else -> {
                    val goblets  = ctx.getCount(ItemPool.CRYSTAL_CRIMBO_GOBLET)
                    val platters = ctx.getCount(ItemPool.CRYSTAL_CRIMBO_PLATTER)
                    if (goblets <= platters) 1 else 2
                }
            }
        }
    }

    fun registerAll(registry: ChoiceHandlerRegistry) =
        handlers.forEach { (id, h) -> registry.register(id, h) }
}
