package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.clan.ClanLoungeSync
import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.item.FreeCraftingTurns
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.modifiers.VykeaCompanionData
import net.sourceforge.kolmafia.shop.DesertBeachAccessibility
import net.sourceforge.kolmafia.skill.SkillData

/** Desktop ConcoctionDatabase.recalculatePermittedMethods v1/v2 for selected craft methods. */
object ConcoctionMethodGates {

    private const val VYKEA_HEX_KEY = 8729
    private const val SOURCE_TERMINAL = 9033
    private const val REPLICA_SOURCE_TERMINAL = 11231
    private const val STILLSUIT = 10932
    private const val MAYAM_CALENDAR = 11572
    private const val TAKERSPACE_LETTER_OF_MARQUE = 11687
    private const val GUIDE_TO_BURNING_LEAVES = 11340
    private const val REAGNIMATED_GNOME = 162
    private const val SLEDGEHAMMER_OF_VAELKYR = 4316
    private const val CLIP_ART = 7216
    private const val JEWELRY_PLIERS = 709
    private const val THORS_PLIERS = 7709

    private val FLOUNDRY_ITEM_IDS = intArrayOf(
        9001, // carpe
        9002, // codpiece
        9003, // troutsers
        9004, // bass clarinet
        9005, // fish hatchet
        9006, // tunac
    )

    fun isPermitted(
        method: String,
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
        familiarUsable: (Int) -> Boolean = { false },
        skills: List<SkillData> = emptyList(),
        limitMode: String = "none",
        resultName: String? = null,
    ): Boolean = when (method) {
        "FLOUNDRY" -> isFloundryPermitted(state, prefs, accessibleCount, resultName)
        "BARREL" -> isBarrelPermitted(state, prefs)
        "GNOME_TINKER" -> isGnomeTinkerPermitted(state, prefs)
        "GNOME_PART" -> isGnomePartPermitted(prefs, familiarUsable)
        "BURNING_LEAVES" -> isBurningLeavesPermitted(prefs, accessibleCount)
        "VYKEA" -> isVykeaPermitted(prefs, accessibleCount)
        "TERMINAL" -> isTerminalPermitted(prefs, state, accessibleCount)
        "SPACEGATE" -> isSpacegatePermitted(state, prefs)
        "FANTASY_REALM" -> isFantasyRealmPermitted(state, prefs)
        "STILLSUIT" -> isStillsuitPermitted(state, accessibleCount)
        "MAYAM" -> isMayamPermitted(state, accessibleCount)
        "PHOTO_BOOTH" -> isPhotoBoothPermitted(prefs)
        "TAKERSPACE" -> isTakerspacePermitted(prefs)
        "SPEAKEASY" -> isSpeakeasyPermitted(limitMode, resultName, state)
        "HOT_DOG" -> isHotDogPermitted(limitMode, resultName, prefs, state)
        "STAFF" -> isStaffPermitted(state, prefs)
        "PHINEAS" -> isPhineasPermitted(accessibleCount)
        "COOK" -> KitchenEquipmentGates.isCookPermitted(state, prefs)
        "MIX" -> KitchenEquipmentGates.isMixPermitted(state, prefs)
        "COOK_FANCY" -> KitchenEquipmentGates.isCookFancyPermitted(
            state, prefs, skills, accessibleCount, familiarUsable, limitMode,
        )
        "MIX_FANCY" -> KitchenEquipmentGates.isMixFancyPermitted(
            state, prefs, skills, accessibleCount, familiarUsable, limitMode,
        )
        "SMITH" -> SmithingGates.isSmithPermitted(state, prefs, accessibleCount, limitMode)
        "SSMITH" -> SmithingGates.isSSmithPermitted(state, prefs, accessibleCount, limitMode)
        "CLIPART" -> isClipArtPermitted(state, prefs, skills, limitMode)
        "JEWEL", "JEWELRY" -> isJewelryPermitted(accessibleCount)
        "ROLL", "ROLLING_PIN", "SEWER", "MUSE", "SUSE", "MULTI_USE", "SINGLE_USE" -> true
        "WAX", "NEWSPAPER", "METEOROID", "WOOL" -> true
        else -> true
    }

    private fun isFloundryPermitted(
        state: CharacterState,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
        resultName: String?,
    ): Boolean {
        if (inBadMoon(state)) return false
        if (!ClanLoungeSync.hasFloundry(prefs)) return false
        if (hasFloundryItemToday(prefs, accessibleCount)) return false
        if (resultName != null && !FloundryAvailability.isAvailable(resultName)) return false
        return StandardRequest.isAllowed(
            RestrictedItemType.ITEMS,
            "Clan Floundry",
            state,
        )
    }

    private fun isBarrelPermitted(state: CharacterState, prefs: Preferences?): Boolean {
        if (inBadMoon(state)) return false
        if (prefs?.getBoolean("barrelShrineUnlocked", false) != true) return false
        if (prefs.getBoolean("_barrelPrayer", false)) return false
        return StandardRequest.isAllowed(
            RestrictedItemType.ITEMS,
            "shrine to the Barrel god",
            state,
        )
    }

    private fun isGnomeTinkerPermitted(state: CharacterState, prefs: Preferences?): Boolean {
        if (state.inZombiecore) return false
        return gnomadsAvailable(state, prefs)
    }

    private fun isVykeaPermitted(prefs: Preferences?, accessibleCount: (Int) -> Int): Boolean {
        val current = prefs?.getString("_currentVykea", "") ?: ""
        if (current.isNotBlank() && VykeaCompanionData.isValid(current)) return false
        return accessibleCount(VYKEA_HEX_KEY) > 0
    }

    private fun isGnomePartPermitted(
        prefs: Preferences?,
        familiarUsable: (Int) -> Boolean,
    ): Boolean {
        if (prefs?.getBoolean("_gnomePart", false) == true) return false
        return familiarUsable(REAGNIMATED_GNOME)
    }

    private fun isBurningLeavesPermitted(
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
    ): Boolean =
        CampgroundItemSync.hasBurningLeaves(prefs) ||
            accessibleCount(GUIDE_TO_BURNING_LEAVES) > 0

    private fun isTerminalPermitted(
        prefs: Preferences?,
        state: CharacterState,
        accessibleCount: (Int) -> Int,
    ): Boolean {
        val hasCampgroundTerminal = CampgroundItemSync.hasSourceTerminal(prefs) ||
            accessibleCount(SOURCE_TERMINAL) > 0 ||
            (state.inLegacyOfLoathing && accessibleCount(REPLICA_SOURCE_TERMINAL) > 0)
        val hasFalloutTerminal = state.inNuclearAutumn && accessibleCount(SOURCE_TERMINAL) > 0
        if (!hasCampgroundTerminal && !hasFalloutTerminal) return false
        return (prefs?.getInt("_sourceTerminalExtrudes", 0) ?: 0) < 3
    }

    private fun isSpacegatePermitted(state: CharacterState, prefs: Preferences?): Boolean {
        if (inBadMoon(state)) return false
        if (prefs?.getBoolean("spacegateAlways", false) != true) return false
        return StandardRequest.isAllowed(
            RestrictedItemType.ITEMS,
            "Spacegate access badge",
            state,
        )
    }

    private fun isFantasyRealmPermitted(state: CharacterState, prefs: Preferences?): Boolean {
        if (inBadMoon(state)) return false
        val hoursLeft = prefs?.getString("_frHoursLeft", "") ?: ""
        if (hoursLeft.toIntOrNull() != null) return false
        val frOpen = prefs?.getBoolean("frAlways", false) == true ||
            prefs?.getBoolean("_frToday", false) == true
        if (!frOpen) return false
        return StandardRequest.isAllowed(
            RestrictedItemType.ITEMS,
            "FantasyRealm membership packet",
            state,
        )
    }

    private fun isStillsuitPermitted(state: CharacterState, accessibleCount: (Int) -> Int): Boolean {
        if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "tiny stillsuit", state)) {
            return false
        }
        return accessibleCount(STILLSUIT) > 0
    }

    private fun isMayamPermitted(state: CharacterState, accessibleCount: (Int) -> Int): Boolean {
        if (inBadMoon(state)) return false
        if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Mayam Calendar", state)) {
            return false
        }
        return accessibleCount(MAYAM_CALENDAR) > 0
    }

    private fun isPhotoBoothPermitted(prefs: Preferences?): Boolean {
        if (!ClanLoungeSync.hasPhotoBooth(prefs)) return false
        return (prefs?.getInt("_photoBoothEquipment", 0) ?: 0) < 3
    }

    private fun isSpeakeasyPermitted(
        limitMode: String,
        resultName: String?,
        state: CharacterState,
    ): Boolean {
        if (LimitModeGates.limitClan(limitMode)) return false
        if (!ClanLoungeSync.isSpeakeasyAllowed(state)) return false
        if (resultName == null) return false
        return SpeakeasyAvailability.isAvailable(resultName)
    }

    private fun isHotDogPermitted(
        limitMode: String,
        resultName: String?,
        prefs: Preferences?,
        state: CharacterState,
    ): Boolean {
        if (LimitModeGates.limitClan(limitMode)) return false
        if (!ClanLoungeSync.isHotDogStandAllowed(state)) return false
        if (resultName == null) return false
        if (!HotDogAvailability.isAvailable(resultName)) return false
        if (HotDogDatabase.isFancyHotDog(resultName) &&
            prefs?.getBoolean(ClanLoungeSync.FANCY_HOT_DOG_EATEN_PREF, false) == true
        ) {
            return false
        }
        return true
    }

    private fun isTakerspacePermitted(prefs: Preferences?): Boolean =
        CampgroundItemSync.hasWorkshedItem(prefs, TAKERSPACE_LETTER_OF_MARQUE)

    private fun isStaffPermitted(state: CharacterState, prefs: Preferences?): Boolean {
        if (!state.characterClassEnum.isMysticality) return false
        return prefs?.getInt("lastGuildStoreOpen", -1) == state.ascensionNumber
    }

    private fun isPhineasPermitted(accessibleCount: (Int) -> Int): Boolean =
        accessibleCount(SLEDGEHAMMER_OF_VAELKYR) > 0

    private fun isClipArtPermitted(
        state: CharacterState,
        prefs: Preferences?,
        skills: List<SkillData>,
        limitMode: String,
    ): Boolean {
        if (!skills.any { it.id == CLIP_ART }) return false
        if (inBadMoon(state) && !skillsRecalled(state, prefs)) return false
        val canInteract = !state.isHardcore && !state.isInRonin
        val summonsUsed = if (canInteract) {
            prefs?.getInt("_clipartSummons", 0) ?: 0
        } else {
            prefs?.getInt("tomeSummons", 0) ?: 0
        }
        return summonsUsed < 3
    }

    private fun isJewelryPermitted(accessibleCount: (Int) -> Int): Boolean =
        accessibleCount(JEWELRY_PLIERS) > 0 || accessibleCount(THORS_PLIERS) > 0

    private fun hasFloundryItemToday(prefs: Preferences?, accessibleCount: (Int) -> Int): Boolean {
        if (prefs?.getBoolean("_floundryItemUsed", false) == true) return true
        return FLOUNDRY_ITEM_IDS.any { accessibleCount(it) > 0 }
    }

    private fun gnomadsAvailable(state: CharacterState, prefs: Preferences?): Boolean {
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        if (sign != ZodiacSign.WOMBAT &&
            sign != ZodiacSign.BLENDER &&
            sign != ZodiacSign.PACKRAT
        ) {
            return false
        }
        if (!DesertBeachAccessibility.isAvailable(state, prefs)) return false
        if (state.isKingdomOfExploathing) return false
        return true
    }

    private fun skillsRecalled(state: CharacterState, prefs: Preferences?): Boolean {
        if (state.skillsRecalled) return true
        return prefs?.getBoolean("skillsRecalled", false) == true
    }

    private fun inBadMoon(state: CharacterState): Boolean =
        ZodiacSign.find(state.zodiacSign)?.isBadMoon == true
}

/** Desktop ConcoctionDatabase kitchen equipment gates (COOK/MIX + fancy v3). */
internal object KitchenEquipmentGates {

    private const val COCKTAIL_MAGIC = 15008

    fun isCookPermitted(state: CharacterState, prefs: Preferences?): Boolean =
        hasOven(prefs) || hasRange(prefs) || inWereProfessor(state)

    fun isMixPermitted(state: CharacterState, prefs: Preferences?): Boolean =
        hasShaker(prefs) || hasCocktailKit(prefs) || inWereProfessor(state)

    fun isCookFancyPermitted(
        state: CharacterState,
        prefs: Preferences?,
        skills: List<SkillData>,
        accessibleCount: (Int) -> Int,
        familiarUsable: (Int) -> Boolean,
        limitMode: String,
    ): Boolean {
        val buyTool = KitchenAutoBuy.willBuyTool(state, prefs, limitMode)
        if (!hasRange(prefs) && !buyTool) return false
        if (hasChef(prefs)) return true
        if (KitchenAutoBuy.willBuyServant(prefs, state, limitMode)) return true
        if (BoxServantAvailability.isAvailable(
                BoxServantAvailability.CHEF,
                BoxServantAvailability.CLOCKWORK_CHEF,
                prefs,
                state,
                accessibleCount,
                skills,
                familiarUsable,
            )
        ) {
            return true
        }
        if (requiresBoxServant(prefs, state) && !hasChef(prefs)) return false
        val freeTurns = FreeCraftingTurns.freeTurnsForMethod(
            "COOK_FANCY",
            freeCraftingContext(state, prefs, skills),
        )
        return state.adventuresLeft + freeTurns > 0
    }

    fun isMixFancyPermitted(
        state: CharacterState,
        prefs: Preferences?,
        skills: List<SkillData>,
        accessibleCount: (Int) -> Int,
        familiarUsable: (Int) -> Boolean,
        limitMode: String,
    ): Boolean {
        val buyTool = KitchenAutoBuy.willBuyTool(state, prefs, limitMode)
        if (!hasCocktailKit(prefs) && !buyTool) return false
        if (hasBartender(prefs)) return true
        if (KitchenAutoBuy.willBuyServant(prefs, state, limitMode)) return true
        if (BoxServantAvailability.isAvailable(
                BoxServantAvailability.BARTENDER,
                BoxServantAvailability.CLOCKWORK_BARTENDER,
                prefs,
                state,
                accessibleCount,
                skills,
                familiarUsable,
            )
        ) {
            return true
        }
        if (skills.any { it.id == COCKTAIL_MAGIC }) return true
        if (requiresBoxServant(prefs, state) && !hasBartender(prefs)) return false
        val freeTurns = FreeCraftingTurns.freeTurnsForMethod(
            "MIX_FANCY",
            freeCraftingContext(state, prefs, skills),
        )
        return state.adventuresLeft + freeTurns > 0
    }

    private fun freeCraftingContext(
        state: CharacterState,
        prefs: Preferences?,
        skills: List<SkillData>,
    ): FreeCraftingTurns.Context =
        FreeCraftingTurns.Context(
            preferences = prefs,
            state = state,
            skills = skills,
        )

    fun hasOven(prefs: Preferences?): Boolean =
        prefs?.getBoolean("hasOven", false) == true

    fun hasRange(prefs: Preferences?): Boolean =
        prefs?.getBoolean("hasRange", false) == true

    fun hasShaker(prefs: Preferences?): Boolean =
        prefs?.getBoolean("hasShaker", false) == true

    fun hasCocktailKit(prefs: Preferences?): Boolean =
        prefs?.getBoolean("hasCocktailKit", false) == true

    fun hasChef(prefs: Preferences?): Boolean =
        prefs?.getBoolean("hasChef", false) == true

    fun hasBartender(prefs: Preferences?): Boolean =
        prefs?.getBoolean("hasBartender", false) == true

    private fun inWereProfessor(state: CharacterState): Boolean =
        state.ascensionPath == AscensionPath.WEREPROFESSOR

    private fun requiresBoxServant(prefs: Preferences?, state: CharacterState): Boolean =
        prefs?.getBoolean("requireBoxServants", false) == true && !state.inGLover
}
