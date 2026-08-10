package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.campground.CampAwayAvailability
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.data.CandyDatabase
import net.sourceforge.kolmafia.data.ConcoctionMayoQueue
import net.sourceforge.kolmafia.data.HotDogDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.MayamAvailability
import net.sourceforge.kolmafia.data.PocketDatabase
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.data.SpeakeasyDatabase
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.mood.BreakfastBurnSkills
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.ItemUseLimitsContext
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.request.maximumUses
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.session.BeachHeadAvailability
import net.sourceforge.kolmafia.session.BreakfastItemIds
import net.sourceforge.kolmafia.session.BreakfastManager
import net.sourceforge.kolmafia.session.DemonTypes
import net.sourceforge.kolmafia.session.RabbitHoleAvailability
import net.sourceforge.kolmafia.session.SkateParkAvailability

/** Pluggable exotic effect-source rules (Phase 389–397). Desktop Maximizer.java ~593–1444. */
object MaximizerBoostSourceRules {

    private const val PILL_KEEPER_ID = 10333
    private const val PROTON_PACK_ID = 9082
    private const val MONKEY_PAW_ID = 11186
    private const val POCKET_WISH_ID = 9537
    private const val DECK_OF_EVERY_CARD_ID = 8382
    private const val REPLICA_DECK_ID = 11230
    private const val SKELETON_ID = 5881
    private const val GREAT_PANTS_ID = 4696
    private const val REPLICA_GREAT_PANTS_ID = 11209
    private const val GRIM_BROTHER_ID = 179
    private const val SOURCE_TERMINAL_ID = 9033
    private const val ASDON_MARTIN_ID = 9508
    private const val SWEET_SYNTHESIS_ID = 166
    private const val EFFECT_SUPERFICIALLY_INTERESTED = 2288
    private const val EFFECT_INTENSELY_INTERESTED = 2289

    data class SourceRuleContext(
        val base: MaximizerNonEquipmentBoosts.Context,
        val effectId: Int,
        val effectName: String,
        val source: String,
        val hasEffect: Boolean,
    ) {
        val includeAll: Boolean get() = base.includeAll
        val charState: CharacterState get() = base.charState
        val preferences: Preferences? get() = base.preferences
    }

    data class SourceRuleResult(
        val cmd: String? = null,
        val text: String? = null,
        val duration: Int? = null,
        val usesRemaining: Int? = null,
        val itemsRemaining: Int? = null,
        val itemsCreatable: Int? = null,
        val extraCosts: MaximizerBoostCostSuffix.BoostCosts? = null,
        val skip: Boolean = false,
    )

    private interface SourceRule {
        fun apply(ctx: SourceRuleContext): SourceRuleResult?
    }

    private val rules: List<SourceRule> = listOf(
        CastSourceRule,
        CargoEffectRule,
        BarrelPrayerRule,
        PhotoBoothEffectRule,
        AlliedRadioEffectRule,
        HotDogEatRule,
        SpeakeasyDrinkRule,
        ConsumptionSourceRule,
        FriarsRule,
        PillkeeperRule,
        PoolRule,
        ShowerRule,
        SwimRule,
        JukeboxRule,
        BallpitRule,
        TelescopeRule,
        FortuneRule,
        MomRule,
        ConcertRule,
        SummonRule,
        MayosoakRule,
        WitchessRule,
        MonorailRule,
        ToggleRule,
        CrossstreamsRule,
        MonkeyPawEffectRule,
        GenieEffectRule,
        GongRule,
        StyxRule,
        PlayRule,
        SkeletonRule,
        GapRule,
        SpacegateRule,
        DaycareRule,
        Vault3Rule,
        GrimRule,
        AprilingBandRule,
        TerminalEnhanceRule,
        CampAwayCloudRule,
        LoathingIdolRule,
        MayamRule,
        AsdonMartinDriveRule,
        BeachHeadRule,
        SkateRule,
        HatterRule,
        SynthesizeRule,
    )

    fun apply(ctx: SourceRuleContext): SourceRuleResult? {
        for (rule in rules) {
            val result = rule.apply(ctx) ?: continue
            return result
        }
        return null
    }

    private object CastSourceRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("cast ", ignoreCase = true)) return null
            val skillName = UneffectSkillEffectMap.effectToSkill(ctx.effectName)
                ?: parseCastSkillName(ctx.source)
                ?: return SourceRuleResult(skip = true)
            if (!StandardRequest.isAllowed(
                    RestrictedItemType.SKILLS,
                    skillName,
                    ctx.charState,
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            val skillDef = SkillDefinitionProxy.getByIdOrName(skillName)
            val skillId = skillDef?.id ?: return SourceRuleResult(skip = true)
            val owned = ctx.base.skillManager?.state?.value?.skills
                ?.firstOrNull { it.id == skillId || it.name.equals(skillName, ignoreCase = true) }
            val maxRemaining = owned?.let { BreakfastBurnSkills.maximumCastRemaining(it) } ?: 0L
            val usesRemaining =
                if (maxRemaining >= Int.MAX_VALUE.toLong()) Int.MAX_VALUE else maxRemaining.toInt()
            var cmd = ctx.source
            var text = ctx.source
            val hasSkill = owned != null
            if (!hasSkill || usesRemaining == 0) {
                if (ctx.includeAll) {
                    val isBuff = SkillDefinitionProxy.isBuff(skillId)
                    text = "(learn to $cmd${if (isBuff) ", or get it from a buffbot" else ""})"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            if (ctx.source.contains(" ^ ")) {
                val required = SkillRequiredItemForEffect.requiredItem(skillId, ctx.effectId)
                if (required != -1 && !equippedOrInInventory(ctx, required)) {
                    return SourceRuleResult(skip = true)
                }
            }
            val extraCosts = MaximizerBoostCostSuffix.BoostCosts(
                adv = SkillCastCosts.adventureCost(skillId),
                soulsauce = SkillCastCosts.soulsauceCost(skillId),
                thunder = SkillCastCosts.thunderCost(skillId),
                rain = SkillCastCosts.rainCost(skillId),
                lightning = SkillCastCosts.lightningCost(skillId),
                hp = SkillCastCosts.hpCost(skillId),
            )
            val skillState = ctx.base.skillManager?.state?.value
            val duration = if (skillState != null) {
                SkillDefinitionProxy.getEffectDuration(
                    skillId = skillId,
                    skillState = skillState,
                    charState = ctx.charState,
                    effectState = EffectState(ctx.base.activeEffects),
                    accessibleCount = ctx.base.inventoryCount,
                    gameDatabase = ctx.base.gameDatabase,
                )
            } else {
                skillDef.duration
            }
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = duration,
                usesRemaining = usesRemaining,
                extraCosts = extraCosts,
            )
        }

        private fun parseCastSkillName(source: String): String? {
            var rest = source.removePrefix("cast ").trim()
            if (rest.startsWith("1 ")) rest = rest.removePrefix("1 ")
            if (rest.contains(" ^ ")) rest = rest.substringBefore(" ^ ")
            return rest.takeIf { it.isNotBlank() }
        }
    }

    private object CargoEffectRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("cargo effect ", ignoreCase = true)) return null
            if (!ctx.charState.inLegacyOfLoathing &&
                !StandardRequest.isAllowed(
                    RestrictedItemType.ITEMS,
                    "Cargo Cultist Shorts",
                    ctx.charState,
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            val hasShorts = ctx.base.inventoryCount(BreakfastItemIds.CARGO_CULTIST_SHORTS_ID) > 0 ||
                (ctx.charState.inLegacyOfLoathing &&
                    ctx.base.inventoryCount(BreakfastItemIds.REPLICA_CARGO_CULTIST_SHORTS_ID) > 0)
            var cmd = ctx.source
            var text = ctx.source
            if (!hasShorts) {
                if (ctx.includeAll) {
                    text = "(acquire a pair of Cargo Cultist Shorts for ${ctx.effectName})"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            val pocketEmptied = ctx.preferences?.getBoolean(Preferences.CARGO_POCKET_EMPTIED, false) == true
            var duration = 0
            if (pocketEmptied) {
                cmd = ""
            } else if (hasShorts) {
                val pockets = PocketDatabase.effectPockets[ctx.effectName]
                val sorted = pockets?.let { PocketDatabase.sortResults(ctx.effectName, it) }
                val picked = pickedCargoPocketIds(ctx.preferences)
                val pocket = PocketDatabase.firstUnpickedPocket(sorted, picked)
                if (pocket == null) {
                    cmd = ""
                } else {
                    duration = (pocket as PocketDatabase.OneResultPocket).count(ctx.effectName)
                }
            }
            val usesRemaining = if (pocketEmptied) 0 else 1
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = duration,
                usesRemaining = usesRemaining,
            )
        }
    }

    private object BarrelPrayerRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("barrelprayer", ignoreCase = true)) return null
            if (isBadMoon(ctx.charState)) return SourceRuleResult(skip = true)
            if (!StandardRequest.isAllowed(
                    RestrictedItemType.ITEMS,
                    "shrine to the Barrel god",
                    ctx.charState,
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            if (LimitModeGates.limitZone("Dungeon Full of Dungeons", ctx.charState.limitMode)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            var text = ctx.source
            val unlocked = ctx.preferences?.getBoolean("barrelShrineUnlocked", false) == true
            if (!unlocked) {
                if (ctx.includeAll) {
                    text = "( install shrine to the Barrel god )"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            val used = ctx.preferences?.getBoolean("_barrelPrayer", false) == true
            if (used) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 50,
                usesRemaining = if (used) 0 else 1,
            )
        }
    }

    private object PhotoBoothEffectRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("photobooth effect ", ignoreCase = true)) return null
            if (isBadMoon(ctx.charState)) return SourceRuleResult(skip = true)
            if (!StandardRequest.isAllowed(
                    RestrictedItemType.CLAN_ITEMS,
                    "Photo Booth",
                    ctx.charState,
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            if (LimitModeGates.limitClan(ctx.charState.limitMode)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            var text = ctx.source
            if (!hasVipKey(ctx)) {
                if (ctx.includeAll) {
                    text = "( get access to the VIP lounge )"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            val used = ctx.preferences?.getInt("_photoBoothEffects", 0) ?: 0
            if (used >= 3) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 50,
                usesRemaining = 3 - used,
            )
        }
    }

    private object AlliedRadioEffectRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("alliedradio effect ", ignoreCase = true)) return null
            if (!StandardRequest.isAllowed(
                    RestrictedItemType.ITEMS,
                    "Allied Radio Backpack",
                    ctx.charState,
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            var text = ctx.source
            val hasBackpack = hasAlliedRadioBackpack(ctx)
            val backpackUses = alliedRadioBackpackUsesRemaining(ctx)
            val needsHandheld = !hasBackpack || backpackUses <= 0
            if (needsHandheld &&
                ctx.base.inventoryCount(BreakfastItemIds.HANDHELD_ALLIED_RADIO_ID) == 0
            ) {
                if (ctx.includeAll) {
                    text = "( acquire a handheld Allied Radio or charge your backpack for ${ctx.effectName} )"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            var duration = 0
            when (ctx.effectName.lowercase()) {
                "wildsun boon" -> {
                    if (ctx.preferences?.getBoolean(Preferences.ALLIED_RADIO_WILDSUN_BOON, false) == true) {
                        cmd = ""
                    }
                    duration = 100
                }
                "ellipsoidtined" -> duration = 30
                "materiel intel" -> {
                    if (ctx.preferences?.getBoolean(Preferences.ALLIED_RADIO_MATERIEL_INTEL, false) == true) {
                        cmd = ""
                    }
                    duration = 10
                }
            }
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = duration,
                usesRemaining = alliedRadioUsesRemaining(ctx),
            )
        }
    }

    /** Desktop Maximizer hot-dog branch when eat target is a clan VIP hot dog. */
    private object HotDogEatRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("eat ", ignoreCase = true)) return null
            val dogName = parseConsumeTarget(ctx.source) ?: return null
            if (!HotDogDatabase.isHotDog(dogName)) return null
            if (isBadMoon(ctx.charState)) return SourceRuleResult(skip = true)
            if (!ClanLoungeSync.isHotDogStandAllowed(ctx.charState)) {
                return SourceRuleResult(skip = true)
            }
            if (ctx.charState.ascensionPath == AscensionPath.AVATAR_OF_JARLSBERG ||
                ctx.charState.inZombiecore
            ) {
                return SourceRuleResult(skip = true)
            }
            if (LimitModeGates.limitClan(ctx.charState.limitMode)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            var text = ctx.source
            if (!hasVipKey(ctx)) {
                if (ctx.includeAll) {
                    text = "( get access to the VIP lounge )"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            val fullCost = HotDogDatabase.nameToFullness(dogName)
            if (fullCost > 0 &&
                ctx.charState.fullness + fullCost > ctx.charState.fullnessLimit
            ) {
                return SourceRuleResult(skip = true)
            }
            if (HotDogDatabase.isFancyHotDog(dogName) &&
                ctx.preferences?.getBoolean(ClanLoungeSync.FANCY_HOT_DOG_EATEN_PREF, false) == true
            ) {
                return SourceRuleResult(skip = true)
            }
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                usesRemaining = 1,
                extraCosts = MaximizerBoostCostSuffix.BoostCosts(full = fullCost),
            )
        }
    }

    /** Desktop clan speakeasy drink sources (`drink 1 Lucky Lindy`, etc.). */
    private object SpeakeasyDrinkRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("drink ", ignoreCase = true)) return null
            val drinkName = parseConsumeTarget(ctx.source) ?: return null
            if (!SpeakeasyDatabase.isSpeakeasyDrink(drinkName)) return null
            if (isBadMoon(ctx.charState)) return SourceRuleResult(skip = true)
            if (!ClanLoungeSync.isSpeakeasyAllowed(ctx.charState)) {
                return SourceRuleResult(skip = true)
            }
            if (LimitModeGates.limitClan(ctx.charState.limitMode)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            var text = ctx.source
            if (!hasVipKey(ctx)) {
                if (ctx.includeAll) {
                    text = "( get access to the VIP lounge )"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            val drunkCost = SpeakeasyDatabase.nameToInebriety(drinkName)
            if (drunkCost > 0 &&
                ctx.charState.inebriety + drunkCost > ctx.charState.inebrietyLimit
            ) {
                return SourceRuleResult(skip = true)
            }
            val drinksDrunk = ctx.preferences?.getInt(ClanLoungeSync.SPEAKEASY_DRINKS_DRUNK_PREF, 0) ?: 0
            if (drinksDrunk >= 3) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                usesRemaining = 3 - drinksDrunk,
                extraCosts = MaximizerBoostCostSuffix.BoostCosts(drunk = drunkCost),
            )
        }
    }

    /** Desktop generic use/eat/drink/chew item sources (~593–723). */
    private object ConsumptionSourceRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("use ", ignoreCase = true) &&
                !ctx.source.startsWith("eat ", ignoreCase = true) &&
                !ctx.source.startsWith("drink ", ignoreCase = true) &&
                !ctx.source.startsWith("chew ", ignoreCase = true)
            ) {
                return null
            }
            val canInteract = StoragePullRules.canInteract(ctx.charState)

            if (MaximizerConsumptionGates.isTriviaMasterUse(ctx.source) &&
                !MaximizerConsumptionGates.canMasterTrivia(ctx.base.inventoryCount, canInteract)
            ) {
                return SourceRuleResult(skip = true)
            }
            if (!canInteract &&
                ctx.source.startsWith("use 1 box of sunshine", ignoreCase = true)
            ) {
                return SourceRuleResult(skip = true)
            }

            val itemName = parseConsumeTarget(ctx.source) ?: return SourceRuleResult(skip = true)
            var itemData = ctx.base.gameDatabase.item(itemName)
            if (itemData == null) {
                val resolvedId = BangPotionResolver.resolveItemId(itemName, ctx.preferences)
                if (resolvedId != null) {
                    itemData = ctx.base.gameDatabase.item(resolvedId)
                }
            }
            if (itemData == null) {
                if (ctx.source.contains(',')) return SourceRuleResult(skip = true)
                return if (ctx.includeAll) {
                    SourceRuleResult(
                        cmd = "",
                        text = "(identify & ${ctx.source})",
                    )
                } else {
                    SourceRuleResult(skip = true)
                }
            }

            val itemId = itemData.id
            if (MaximizerConsumptionGates.excludedTCRSItem(itemId)) {
                return SourceRuleResult(skip = true)
            }
            if (MaximizerConsumptionGates.blockedInGLover(
                    itemId,
                    itemData.name,
                    ctx.charState.inGLover,
                )
            ) {
                return SourceRuleResult(skip = true)
            }

            var duration = 0
            if (itemId == ItemDatabase.VAMPIRE_VINTNER_WINE) {
                if (!MaximizerConsumptionGates.vintnerWineAllowed(
                        ctx.effectName,
                        ctx.base.inventoryCount,
                        ctx.preferences,
                    )
                ) {
                    return SourceRuleResult(skip = true)
                }
                duration = 12
            } else {
                duration = MaximizerConsumptionGates.itemEffectDuration(itemData.name, ctx.effectName)
            }

            val limitsCtx = ItemUseLimitsContext(
                character = ctx.charState,
                preferences = ctx.preferences,
                expressionContext = ExpressionContext.from(
                    ctx.charState,
                    ctx.base.activeEffects,
                    emptySet(),
                ),
                accessibleCount = ctx.base.inventoryCount,
            )
            val usesRemaining = maximumUses(itemId, itemData.name, limitsCtx)
            if (usesRemaining <= 0) {
                return SourceRuleResult(skip = true)
            }

            var cmd = ctx.source
            var text = ctx.source
            if (ctx.hasEffect && !ctx.source.contains(ctx.effectName, ignoreCase = true)) {
                text = "$text (to remove ${ctx.effectName})"
            }

            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = duration,
                usesRemaining = usesRemaining,
            )
        }
    }

    private object FriarsRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("friars ", ignoreCase = true)) return null
            val lastCeremony = ctx.preferences?.getInt("lastFriarCeremonyAscension", 0) ?: 0
            val knownAscensions = ctx.preferences?.getInt("knownAscensions", 0) ?: 0
            if (lastCeremony < knownAscensions ||
                LimitModeGates.limitZone("Friars", ctx.charState.limitMode)
            ) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            val received = ctx.preferences?.getBoolean("friarsBlessingReceived", false) == true
            if (received) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = ctx.source,
                duration = 20,
                usesRemaining = if (received) 0 else 1,
            )
        }
    }

    private object PillkeeperRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("pillkeeper", ignoreCase = true)) return null
            if (!StandardRequest.isAllowed(
                    RestrictedItemType.ITEMS,
                    "Eight Days a Week Pill Keeper",
                    ctx.charState,
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            var text = ctx.source
            if (ctx.base.inventoryCount(PILL_KEEPER_ID) == 0) {
                if (ctx.includeAll) {
                    text = "(get an Eight Days a Week Pill Keeper)"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            val freeUsed = ctx.preferences?.getBoolean("_freePillKeeperUsed", false) == true
            var usesRemaining = 1
            var extraCosts: MaximizerBoostCostSuffix.BoostCosts? = null
            if (freeUsed) {
                usesRemaining = ctx.charState.spleenRemaining
                if (usesRemaining < 3) cmd = ""
                extraCosts = MaximizerBoostCostSuffix.BoostCosts(spleen = 3)
            }
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 30,
                usesRemaining = usesRemaining,
                extraCosts = extraCosts,
            )
        }
    }

    private object PoolRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("pool ", ignoreCase = true)) return null
            return vipClanFacilityWithCount(
                ctx = ctx,
                restriction = "Pool Table",
                prefCount = ctx.preferences?.getInt("_poolGames", 0) ?: 0,
                maxUses = 3,
                duration = 10,
            )
        }
    }

    private object ShowerRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("shower ", ignoreCase = true)) return null
            return vipClanDailyFacility(
                ctx = ctx,
                restriction = "April Shower",
                used = ctx.preferences?.getBoolean("_aprilShower", false) == true,
                duration = 50,
            )
        }
    }

    private object SwimRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("swim ", ignoreCase = true)) return null
            return vipClanDailyFacility(
                ctx = ctx,
                restriction = "Clan Swimming Pool",
                used = ctx.preferences?.getBoolean("_olympicSwimmingPool", false) == true,
                duration = 50,
            )
        }
    }

    private object JukeboxRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("jukebox", ignoreCase = true)) return null
            if (!canInteract(ctx.charState)) return SourceRuleResult(skip = true)
            if (LimitModeGates.limitClan(ctx.charState.limitMode)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            val used = ctx.preferences?.getBoolean("_jukebox", false) == true
            if (used) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = ctx.source,
                duration = 10,
                usesRemaining = if (used) 0 else 1,
            )
        }
    }

    private object BallpitRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("ballpit", ignoreCase = true)) return null
            if (!canInteract(ctx.charState)) return SourceRuleResult(skip = true)
            if (LimitModeGates.limitClan(ctx.charState.limitMode)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            val used = ctx.preferences?.getBoolean("_ballpit", false) == true
            if (used) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = ctx.source,
                duration = 20,
                usesRemaining = if (used) 0 else 1,
            )
        }
    }

    private object TelescopeRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("telescope ", ignoreCase = true)) return null
            if (isBadMoon(ctx.charState)) return SourceRuleResult(skip = true)
            var cmd = ctx.source
            var text = ctx.source
            if (telescopeUpgrades(ctx) == 0) {
                if (ctx.includeAll) {
                    text = "( get a telescope )"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            val lookedHigh = telescopeLookedHigh(ctx)
            if (lookedHigh) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 10,
                usesRemaining = if (lookedHigh) 0 else 1,
            )
        }
    }

    private object FortuneRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("fortune ", ignoreCase = true)) return null
            return vipClanDailyFacility(
                ctx = ctx,
                restriction = "Clan Love Tester",
                used = ctx.preferences?.getBoolean("_clanFortuneBuffUsed", false) == true,
                duration = 100,
            )
        }
    }

    private object MomRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("mom ", ignoreCase = true)) return null
            if (!isQuestFinished(ctx.preferences, Quest.SEA_MONKEES)) {
                return SourceRuleResult(skip = true)
            }
            if (LimitModeGates.limitZone("The Sea", ctx.charState.limitMode)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            val received = ctx.preferences?.getBoolean("_momFoodReceived", false) == true
            if (received) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = ctx.source,
                duration = 50,
                usesRemaining = if (received) 0 else 1,
            )
        }
    }

    private object ConcertRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("concert ", ignoreCase = true)) return null
            val side = ctx.preferences?.getString("sidequestArenaCompleted", "none") ?: "none"
            if (side == "none") return SourceRuleResult(skip = true)
            if (LimitModeGates.limitZone("Island", ctx.charState.limitMode) ||
                LimitModeGates.limitZone("IsleWar", ctx.charState.limitMode)
            ) {
                return SourceRuleResult(skip = true)
            }
            if (!concertSongAvailable(ctx.source, side)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            val visited = ctx.preferences?.getBoolean("concertVisited", false) == true
            if (visited) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = ctx.source,
                duration = 20,
                usesRemaining = if (visited) 0 else 1,
            )
        }
    }

    private object SummonRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("summon ", ignoreCase = true)) return null
            if (!isQuestFinished(ctx.preferences, Quest.MANOR)) {
                return SourceRuleResult(skip = true)
            }
            val scrolls = ctx.base.inventoryCount(DemonTypes.EVIL_SCROLL)
            val candles = ctx.base.inventoryCount(DemonTypes.BLACK_CANDLE)
            if (!canInteract(ctx.charState) && (scrolls < 1 || candles < 3)) {
                return SourceRuleResult(skip = true)
            }
            if (LimitModeGates.limitZone("Manor0", ctx.charState.limitMode)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            var text = ctx.source
            val summoned = ctx.preferences?.getBoolean(Preferences.DEMON_SUMMONED, false) == true
            if (summoned) {
                cmd = ""
            } else {
                val demonNumber = parseSummonDemonNumber(ctx.source)
                if (demonNumber != null) {
                    val demonName = ctx.preferences?.getString(DemonTypes.demonNameKey(demonNumber), "")
                        .orEmpty()
                    if (demonName.isEmpty()) cmd = ""
                }
            }
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 30,
                usesRemaining = if (summoned) 0 else 1,
            )
        }
    }

    private object MayosoakRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("mayosoak", ignoreCase = true)) return null
            if (isBadMoon(ctx.charState)) return SourceRuleResult(skip = true)
            if (!StandardRequest.isAllowed(
                    RestrictedItemType.ITEMS,
                    "portable Mayo Clinic",
                    ctx.charState,
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            if (LimitModeGates.limitCampground(ctx.charState.limitMode)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            var text = ctx.source
            if (!CampgroundItemSync.hasWorkshedItem(ctx.preferences, ConcoctionMayoQueue.MAYO_CLINIC)) {
                if (ctx.includeAll) {
                    text = "( install portable Mayo Clinic )"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            val soaked = ctx.preferences?.getBoolean("_mayoTankSoaked", false) == true
            if (soaked) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 20,
                usesRemaining = if (soaked) 0 else 1,
            )
        }
    }

    private object WitchessRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.equals("witchess", ignoreCase = true)) return null
            val loeReplica = ctx.charState.inLegacyOfLoathing &&
                ctx.preferences?.getBoolean("replicaWitchessSetAvailable", false) == true
            if (!loeReplica &&
                !StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Witchess Set", ctx.charState)
            ) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            var text = ctx.source
            if (!CampgroundItemSync.hasWitchessSet(ctx.preferences)) {
                if (ctx.includeAll) {
                    text = "(install Witchess Set for ${ctx.effectName})"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            } else if (ctx.preferences?.getBoolean("_witchessBuff", false) == true) {
                cmd = ""
            } else if (ctx.preferences?.getInt("puzzleChampBonus", 5) != 20) {
                text = "(manually get ${ctx.effectName})"
                cmd = ""
            }
            val used = ctx.preferences?.getBoolean("_witchessBuff", false) == true
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 25,
                usesRemaining = if (used) 0 else 1,
            )
        }
    }

    private object MonorailRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("monorail ", ignoreCase = true)) return null
            var cmd = ctx.source
            val favored = ctx.preferences?.getBoolean("_lyleFavored", false) == true
            if (favored) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = ctx.source,
                duration = 10,
                usesRemaining = if (favored) 0 else 1,
            )
        }
    }

    private object ToggleRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("toggle", ignoreCase = true)) return null
            val hasInterest = ctx.base.activeEffects.any {
                it.id == EFFECT_SUPERFICIALLY_INTERESTED || it.id == EFFECT_INTENSELY_INTERESTED
            }
            if (!hasInterest) return SourceRuleResult(skip = true)
            return SourceRuleResult(
                cmd = ctx.source,
                text = ctx.source,
            )
        }
    }

    private object CrossstreamsRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.equals("crossstreams", ignoreCase = true)) return null
            var cmd = ctx.source
            var text = ctx.source
            if (ctx.base.inventoryCount(PROTON_PACK_ID) == 0) {
                if (ctx.includeAll) {
                    text = "(acquire protonic accelerator pack and crossstreams for ${ctx.effectName})"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            val crossed = ctx.preferences?.getBoolean("_streamsCrossed", false) == true
            if (crossed) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 10,
                usesRemaining = if (crossed) 0 else 1,
            )
        }
    }

    private object MonkeyPawEffectRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("monkeypaw effect ", ignoreCase = true)) return null
            if (!StandardRequest.isAllowed(
                    RestrictedItemType.ITEMS,
                    "cursed monkey's paw",
                    ctx.charState,
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            var text = ctx.source
            if (!hasEquippedOrInInventory(ctx, MONKEY_PAW_ID)) {
                if (ctx.includeAll) {
                    text = "( acquire a cursed monkey's paw )"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            val used = ctx.preferences?.getInt("_monkeyPawWishesUsed", 0) ?: 0
            if (used >= 5) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 30,
                usesRemaining = 5 - used,
            )
        }
    }

    private object GenieEffectRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("genie effect ", ignoreCase = true)) return null
            if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "pocket wish", ctx.charState)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            var text = ctx.source
            val genieCount = ctx.base.inventoryCount(BreakfastItemIds.GENIE_BOTTLE_ID)
            val replicaCount = ctx.base.inventoryCount(BreakfastItemIds.REPLICA_GENIE_BOTTLE_ID)
            val wishesUsed = ctx.preferences?.getInt("_genieWishesUsed", 0) ?: 0
            val pocketWishes = ctx.base.inventoryCount(POCKET_WISH_ID)
            val bottleWishesRemaining =
                if (genieCount > 0 || replicaCount > 0) maxOf(0, 3 - wishesUsed) else 0
            val usesRemaining = bottleWishesRemaining + pocketWishes
            if (usesRemaining <= 0) {
                if (ctx.includeAll) {
                    text = "( acquire a genie bottle or pocket wish for ${ctx.effectName} )"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 20,
                usesRemaining = usesRemaining,
            )
        }
    }

    private object GongRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("gong ", ignoreCase = true)) return null
            return SourceRuleResult(
                cmd = ctx.source,
                text = ctx.source,
                duration = 20,
                extraCosts = MaximizerBoostCostSuffix.BoostCosts(adv = 3),
            )
        }
    }

    private object StyxRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("styx ", ignoreCase = true)) return null
            if (!isBadMoon(ctx.charState)) return SourceRuleResult(skip = true)
            if (LimitModeGates.limitZone("BadMoon", ctx.charState.limitMode)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            val visited = ctx.preferences?.getBoolean("styxPixieVisited", false) == true
            if (visited) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = ctx.source,
                duration = 10,
                usesRemaining = if (visited) 0 else 1,
            )
        }
    }

    private object PlayRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("play", ignoreCase = true)) return null
            val hasDeck = ctx.base.inventoryCount(DECK_OF_EVERY_CARD_ID) > 0
            val hasReplica = ctx.base.inventoryCount(REPLICA_DECK_ID) > 0 &&
                ctx.charState.inLegacyOfLoathing
            var cmd = ctx.source
            var text = ctx.source
            if (!hasDeck && !hasReplica) {
                if (ctx.includeAll) {
                    text = "(acquire Deck of Every Card for ${ctx.effectName})"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            val cardsDrawn = ctx.preferences?.getInt("_deckCardsDrawn", 0) ?: 0
            if (cardsDrawn > 10) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 20,
                usesRemaining = (15 - cardsDrawn) / 5,
            )
        }
    }

    private object SkeletonRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("skeleton ", ignoreCase = true)) return null
            return SourceRuleResult(
                cmd = ctx.source,
                text = ctx.source,
                duration = 30,
            )
        }
    }

    private object GapRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("gap ", ignoreCase = true)) return null
            val hasGap = ctx.base.inventoryCount(GREAT_PANTS_ID) > 0
            val hasReplica = ctx.base.inventoryCount(REPLICA_GREAT_PANTS_ID) > 0 &&
                ctx.charState.inLegacyOfLoathing
            var cmd = ctx.source
            var text = ctx.source
            if (!hasGap && !hasReplica) {
                if (ctx.includeAll) {
                    text = "(acquire and equip Greatest American Pants for ${ctx.effectName})"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            val gapBuffs = ctx.preferences?.getInt("_gapBuffs", 0) ?: 0
            if (gapBuffs >= 5) {
                cmd = ""
            } else if (!hasGapPantsEquipped(ctx)) {
                text = "(equip Greatest American Pants for ${ctx.effectName})"
                cmd = ""
            }
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = gapDuration(ctx.effectName),
                usesRemaining = 5 - gapBuffs,
            )
        }
    }

    private object SpacegateRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("spacegate", ignoreCase = true)) return null
            if (!StandardRequest.isAllowed(
                    RestrictedItemType.ITEMS,
                    "Spacegate access badge",
                    ctx.charState,
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            if (ctx.charState.isKingdomOfExploathing) return SourceRuleResult(skip = true)
            var cmd = ctx.source
            var text = ctx.source
            val available = ctx.preferences?.getBoolean("spacegateAlways", false) == true ||
                ctx.preferences?.getBoolean("_spacegateToday", false) == true
            val vaccineNumber = ctx.source.lastOrNull()?.takeIf { it.isDigit() }?.toString().orEmpty()
            val vaccineAvailable = vaccineNumber.isNotEmpty() &&
                ctx.preferences?.getBoolean("spacegateVaccine$vaccineNumber", false) == true
            if (!available || !vaccineAvailable) {
                if (ctx.includeAll) {
                    text = "(unlock Spacegate and vaccine $vaccineNumber for ${ctx.effectName})"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            } else if (ctx.preferences?.getBoolean("_spacegateVaccine", false) == true) {
                cmd = ""
            }
            val vaccineUsed = ctx.preferences?.getBoolean("_spacegateVaccine", false) == true
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 30,
                usesRemaining = if (vaccineUsed) 0 else 1,
            )
        }
    }

    private object DaycareRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("daycare", ignoreCase = true)) return null
            if (!StandardRequest.isAllowed(
                    RestrictedItemType.ITEMS,
                    "Boxing Day care package",
                    ctx.charState,
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            var text = ctx.source
            val available = ctx.preferences?.getBoolean("daycareOpen", false) == true ||
                ctx.preferences?.getBoolean("_daycareToday", false) == true
            if (!available) {
                if (ctx.includeAll) {
                    text = "(unlock Boxing Daycare and visit spa for ${ctx.effectName})"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            } else if (ctx.preferences?.getBoolean("_daycareSpa", false) == true) {
                cmd = ""
            }
            val spaUsed = ctx.preferences?.getBoolean("_daycareSpa", false) == true
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 100,
                usesRemaining = if (spaUsed) 0 else 1,
            )
        }
    }

    private object Vault3Rule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("campground vault3", ignoreCase = true)) return null
            if (!ctx.charState.inNuclearAutumn) return SourceRuleResult(skip = true)
            if ((ctx.preferences?.getInt("falloutShelterLevel", 0) ?: 0) < 3) {
                return SourceRuleResult(skip = true)
            }
            if (LimitModeGates.limitCampground(ctx.charState.limitMode)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            val spaUsed = ctx.preferences?.getBoolean("_falloutShelterSpaUsed", false) == true
            if (spaUsed) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = ctx.source,
                duration = 100,
                usesRemaining = if (spaUsed) 0 else 1,
            )
        }
    }

    private object GrimRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("grim", ignoreCase = true)) return null
            if (!StandardRequest.isAllowed(
                    RestrictedItemType.FAMILIARS,
                    "Grim Brother",
                    ctx.charState,
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            var text = ctx.source
            if (!ownsGrimBrother(ctx)) {
                if (LimitModeGates.limitFamiliars(ctx.charState.limitMode)) {
                    return SourceRuleResult(skip = true)
                }
                if (ctx.includeAll) {
                    text = "(get a Grim Brother familiar for ${ctx.effectName})"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            } else if (ctx.preferences?.getBoolean("_grimBuff", false) == true) {
                cmd = ""
            }
            val grimUsed = ctx.preferences?.getBoolean("_grimBuff", false) == true
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 30,
                usesRemaining = if (grimUsed) 0 else 1,
            )
        }
    }

    private object AprilingBandRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("aprilband ", ignoreCase = true)) return null
            return SourceRuleResult(
                cmd = ctx.source,
                text = ctx.source,
            )
        }
    }

    private object TerminalEnhanceRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("terminal enhance", ignoreCase = true)) return null
            var cmd = ctx.source
            var text = ctx.source
            val chips = ctx.preferences?.getString("sourceTerminalChips", "").orEmpty()
            val files = ctx.preferences?.getString("sourceTerminalEnhanceKnown", "").orEmpty()
            var limit = 1
            if (chips.contains("CRAM")) limit++
            if (chips.contains("SCRAM")) limit++
            val haveTerminal = CampgroundItemSync.hasSourceTerminal(ctx.preferences) ||
                (ctx.charState.inNuclearAutumn && ctx.base.inventoryCount(SOURCE_TERMINAL_ID) > 0)
            if (!haveTerminal) {
                if (ctx.includeAll) {
                    text = "(install Source Terminal for ${ctx.effectName})"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            } else if (cmd.contains(ctx.effectName) && !files.contains(ctx.effectName)) {
                if (ctx.includeAll) {
                    text = "(install Source terminal file: ${ctx.effectName} for ${ctx.effectName})"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            } else {
                val uses = ctx.preferences?.getInt("_sourceTerminalEnhanceUses", 0) ?: 0
                if (uses >= limit) cmd = ""
            }
            val uses = ctx.preferences?.getInt("_sourceTerminalEnhanceUses", 0) ?: 0
            val pram = ctx.preferences?.getInt("sourceTerminalPram", 0) ?: 0
            val duration = 25 + (if (chips.contains("INGRAM")) 25 else 0) + 5 * pram
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = duration,
                usesRemaining = limit - uses,
            )
        }
    }

    private object CampAwayCloudRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.equals("campaway cloud", ignoreCase = true)) return null
            if (!CampAwayAvailability.campAwayTentAvailable(ctx.charState, ctx.preferences)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            val used = ctx.preferences?.getInt("_campAwayCloudBuffs", 0) ?: 0
            if (used > 0) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = ctx.source,
                duration = 100,
                usesRemaining = 1 - used,
            )
        }
    }

    private object LoathingIdolRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("loathingidol ", ignoreCase = true)) return null
            return SourceRuleResult(
                cmd = ctx.source,
                text = ctx.source,
                duration = 30,
            )
        }
    }

    private object MayamRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("mayam ", ignoreCase = true)) return null
            var cmd = ctx.source
            if (cmd.startsWith("mayam resonance ", ignoreCase = true)) {
                val resonanceName = cmd.substring(16).lowercase()
                if (!MayamAvailability.availableResonances(ctx.preferences)
                        .contains(resonanceName)
                ) {
                    cmd = ""
                }
            }
            return SourceRuleResult(
                cmd = cmd,
                text = ctx.source,
            )
        }
    }

    private object AsdonMartinDriveRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("asdonmartin drive", ignoreCase = true)) return null
            var cmd = ctx.source
            var text = ctx.source
            if (!CampgroundItemSync.hasWorkshedItem(ctx.preferences, ASDON_MARTIN_ID)) {
                if (ctx.includeAll) {
                    text = "(install Asdon Martin for ${ctx.effectName})"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            } else {
                val fuel = CampgroundItemSync.asdonMartinFuel(ctx.preferences)
                if (fuel < 37) cmd = ""
            }
            val fuel = CampgroundItemSync.asdonMartinFuel(ctx.preferences)
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 30,
                usesRemaining = fuel / 37,
                extraCosts = MaximizerBoostCostSuffix.BoostCosts(fuel = 37),
            )
        }
    }

    private object BeachHeadRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("beach head ", ignoreCase = true)) return null
            if (!StandardRequest.isAllowed(
                    RestrictedItemType.ITEMS,
                    "Beach Comb",
                    ctx.charState,
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            val hasComb = ctx.base.inventoryCount(BeachHeadAvailability.BEACH_COMB_ID) > 0 ||
                ctx.base.inventoryCount(BeachHeadAvailability.DRIFTWOOD_BEACH_COMB_ID) > 0
            var cmd = ctx.source
            var text = ctx.source
            if (!hasComb) {
                if (ctx.includeAll) {
                    text = "(acquire a Beach Comb or a driftwood beach comb for ${ctx.effectName})"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            val headAvailable = BeachHeadAvailability.headAvailable(ctx.effectName, ctx.preferences)
            if (!headAvailable) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 50,
                usesRemaining = if (headAvailable) 1 else 0,
            )
        }
    }

    private object SkateRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("skate ", ignoreCase = true)) return null
            if (LimitModeGates.limitZone("The Sea", ctx.charState.limitMode)) {
                return SourceRuleResult(skip = true)
            }
            val buffIndex = SkateParkAvailability.placeToBuff(ctx.source.substring(6))
                ?: return SourceRuleResult(skip = true)
            val data = SkateParkAvailability.buffToData(buffIndex)
                ?: return SourceRuleResult(skip = true)
            val status = ctx.preferences?.getString("skateParkStatus", "war") ?: "war"
            if (status != data.state) return SourceRuleResult(skip = true)
            var cmd = ctx.source
            val used = SkateParkAvailability.buffPrefUsed(ctx.preferences, buffIndex)
            if (used) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = ctx.source,
                duration = 30,
                usesRemaining = if (used) 0 else 1,
            )
        }
    }

    private object HatterRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("hatter ", ignoreCase = true)) return null
            val havePotion = ctx.base.inventoryCount(RabbitHoleAvailability.DRINK_ME_POTION_ID) > 0
            if (!havePotion && !ctx.hasEffect) return SourceRuleResult(skip = true)
            val desiredLength = ctx.source.substring(7).trim().toIntOrNull()
                ?: return SourceRuleResult(skip = true)
            val equippedHat = ctx.charState.equippedItem(EquipmentSlot.HAT).orEmpty()
            if (!RabbitHoleAvailability.hatLengthAvailable(
                    desiredLength,
                    ctx.base.inventoryCount,
                    equippedHat.takeIf { it.isNotBlank() },
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            if (LimitModeGates.limitZone("Rabbit Hole", ctx.charState.limitMode)) {
                return SourceRuleResult(skip = true)
            }
            var cmd = ctx.source
            val teaPartyUsed = !RabbitHoleAvailability.teaPartyAvailable(ctx.preferences)
            if (teaPartyUsed) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = ctx.source,
                duration = 30,
                usesRemaining = if (teaPartyUsed) 0 else 1,
            )
        }
    }

    private object SynthesizeRule : SourceRule {
        override fun apply(ctx: SourceRuleContext): SourceRuleResult? {
            if (!ctx.source.startsWith("synthesize ", ignoreCase = true)) return null
            if (ctx.charState.inGLover) return SourceRuleResult(skip = true)
            if (!StandardRequest.isAllowed(
                    RestrictedItemType.SKILLS,
                    "Sweet Synthesis",
                    ctx.charState,
                )
            ) {
                return SourceRuleResult(skip = true)
            }
            val owned = ctx.base.skillManager?.state?.value?.skills
                ?.any {
                    it.id == SWEET_SYNTHESIS_ID ||
                        it.name.equals("Sweet Synthesis", ignoreCase = true)
                } ?: false
            var cmd = ctx.source
            var text = ctx.source
            if (!owned) {
                if (ctx.includeAll) {
                    text = "(learn the Sweet Synthesis skill)"
                    cmd = ""
                } else {
                    return SourceRuleResult(skip = true)
                }
            }
            CandyDatabase.loadBlacklist(ctx.preferences)
            val spleenRemaining = ctx.charState.spleenRemaining
            if (spleenRemaining < 1) cmd = ""
            if (!CandyDatabase.synthesisPair(ctx.effectId, ctx.base.inventoryCount)) cmd = ""
            return SourceRuleResult(
                cmd = cmd,
                text = text,
                duration = 30,
                usesRemaining = spleenRemaining,
                extraCosts = MaximizerBoostCostSuffix.BoostCosts(spleen = 1),
            )
        }
    }

    private fun vipClanDailyFacility(
        ctx: SourceRuleContext,
        restriction: String,
        used: Boolean,
        duration: Int,
    ): SourceRuleResult? {
        if (isBadMoon(ctx.charState)) return SourceRuleResult(skip = true)
        if (!StandardRequest.isAllowed(RestrictedItemType.CLAN_ITEMS, restriction, ctx.charState)) {
            return SourceRuleResult(skip = true)
        }
        if (LimitModeGates.limitClan(ctx.charState.limitMode)) {
            return SourceRuleResult(skip = true)
        }
        var cmd = ctx.source
        var text = ctx.source
        if (!hasVipKey(ctx)) {
            if (ctx.includeAll) {
                text = "( get access to the VIP lounge )"
                cmd = ""
            } else {
                return SourceRuleResult(skip = true)
            }
        }
        if (used) cmd = ""
        return SourceRuleResult(
            cmd = cmd,
            text = text,
            duration = duration,
            usesRemaining = if (used) 0 else 1,
        )
    }

    private fun vipClanFacilityWithCount(
        ctx: SourceRuleContext,
        restriction: String,
        prefCount: Int,
        maxUses: Int,
        duration: Int,
    ): SourceRuleResult? {
        if (isBadMoon(ctx.charState)) return SourceRuleResult(skip = true)
        if (!StandardRequest.isAllowed(RestrictedItemType.CLAN_ITEMS, restriction, ctx.charState)) {
            return SourceRuleResult(skip = true)
        }
        if (LimitModeGates.limitClan(ctx.charState.limitMode)) {
            return SourceRuleResult(skip = true)
        }
        var cmd = ctx.source
        var text = ctx.source
        if (!hasVipKey(ctx)) {
            if (ctx.includeAll) {
                text = "( get access to the VIP lounge )"
                cmd = ""
            } else {
                return SourceRuleResult(skip = true)
            }
        }
        if (prefCount >= maxUses) cmd = ""
        return SourceRuleResult(
            cmd = cmd,
            text = text,
            duration = duration,
            usesRemaining = maxUses - prefCount,
        )
    }

    private fun canInteract(state: CharacterState): Boolean =
        StoragePullRules.canInteract(state)

    private fun hasEquippedOrInInventory(ctx: SourceRuleContext, itemId: Int): Boolean {
        if (ctx.base.inventoryCount(itemId) > 0) return true
        val name = ctx.base.gameDatabase.item(itemId)?.name ?: return false
        return ctx.charState.equipment.values.any { it.equals(name, ignoreCase = true) }
    }

    private fun isQuestFinished(prefs: Preferences?, quest: Quest): Boolean =
        prefs?.getString(quest.prefKey, QuestDatabase.UNSTARTED) == QuestDatabase.FINISHED

    private fun telescopeUpgrades(ctx: SourceRuleContext): Int {
        if (ctx.charState.telescopeUpgrades > 0) return ctx.charState.telescopeUpgrades
        return ctx.preferences?.getInt("telescopeUpgrades", 0) ?: 0
    }

    private fun telescopeLookedHigh(ctx: SourceRuleContext): Boolean =
        ctx.charState.telescopeLookedHigh ||
            ctx.preferences?.getBoolean("telescopeLookedHigh", false) == true

    private fun concertSongAvailable(cmd: String, side: String): Boolean = when (side) {
        "fratboy" ->
            cmd.contains("Elvish", ignoreCase = true) ||
                cmd.contains("Winklered", ignoreCase = true) ||
                cmd.contains("White-boy Angst", ignoreCase = true)
        "hippy" ->
            cmd.contains("Moon", ignoreCase = true) ||
                cmd.contains("Dilated", ignoreCase = true) ||
                cmd.contains("Optimist", ignoreCase = true)
        else -> false
    }

    private fun parseSummonDemonNumber(source: String): Int? =
        source.removePrefix("summon ").trim().split(' ').firstOrNull()?.toIntOrNull()

    private fun parseConsumeTarget(source: String): String? {
        val rest = source.substringAfter(' ').trim()
        val qtyMatch = Regex("""^(\d+)\s+(.+)$""").matchEntire(rest) ?: return rest.takeIf { it.isNotBlank() }
        return qtyMatch.groupValues[2].takeIf { it.isNotBlank() }
    }

    private fun pickedCargoPocketIds(prefs: Preferences?): Set<Int> {
        val value = prefs?.getString(Preferences.CARGO_POCKETS_EMPTIED, "") ?: return emptySet()
        if (value.isEmpty()) return emptySet()
        return value.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    private fun hasVipKey(ctx: SourceRuleContext): Boolean =
        ctx.base.inventoryCount(BreakfastManager.VIP_LOUNGE_KEY_ID) > 0

    private fun hasAlliedRadioBackpack(ctx: SourceRuleContext): Boolean {
        if (ctx.base.inventoryCount(BreakfastItemIds.ALLIED_RADIO_BACKPACK_ID) > 0) return true
        val container = ctx.charState.equippedItem(EquipmentSlot.CONTAINER).orEmpty()
        return container.contains("Allied Radio Backpack", ignoreCase = true)
    }

    private fun alliedRadioBackpackUsesRemaining(ctx: SourceRuleContext): Int {
        if (!hasAlliedRadioBackpack(ctx)) return 0
        val used = ctx.preferences?.getInt(Preferences.ALLIED_RADIO_DROPS_USED, 0) ?: 0
        return (3 - used).coerceAtLeast(0)
    }

    private fun alliedRadioUsesRemaining(ctx: SourceRuleContext): Int {
        var uses = if (ctx.base.inventoryCount(BreakfastItemIds.HANDHELD_ALLIED_RADIO_ID) > 0) 1 else 0
        uses += alliedRadioBackpackUsesRemaining(ctx)
        return uses
    }

    private fun equippedOrInInventory(ctx: SourceRuleContext, itemId: Int): Boolean {
        if (ctx.base.inventoryCount(itemId) > 0) return true
        val itemName = ctx.base.gameDatabase.item(itemId)?.name ?: return false
        return ctx.charState.equipment.values.any { it.equals(itemName, ignoreCase = true) }
    }

    private fun isBadMoon(state: CharacterState): Boolean =
        state.zodiacSign.equals("Bad Moon", ignoreCase = true)

    private fun ownsGrimBrother(ctx: SourceRuleContext): Boolean =
        ctx.base.familiarManager?.state?.value?.ownedFamiliars
            ?.any { it.id == GRIM_BROTHER_ID } == true

    private fun hasGapPantsEquipped(ctx: SourceRuleContext): Boolean {
        val pants = ctx.charState.equippedItem(EquipmentSlot.PANTS).orEmpty()
        if (pants.isBlank()) return false
        val gapName = ctx.base.gameDatabase.item(GREAT_PANTS_ID)?.name
        val replicaName = ctx.base.gameDatabase.item(REPLICA_GREAT_PANTS_ID)?.name
        return pants.equals(gapName, ignoreCase = true) ||
            pants.equals(replicaName, ignoreCase = true) ||
            pants.contains("Greatest American Pants", ignoreCase = true)
    }

    private fun gapDuration(effectName: String): Int? = when (effectName) {
        "Super Skill" -> 5
        "Super Structure", "Super Accuracy" -> 10
        "Super Vision", "Super Speed" -> 20
        else -> null
    }
}
