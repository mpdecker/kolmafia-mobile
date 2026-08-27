package net.sourceforge.kolmafia.adventure.prep

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

/**
 * Desktop [KoLAdventure.preValidateAdventure] + [KoLAdventure.canAdventure] high-traffic
 * zone routers (Phases 1851–1895).
 */
object AdventureZoneGates {

    fun tooDrunkToAdventure(
        locationName: String,
        zone: AdventureZone?,
        ctx: AdventureGateContext,
    ): Boolean {
        val cs = ctx.character ?: return false
        if (!cs.isFallingDown) return false

        if (ctx.hasEquipped(ItemIds.DRUNKULA_WINEGLASS)) {
            if (zone?.hasSnarfblat() == true) return false
            if (zone?.zoneName.equals("Shadow Rift", ignoreCase = true) == true) return false
        }

        val mode = cs.limitMode.lowercase()
        if (mode.contains("spelunk") || mode.contains("batman") || mode == "batfellow") {
            return false
        }

        if (zone?.isOverdrunk == true) return false
        // Named overdrunk-allowed locations
        if (locationName.equals("Drunken Stupor", ignoreCase = true)) return false

        return true
    }

    fun preValidateAdventure(
        locationName: String,
        zone: AdventureZone?,
        ctx: AdventureGateContext,
    ): Boolean {
        if (tooDrunkToAdventure(locationName, zone, ctx)) return false
        val z = zone?.zoneName ?: return true

        return when {
            z.equals("Spring Break Beach", ignoreCase = true) ->
                AdventureUnlockHelpers.checkZoneAccess("sleazeAirportAlways", "_sleazeAirportToday", ctx)
            z.equals("Conspiracy Island", ignoreCase = true) ->
                AdventureUnlockHelpers.checkZoneAccess("spookyAirportAlways", "_spookyAirportToday", ctx)
            z.equals("Dinseylandfill", ignoreCase = true) ->
                AdventureUnlockHelpers.checkZoneAccess("stenchAirportAlways", "_stenchAirportToday", ctx)
            z.equals("That 70s Volcano", ignoreCase = true) ->
                AdventureUnlockHelpers.checkZoneAccess("hotAirportAlways", "_hotAirportToday", ctx)
            z.equals("The Glaciest", ignoreCase = true) ->
                AdventureUnlockHelpers.checkZoneAccess("coldAirportAlways", "_coldAirportToday", ctx)
            z.equals("Gingerbread City", ignoreCase = true) ->
                AdventureUnlockHelpers.checkZoneAccess(
                    "gingerbreadCityAvailable",
                    "_gingerbreadCityToday",
                    ctx,
                )
            z.equals("LT&T", ignoreCase = true) || z.equals("LT&T Office", ignoreCase = true) ->
                AdventureUnlockHelpers.checkZoneAccess(
                    "telegraphOfficeAvailable",
                    "_telegraphOfficeToday",
                    ctx,
                )
            z.equals("Neverending Party", ignoreCase = true) -> {
                if (ctx.character?.ascensionPath == AscensionPath.LEGACY_OF_LOATHING &&
                    ctx.prefBool("replicaNeverendingPartyAlways")
                ) {
                    true
                } else {
                    AdventureUnlockHelpers.checkZoneAccess(
                        "neverendingPartyAlways",
                        "_neverendingPartyToday",
                        ctx,
                    )
                }
            }
            z.equals("FantasyRealm", ignoreCase = true) ->
                AdventureUnlockHelpers.checkZoneAccess("frAlways", "_frToday", ctx)
            z.startsWith("PirateRealm", ignoreCase = true) ->
                AdventureUnlockHelpers.checkZoneAccess("prAlways", "_prToday", ctx)
            z.equals("Server Room", ignoreCase = true) ->
                AdventureUnlockHelpers.checkZoneAccess("crAlways", "_crToday", ctx)
            z.equals("Tunnel of L.O.V.E.", ignoreCase = true) ||
                z.contains("L.O.V.E", ignoreCase = true) ->
                AdventureUnlockHelpers.checkZoneAccess("loveTunnelAvailable", "_loveTunnelToday", ctx)
            z.equals("Twitch", ignoreCase = true) ||
                z.equals("Time Twitching Tower", ignoreCase = true) ->
                ctx.prefBool("timeTowerAvailable")
            z.equals("Speakeasy", ignoreCase = true) ->
                ctx.prefBool("ownsSpeakeasy") || ctx.hasItem(ItemIds.MILK_CAP)
            z.equals("The Spacegate", ignoreCase = true) ||
                z.equals("Spacegate", ignoreCase = true) -> {
                if (ctx.isKoE) return false
                ctx.prefBool("spacegateAlways") ||
                    ctx.prefBool("_spacegateToday") ||
                    ctx.hasItem(ItemIds.OPEN_PORTABLE_SPACEGATE)
            }
            z.equals("The Sea Floor", ignoreCase = true) ||
                z.equals("The Sea", ignoreCase = true) ->
                ctx.isQuestStarted(Quest.SEA_OLD_GUY)
            else -> true
        }
    }

    fun canAdventureZone(
        locationName: String,
        zone: AdventureZone,
        ctx: AdventureGateContext,
    ): Boolean {
        if (!preValidateAdventure(locationName, zone, ctx)) return false

        // Shadow Rift
        if (zone.zoneName.equals("Shadow Rift", ignoreCase = true)) {
            return canShadowRift(locationName, ctx)
        }

        // Limit modes / astral / mole (path zones)
        if (zone.zoneName.equals("Astral", ignoreCase = true) ||
            locationName.contains("Astral", ignoreCase = true)
        ) {
            return ctx.character?.limitMode?.contains("astral", ignoreCase = true) == true ||
                ctx.prefString("currentAstralTrip").isNotEmpty()
        }
        if (zone.zoneName.contains("Shape of Mole", ignoreCase = true) ||
            locationName.contains("Mt. Molehill", ignoreCase = true)
        ) {
            return ctx.character?.limitMode?.contains("mole", ignoreCase = true) == true
        }

        // Grimstone / psychoses — can-gate only (prepare must not use the jar/mask)
        if (zone.zoneName.contains("Grimstone", ignoreCase = true)) {
            return ctx.prefBool("grimstoneAvailable") ||
                ctx.prefString("grimstoneZone").isNotEmpty()
        }
        if (zone.zoneName.contains("Psychoses", ignoreCase = true)) {
            return ctx.prefString("currentPsychoses").isNotEmpty()
        }

        // Core kingdom by zone name
        return when {
            zone.zoneName.equals("Town", ignoreCase = true) -> canTown(locationName, ctx)
            zone.zoneName.equals("Campground", ignoreCase = true) ->
                ctx.hasCampground
            zone.zoneName.startsWith("Manor", ignoreCase = true) ->
                canManor(locationName, zone.zoneName, ctx)
            zone.zoneName.equals("Mountain", ignoreCase = true) ||
                zone.zoneName.equals("Mountains", ignoreCase = true) -> true
            zone.zoneName.equals("Plains", ignoreCase = true) -> true
            zone.zoneName.equals("Woods", ignoreCase = true) ||
                zone.zoneName.equals("The Distant Woods", ignoreCase = true) ->
                AdventureUnlockHelpers.woodsOpen(ctx) || ctx.hasItem(ItemIds.TRANSFUNCTIONER)
            zone.zoneName.equals("Beach", ignoreCase = true) ||
                zone.zoneName.equals("Desert Beach", ignoreCase = true) ->
                AdventureUnlockHelpers.desertBeachAccessible(ctx)
            zone.zoneName.equals("Island", ignoreCase = true) ||
                zone.zoneName.contains("IsleWar", ignoreCase = true) ||
                zone.zoneName.contains("Island War", ignoreCase = true) ->
                AdventureUnlockHelpers.islandAccessible(ctx)
            zone.zoneName.equals("BatHole", ignoreCase = true) ||
                zone.zoneName.contains("Bat Hole", ignoreCase = true) ->
                ctx.isQuestStarted(Quest.BAT)
            zone.zoneName.contains("Knob", ignoreCase = true) ->
                ctx.isQuestStarted(Quest.GOBLIN) || ctx.level >= 5
            zone.zoneName.contains("Friar", ignoreCase = true) ||
                zone.zoneName.contains("Pandamonium", ignoreCase = true) ->
                ctx.isQuestStarted(Quest.FRIAR) || ctx.level >= 6
            zone.zoneName.contains("Cyrpt", ignoreCase = true) ||
                zone.zoneName.contains("Misspelled Cemetary", ignoreCase = true) ->
                AdventureUnlockHelpers.cemeteryOpen(ctx)
            zone.zoneName.contains("McLarge", ignoreCase = true) ||
                zone.zoneName.contains("Highlands", ignoreCase = true) ->
                ctx.isQuestStarted(Quest.TRAPPER) || ctx.isAtLeast(Quest.TOPPING, QuestDatabase.STARTED)
            zone.zoneName.contains("Beanstalk", ignoreCase = true) ||
                zone.zoneName.contains("Castle in the Clouds", ignoreCase = true) ->
                canBeanstalk(ctx)
            zone.zoneName.contains("Hidden City", ignoreCase = true) ||
                zone.zoneName.contains("Hidden Apartment", ignoreCase = true) ||
                zone.zoneName.contains("Hidden Hospital", ignoreCase = true) ||
                zone.zoneName.contains("Hidden Office", ignoreCase = true) ||
                zone.zoneName.contains("Hidden Park", ignoreCase = true) ->
                ctx.isLaterThan(Quest.WORSHIP, "step2") || ctx.isAtLeast(Quest.WORSHIP, "step3")
            zone.zoneName.contains("Pyramid", ignoreCase = true) ->
                ctx.isQuestStarted(Quest.PYRAMID)
            zone.zoneName.contains("Palindome", ignoreCase = true) ->
                ctx.isQuestStarted(Quest.PALINDOME)
            zone.zoneName.contains("Pirate", ignoreCase = true) &&
                !zone.zoneName.startsWith("PirateRealm", ignoreCase = true) ->
                ctx.isQuestStarted(Quest.PIRATE) || ctx.hasItem(ItemIds.TRANSFUNCTIONER)
            zone.zoneName.contains("Volcano", ignoreCase = true) &&
                !zone.zoneName.contains("70s", ignoreCase = true) ->
                ctx.isAtLeast(Quest.NEMESIS, "step20") || ctx.isFinished(Quest.NEMESIS)
            zone.zoneName.contains("Snojo", ignoreCase = true) ->
                ctx.prefBool("snojoAvailable") || ctx.prefBool("_snojoFreeFights")
            zone.zoneName.contains("Spaaace", ignoreCase = true) ||
                zone.zoneName.contains("Hole in the Sky", ignoreCase = true) ->
                ctx.hasItem(ItemIds.TRANSFUNCTIONER) || AdventureUnlockHelpers.woodsOpen(ctx)
            zone.zoneName.contains("Rabbit Hole", ignoreCase = true) ->
                ctx.prefBool("rabbitHoleAvailable") || ctx.prefString("lastRabbitHole").isNotEmpty()
            zone.zoneName.contains("DMT", ignoreCase = true) ||
                zone.zoneName.contains("Deep Machine Tunnels", ignoreCase = true) ->
                ctx.prefBool("deepMachineTunnelsAvailable") ||
                    ctx.prefBool("_dmtToday")
            zone.zoneName.contains("Video Game", ignoreCase = true) ||
                zone.zoneName.contains("GameInformPower", ignoreCase = true) ->
                ctx.prefBool("hasDetectiveSchool") || ctx.prefBool("lolCampusAvailable")
            // Path residuals
            ctx.character?.ascensionPath == AscensionPath.KOLHS &&
                zone.zoneName.contains("KOLHS", ignoreCase = true) -> true
            ctx.character?.ascensionPath == AscensionPath.GREY_YOU &&
                zone.zoneName.contains("Grey", ignoreCase = true) -> true
            else -> true
        }
    }

    private fun canShadowRift(locationName: String, ctx: AdventureGateContext): Boolean {
        val ingress = ctx.prefString("shadowRiftIngress")
        // Generic Shadow Rift requires prior ingress
        if (locationName.equals("Shadow Rift", ignoreCase = true) ||
            locationName.equals("The Shadow Rift", ignoreCase = true)
        ) {
            return ingress.isNotEmpty()
        }
        // Specific rifts — match ingress or allow open map rifts
        return when {
            locationName.contains("Town", ignoreCase = true) ||
                locationName.contains("Plains", ignoreCase = true) -> true
            locationName.contains("Woods", ignoreCase = true) ||
                locationName.contains("Village", ignoreCase = true) ||
                locationName.contains("8-Bit", ignoreCase = true) ->
                AdventureUnlockHelpers.woodsOpen(ctx)
            locationName.contains("Manor", ignoreCase = true) ->
                ctx.isLaterThan(Quest.SPOOKYRAVEN_DANCE, "step3")
            locationName.contains("Cemetary", ignoreCase = true) ||
                locationName.contains("Cemetery", ignoreCase = true) ->
                AdventureUnlockHelpers.cemeteryOpen(ctx)
            locationName.contains("Beach", ignoreCase = true) ->
                AdventureUnlockHelpers.desertBeachAccessible(ctx)
            locationName.contains("McLarge", ignoreCase = true) ->
                ctx.isQuestStarted(Quest.TRAPPER)
            locationName.contains("Beanstalk", ignoreCase = true) -> canBeanstalk(ctx)
            locationName.contains("Castle", ignoreCase = true) ->
                ctx.isKoE || ctx.isFinished(Quest.GARBAGE) || ctx.hasItem(ItemIds.SOCK)
            locationName.contains("City", ignoreCase = true) ->
                ctx.isLaterThan(Quest.WORSHIP, "step2")
            locationName.contains("Pyramid", ignoreCase = true) ->
                ctx.isQuestStarted(Quest.PYRAMID)
            else -> ingress.isNotEmpty()
        }
    }

    private fun canBeanstalk(ctx: AdventureGateContext): Boolean {
        if (ctx.isKoE) return false
        return ctx.isLaterThan(Quest.GARBAGE, QuestDatabase.STARTED) ||
            (ctx.isQuestStarted(Quest.GARBAGE) && ctx.hasItem(ItemIds.ENCHANTED_BEAN)) ||
            ctx.isFinished(Quest.GARBAGE) ||
            ctx.hasItem(ItemIds.SOCK)
    }

    private fun canTown(locationName: String, ctx: AdventureGateContext): Boolean = when {
        locationName.contains("Sleazy Back Alley", ignoreCase = true) -> true
        locationName.contains("Overgrown Lot", ignoreCase = true) ->
            ctx.prefBool("overgrownLotAvailable") ||
                ctx.hasItem(ItemIds.BOOZE_MAP) ||
                !ctx.isKoE
        locationName.contains("Madness Bakery", ignoreCase = true) ->
            ctx.prefBool("madnessBakeryAvailable") ||
                ctx.hasItem(ItemIds.HYPNOTIC_BREADCRUMBS) ||
                !ctx.isKoE
        locationName.contains("Skeleton Store", ignoreCase = true) ->
            ctx.prefBool("skeletonStoreAvailable") ||
                ctx.hasItem(ItemIds.BONE_WITH_A_PRICE_TAG) ||
                !ctx.isKoE
        locationName.contains("Copperhead Club", ignoreCase = true) ->
            ctx.isQuestStarted(Quest.SHEN)
        locationName.contains("Super Villain", ignoreCase = true) ->
            ctx.character?.ascensionPath == AscensionPath.LICENSE_TO_ADVENTURE
        else -> true
    }

    private fun canManor(locationName: String, zoneName: String, ctx: AdventureGateContext): Boolean {
        when {
            zoneName.equals("Manor0", ignoreCase = true) ->
                return ctx.isLaterThan(Quest.MANOR, QuestDatabase.STARTED) ||
                    ctx.isAtLeast(Quest.MANOR, "step1")
            zoneName.equals("Manor1", ignoreCase = true) -> {
                val neededLevel = if (ctx.ascensions > 0) 0 else 5
                return when {
                    locationName.contains("Pantry", ignoreCase = true) -> true
                    locationName.contains("Kitchen", ignoreCase = true) ||
                        locationName.contains("Conservatory", ignoreCase = true) ->
                        ctx.isQuestStarted(Quest.SPOOKYRAVEN_NECKLACE) ||
                            ctx.hasItem(ItemIds.SPOOKYRAVEN_TELEGRAM) ||
                            ctx.level >= neededLevel
                    locationName.contains("Library", ignoreCase = true) ->
                        ctx.hasItem(ItemIds.LIBRARY_KEY)
                    locationName.contains("Billiard", ignoreCase = true) ->
                        ctx.hasItem(ItemIds.BILLIARDS_KEY)
                    else -> true
                }
            }
            zoneName.equals("Manor2", ignoreCase = true) -> {
                val neededLevel = if (ctx.ascensions > 0) 0 else 7
                return ctx.isLaterThan(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.STARTED) ||
                    (ctx.level >= neededLevel &&
                        (ctx.hasItem(ItemIds.SPOOKYRAVEN_NECKLACE) ||
                            ctx.isFinished(Quest.SPOOKYRAVEN_NECKLACE)))
            }
            zoneName.equals("Manor3", ignoreCase = true) ->
                return ctx.isFinished(Quest.SPOOKYRAVEN_DANCE) ||
                    ctx.isAtLeast(Quest.SPOOKYRAVEN_DANCE, "step3")
            else -> return ctx.prefBool("spookyravenManorUnlocked") ||
                ctx.isQuestStarted(Quest.MANOR) ||
                locationName.contains("Pantry", ignoreCase = true)
        }
    }
}
