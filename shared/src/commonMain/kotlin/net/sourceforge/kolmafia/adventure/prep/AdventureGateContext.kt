package net.sourceforge.kolmafia.adventure.prep

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.AdventureZone
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

/** Shared context for AdventurePrep gates and prepare actions (Phases 1851–1910). */
data class AdventureGateContext(
    val character: CharacterState? = null,
    val preferences: Preferences? = null,
    val questDatabase: QuestDatabase? = null,
    val inventoryCount: (Int) -> Int = { 0 },
    val hasEquipped: (Int) -> Boolean = { false },
    val hasCampground: Boolean = true,
) {
    val quests: QuestDatabase?
        get() = questDatabase ?: preferences?.let { QuestDatabase(it) }

    fun hasItem(itemId: Int, atLeast: Int = 1): Boolean =
        inventoryCount(itemId) >= atLeast

    fun isQuestStarted(quest: Quest): Boolean =
        quests?.isQuestStarted(quest) == true

    fun isAtLeast(quest: Quest, step: String): Boolean =
        quests?.isAtLeast(quest, step) == true

    fun isLaterThan(quest: Quest, step: String): Boolean =
        quests?.isQuestLaterThan(quest, step) == true

    fun isFinished(quest: Quest): Boolean =
        quests?.isQuestFinished(quest) == true

    fun prefBool(key: String, default: Boolean = false): Boolean =
        preferences?.getBoolean(key, default) ?: default

    fun prefString(key: String, default: String = ""): String =
        preferences?.getString(key, default) ?: default

    fun prefInt(key: String, default: Int = 0): Int =
        preferences?.getInt(key, default) ?: default

    val ascensions: Int get() = character?.ascensionNumber ?: 0
    val level: Int get() = character?.level ?: 1
    val isKoE: Boolean get() = character?.isKingdomOfExploathing == true
}

object AdventureUnlockHelpers {
    /** Desktop [KoLAdventure.woodsOpen]. */
    fun woodsOpen(ctx: AdventureGateContext): Boolean =
        ctx.isQuestStarted(Quest.LARVA) || ctx.isQuestStarted(Quest.CITADEL)

    /** Desktop [KoLAdventure.cemetaryOpen]. */
    fun cemeteryOpen(ctx: AdventureGateContext): Boolean =
        ctx.isQuestStarted(Quest.CYRPT) ||
            ctx.isQuestStarted(Quest.EGO) ||
            ctx.isQuestStarted(Quest.NEMESIS)

    fun desertBeachAccessible(ctx: AdventureGateContext): Boolean {
        if (ctx.isKoE) return false
        val last = ctx.prefInt("lastDesertUnlock", -1)
        return last == ctx.ascensions ||
            ctx.hasItem(ItemIds.BITCHIN_MEATCAR) ||
            ctx.hasItem(ItemIds.DESERT_BUS_PASS) ||
            ctx.hasItem(ItemIds.PUMPKIN_CARRIAGE) ||
            ctx.hasItem(ItemIds.TIN_LIZZIE)
    }

    fun islandAccessible(ctx: AdventureGateContext): Boolean {
        if (ctx.prefInt("lastIslandUnlock", -1) == ctx.ascensions) return true
        return ctx.hasItem(ItemIds.DINGY_DINGHY) ||
            ctx.hasItem(ItemIds.SKIFF) ||
            ctx.hasItem(ItemIds.YELLOW_SUBMARINE)
    }

    /**
     * Pref-only [checkZone] — permanent or today access without HTTP map probe.
     */
    fun checkZoneAccess(
        alwaysPref: String?,
        todayPref: String?,
        ctx: AdventureGateContext,
        milkCapUnlock: Boolean = false,
    ): Boolean {
        if (alwaysPref != null && ctx.prefBool(alwaysPref)) return true
        if (todayPref != null && ctx.prefBool(todayPref)) return true
        if (milkCapUnlock) return true
        // No always pref and no today → inaccessible until synced
        return alwaysPref == null && todayPref == null
    }
}

/** High-traffic item ids used by AdventurePrep gates/prepare. */
object ItemIds {
    const val ENCHANTED_BEAN = 186
    const val BITCHIN_MEATCAR = 134
    const val DINGY_DINGHY = 141
    const val KNOB_GOBLIN_PERFUME = 307
    const val TRANSFUNCTIONER = 458
    const val SONAR = 563
    const val SOCK = 609
    const val ASTRAL_MUSHROOM = 1622
    const val GONG = 3353
    const val KNOB_CAKE = 4942
    const val DRUNKULA_WINEGLASS = 6474
    const val BILLIARDS_KEY = 7301
    const val LIBRARY_KEY = 7302
    const val SPOOKYRAVEN_NECKLACE = 7303
    const val SPOOKYRAVEN_TELEGRAM = 7304
    const val BONE_WITH_A_PRICE_TAG = 8158
    const val BOOZE_MAP = 8187
    const val HYPNOTIC_BREADCRUMBS = 8199
    const val DESERT_BUS_PASS = 4770
    const val PUMPKIN_CARRIAGE = 4769
    const val TIN_LIZZIE = 6775
    const val SKIFF = 5885
    const val YELLOW_SUBMARINE = 8376
    const val OPEN_PORTABLE_SPACEGATE = 9477
    const val MILK_CAP = 11012
    const val FILTHWORM_QUEEN_HEART = 2347
    const val FILTHWORM_HATCHLING_GLAND = 2344
    const val FILTHWORM_DRONE_GLAND = 2345
    const val FILTHWORM_GUARD_GLAND = 2346
    const val DINGHY_PLANS = 146
    const val DINGY_PLANKS = 140
    const val PIRATE_FLEDGES = 3033
    const val ROYAL_JELLY = 2309
    const val CASINO_PASS = 40
    const val ABSINTHE = 2655
    const val DRINK_ME_POTION = 4508
    const val TRANSPONDER = 5170
    const val DEVILISH_FOLIO = 5444
    const val BLACK_GLASS = 6398
    const val MACHINE_SNOWGLOBE = 8749
    const val FANTASY_REALM_GEM = 9837
    const val DRIP_HARNESS = 10441
    const val EMPTY_AGUA_DE_VIDA_BOTTLE = 4130
    const val TRAPEZOID = 3198
}

fun AdventureZone.hasSnarfblat(): Boolean =
    urlParams.contains("adventure=", ignoreCase = true)
