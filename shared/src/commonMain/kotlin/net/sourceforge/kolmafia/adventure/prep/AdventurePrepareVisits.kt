package net.sourceforge.kolmafia.adventure.prep

import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

/**
 * Desktop [KoLAdventure.prepareForAdventure] PlaceRequest / GenericRequest unlock
 * clusters (Phases 1971–2015). Soft-fails when [AdventurePrepareActions.PrepareDeps.visitUrl]
 * is null.
 */
object AdventurePrepareVisits {

    suspend fun prepareUnlockVisits(
        locationName: String,
        zoneName: String?,
        ctx: AdventureGateContext,
        deps: AdventurePrepareActions.PrepareDeps,
    ) {
        val visit = deps.visitUrl ?: return

        // Degrassi Knoll / Untinker quest open
        if (locationName.contains("Degrassi Knoll", ignoreCase = true) ||
            locationName.contains("Untinker", ignoreCase = true)
        ) {
            if (!ctx.isQuestStarted(Quest.UNTINKER)) {
                visit("place.php?whichplace=forestvillage&action=fv_untinker_quest")
            }
        }

        // McLargeHuge trapper cabin
        if (zoneName?.contains("McLarge", ignoreCase = true) == true ||
            locationName.contains("Trapper", ignoreCase = true) ||
            locationName.contains("Itznotyerzitz", ignoreCase = true) ||
            locationName.contains("Goatlet", ignoreCase = true)
        ) {
            if (!ctx.isLaterThan(Quest.TRAPPER, QuestDatabase.STARTED)) {
                visit("place.php?whichplace=mclargehuge&action=trappercabin")
            }
        }

        // Pandamonium / Friars ritual
        if (zoneName?.contains("Pandamonium", ignoreCase = true) == true ||
            zoneName?.contains("Friar", ignoreCase = true) == true ||
            locationName.contains("Pandamonium", ignoreCase = true)
        ) {
            if (!ctx.isFinished(Quest.FRIAR) && !ctx.isAtLeast(Quest.FRIAR, "step3")) {
                visit("friars.php?action=ritual")
            }
            if (locationName.contains("Pandamonium", ignoreCase = true)) {
                visit("pandamonium.php")
            }
        }

        // Manor unlocks
        if (zoneName?.startsWith("Manor", ignoreCase = true) == true ||
            locationName.contains("Haunted", ignoreCase = true)
        ) {
            when {
                zoneName.equals("Manor0", ignoreCase = true) ||
                    locationName.contains("Summoning Chamber", ignoreCase = true) -> {
                    if (!ctx.isAtLeast(Quest.MANOR, "step3")) {
                        visit("place.php?whichplace=manor4&action=manor4_chamberwall")
                    }
                }
                zoneName.equals("Manor1", ignoreCase = true) -> {
                    if (!ctx.isQuestStarted(Quest.SPOOKYRAVEN_NECKLACE)) {
                        if (ctx.hasItem(ItemIds.SPOOKYRAVEN_TELEGRAM)) {
                            deps.useItemRequest?.use(ItemIds.SPOOKYRAVEN_TELEGRAM, 1)
                                ?: visit("inv_use.php?which=3&whichitem=${ItemIds.SPOOKYRAVEN_TELEGRAM}&ajax=1")
                        } else {
                            visit("place.php?whichplace=manor1&action=manor1_ladys")
                        }
                    }
                }
                zoneName.equals("Manor2", ignoreCase = true) -> {
                    if (!ctx.isLaterThan(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.STARTED)) {
                        if (ctx.hasItem(ItemIds.SPOOKYRAVEN_NECKLACE)) {
                            visit("place.php?whichplace=manor1&action=manor1_ladys")
                        }
                        if (ctx.isFinished(Quest.SPOOKYRAVEN_NECKLACE)) {
                            visit("place.php?whichplace=manor2&action=manor2_ladys")
                        }
                    }
                }
            }
        }

        // 8-Bit / Crackpot Mystic → choice 664
        if (needsMystic(locationName, zoneName, ctx)) {
            if (!ctx.hasItem(ItemIds.TRANSFUNCTIONER)) {
                visit("place.php?whichplace=forestvillage&action=fv_mystic")
                visit("choice.php?whichchoice=664&option=1")
            }
            if (ctx.hasItem(ItemIds.TRANSFUNCTIONER) &&
                !ctx.hasEquipped(ItemIds.TRANSFUNCTIONER)
            ) {
                deps.equipItem?.invoke(ItemIds.TRANSFUNCTIONER)
            }
        }

        // Knob decrypt cipher (map) when entering labyrinth/treasury without decryption
        if (locationName.contains("Cobb's Knob", ignoreCase = true) &&
            (locationName.contains("Labyrinth", ignoreCase = true) ||
                locationName.contains("Menagerie", ignoreCase = true))
        ) {
            if (!ctx.prefBool("knobDecryptProgress") &&
                ctx.prefString("questL05Goblin") == "started"
            ) {
                visit("cobbsknob.php?action=throneroom")
            }
        }
    }

    suspend fun prepareMarketNpcFallbacks(
        locationName: String,
        ctx: AdventureGateContext,
        deps: AdventurePrepareActions.PrepareDeps,
    ) {
        val visit = deps.visitUrl ?: return
        when {
            locationName.contains("Skeleton Store", ignoreCase = true) &&
                !ctx.prefBool("skeletonStoreAvailable") &&
                !ctx.hasItem(ItemIds.BONE_WITH_A_PRICE_TAG) -> {
                visit("shop.php?whichshop=meatsmith")
                visit("shop.php?whichshop=meatsmith&action=talk")
                visit("choice.php?whichchoice=1059&option=1")
            }
            locationName.contains("Madness Bakery", ignoreCase = true) &&
                !ctx.prefBool("madnessBakeryAvailable") &&
                !ctx.hasItem(ItemIds.HYPNOTIC_BREADCRUMBS) -> {
                visit("shop.php?whichshop=armory")
                visit("shop.php?whichshop=armory&action=talk")
                visit("choice.php?whichchoice=1065&option=1")
            }
            locationName.contains("Overgrown Lot", ignoreCase = true) &&
                !ctx.prefBool("overgrownLotAvailable") &&
                !ctx.hasItem(ItemIds.BOOZE_MAP) -> {
                visit("shop.php?whichshop=doc")
                visit("shop.php?whichshop=doc&action=talk")
                visit("choice.php?whichchoice=1060&option=1")
            }
        }
    }

    /** Desktop [KoLAdventure.buildDinghy] — use plans when planks present. */
    suspend fun buildDinghy(ctx: AdventureGateContext, deps: AdventurePrepareActions.PrepareDeps): Boolean {
        if (AdventureUnlockHelpers.islandAccessible(ctx)) return true
        if (ctx.hasItem(ItemIds.DINGHY_PLANS) && ctx.hasItem(ItemIds.DINGY_PLANKS)) {
            val use = deps.useItemRequest
            if (use != null) {
                use.use(ItemIds.DINGHY_PLANS, 1)
                return true
            }
            deps.visitUrl?.invoke("inv_use.php?which=3&whichitem=${ItemIds.DINGHY_PLANS}&ajax=1")
            return true
        }
        return false
    }

    private fun needsMystic(
        locationName: String,
        zoneName: String?,
        ctx: AdventureGateContext,
    ): Boolean {
        if (locationName.contains("8-Bit", ignoreCase = true) ||
            zoneName?.contains("8-Bit", ignoreCase = true) == true ||
            locationName.contains("Vanya", ignoreCase = true)
        ) {
            return true
        }
        // Shadow Rift in 8-bit: still need transfunctioner unless ingress already 8bit
        if (locationName.contains("Shadow Rift", ignoreCase = true) &&
            ctx.prefString("shadowRiftIngress").equals("8bit", ignoreCase = true).not() &&
            locationName.contains("8", ignoreCase = true)
        ) {
            return true
        }
        return false
    }
}
