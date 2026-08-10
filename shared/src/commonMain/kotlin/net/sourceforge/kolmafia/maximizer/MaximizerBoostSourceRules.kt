package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.data.HotDogDatabase
import net.sourceforge.kolmafia.data.PocketDatabase
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.data.SpeakeasyDatabase
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.mood.BreakfastBurnSkills
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.session.BreakfastItemIds
import net.sourceforge.kolmafia.session.BreakfastManager

/** Pluggable exotic effect-source rules (Phase 389–390). Desktop Maximizer.java ~724–1444. */
object MaximizerBoostSourceRules {

    private const val PILL_KEEPER_ID = 10333

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
        FriarsRule,
        PillkeeperRule,
        PoolRule,
        ShowerRule,
        SwimRule,
        JukeboxRule,
        BallpitRule,
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
}
