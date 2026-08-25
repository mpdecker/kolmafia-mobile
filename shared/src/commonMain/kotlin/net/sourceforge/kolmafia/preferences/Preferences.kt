package net.sourceforge.kolmafia.preferences

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import net.sourceforge.kolmafia.data.DefaultsDatabase

class Preferences(private val settings: Settings) {

    fun hasKey(key: String): Boolean = settings.hasKey(key)

    fun removeKey(key: String) {
        settings.remove(key)
    }

    fun storedKeys(): Set<String> =
        (settings as? ObservableSettings)?.keys ?: emptySet()

    fun getString(key: String, default: String? = null): String =
        settings.getString(key, default ?: DefaultsDatabase.getString(key))

    fun setString(key: String, value: String) =
        settings.putString(key, value)

    fun getBoolean(key: String, default: Boolean? = null): Boolean =
        if (default != null) {
            settings.getBoolean(key, default)
        } else {
            settings.getBoolean(key, DefaultsDatabase.getBoolean(key))
        }

    fun setBoolean(key: String, value: Boolean) =
        settings.putBoolean(key, value)

    fun getInt(key: String, default: Int? = null): Int =
        if (default != null) {
            settings.getInt(key, default)
        } else {
            settings.getInt(key, DefaultsDatabase.getInt(key))
        }

    fun setInt(key: String, value: Int) =
        settings.putInt(key, value)

    fun getLong(key: String, default: Long? = null): Long =
        if (default != null) {
            settings.getLong(key, default)
        } else {
            val intDefault = DefaultsDatabase.getInt(key)
            if (intDefault != 0 || DefaultsDatabase.has(key)) {
                intDefault.toLong()
            } else {
                settings.getLong(key, 0L)
            }
        }

    fun setLong(key: String, value: Long) =
        settings.putLong(key, value)

    fun getFloat(key: String, default: Float = 0f): Float {
        if (!settings.hasKey(key)) return default
        val asString = settings.getString(key, "")
        return asString.toFloatOrNull() ?: default
    }

    fun setFloat(key: String, value: Float) =
        settings.putString(key, value.toString())

    fun registerCounterName(name: String) {
        val existing = getString(COUNTER_NAMES, "").split('|').filter { it.isNotBlank() }.toMutableSet()
        if (existing.add(name)) {
            setString(COUNTER_NAMES, existing.sorted().joinToString("|"))
        }
    }

    fun counterNames(): List<String> =
        getString(COUNTER_NAMES, "").split('|').filter { it.isNotBlank() }

    companion object Keys {
        const val LAST_USERNAME = "lastUsername"

        // HP recovery
        const val AUTO_RECOVER_HP          = "autoRecoverHp"
        const val HP_RECOVERY_TARGET_PCT   = "hpRecoveryTargetPct"   // below → start recovering
        const val HP_RECOVERY_STOP_PCT     = "hpRecoveryStopPct"     // above → stop recovering
        /** Desktop autoAbortThreshold — fraction of max HP; <0 disables. */
        const val AUTO_ABORT_THRESHOLD     = "autoAbortThreshold"

        // MP recovery
        const val AUTO_RECOVER_MP          = "autoRecoverMp"
        const val MP_RECOVERY_TARGET_PCT   = "mpRecoveryTargetPct"
        const val MP_RECOVERY_STOP_PCT     = "mpRecoveryStopPct"     // above → stop recovering

        // Combat tracking
        const val LAST_MONSTER             = "_lastMonster"   // string; last monster fought
        const val DEMON_NAME_12              = "demonName12"    // Intergnat demon name (desktop key)
        const val DEMON_NAME_13              = "demonName13"    // Yeg demon name (cargo scrap syllables)
        const val DEMON_NAME_14              = "demonName14"    // Demon in Combat name
        const val DEMON_NAME_14_SEGMENTS     = "demonName14Segments"
        const val DEMON_SUMMONED             = "demonSummoned"  // daily demon summon flag
        const val CARGO_POCKET_SCRAPS        = "cargoPocketScraps"
        const val CARGO_POCKETS_EMPTIED      = "cargoPocketsEmptied"
        const val CARGO_POCKET_EMPTIED       = "_cargoPocketEmptied"
        const val ALLIED_RADIO_DROPS_USED    = "_alliedRadioDropsUsed"
        const val ALLIED_RADIO_MATERIEL_INTEL = "_alliedRadioMaterielIntel"
        const val ALLIED_RADIO_WILDSUN_BOON  = "_alliedRadioWildsunBoon"
        const val NONCOMBAT_FORCER_ACTIVE    = "noncombatForcerActive"
        const val ROYALTY                    = "royalty"
        const val COUNTER_NAMES            = "counterNames"   // pipe-separated counter pref names

        // Mood
        const val AUTO_BUFF                = "autoBuff"
        const val REMOVE_MALIGNANT_EFFECTS = "removeMalignantEffects"  // default true
        const val ACTIVE_MOOD_NAME         = "activeMoodName"        // persisted active mood name
        const val ACTIVE_MOOD_TRIGGERS     = "activeMoodTriggers"    // serialized trigger list
        const val MOOD_LIBRARY_NAMES       = "moodLibraryNames"      // |-separated saved mood names
        // Per-mood data stored under "moodTriggers_${name}" (dynamic key, not a constant)

        // Banish tracking
        const val BANISHED_MONSTERS = "banishedMonsters"   // serialized banish list (same key as desktop)
        const val BANISHED_PHYLA = "banishedPhyla"         // phylum banishes (Patriotic Screech)

        // ManaBurn
        const val MANA_BURN_ENABLED        = "manaBurnEnabled"       // default false
        const val MANA_BURN_MIN_MP_PCT     = "manaBurnMinMpPct"      // burn when MP% >= this; default 90
        const val ALLOW_NON_MOOD_BURNING   = "allowNonMoodBurning"   // burn non-mood buffs; default false
        const val MAX_MANA_BURN            = "maxManaBurn"           // max effect duration extension; default 1000
        const val ALLOW_SUMMON_BURNING     = "allowSummonBurning"    // breakfast/libram burn path; Phase 351
        const val MANA_BURN_SUMMON_THRESHOLD = "manaBurnSummonThreshold" // MP% to prefer summons; 0=off
        const val MANA_BURN_SKILLS         = "manaBurnSkills"        // pipe-separated skill priority list
        const val PETE_MOTORBIKE_MUFFLER   = "peteMotorbikeMuffler"
        const val LIBRAM_SUMMONS           = "libramSummons"         // daily libram cast count for MP cost
        const val LIBRAM_SKILLS_HARDCORE   = "libramSkillsHardcore"
        const val LIBRAM_SKILLS_SOFTCORE   = "libramSkillsSoftcore"
        const val LAST_CHANCE_BURN         = "lastChanceBurn"        // CLI when no other burn target
        const val LAST_CHANCE_THRESHOLD    = "lastChanceThreshold"   // min available MP to use lastChanceBurn; default 100

        fun skillBurnPrefKey(skillId: Int): String = "skillBurn$skillId"

        fun libramSkillsPrefKey(isHardcore: Boolean): String =
            if (isHardcore) LIBRAM_SKILLS_HARDCORE else LIBRAM_SKILLS_SOFTCORE

        // Adventure location tracking
        const val ADVENTURE_SPENT_TURNS    = "adventureSpentTurns"
        const val WILDFIRE_FIRE_LEVELS     = "wildfireFireLevels"
        const val LAST_LOCATION            = "_lastLocation"
        const val CACHED_CLOSET            = "_cachedCloset"
        const val CACHED_STORAGE           = "_cachedStorage"
        const val CACHED_FREEPULLS         = "_cachedFreepulls"
        const val CACHED_STASH             = "_cachedStash"
        const val CACHED_DISPLAY           = "_cachedDisplay"
        const val CACHED_CAMPGROUND        = "_cachedCampground"
        const val COMBAT_SCRIPT           = "combatScript"
        const val BETWEEN_BATTLE_SCRIPT   = "betweenBattleScript"
        const val AFTER_ADVENTURE_SCRIPT  = "afterAdventureScript"
        const val USER_NOTE                = "userNote"
        const val CURRENT_CHAT_CHANNEL     = "currentChatChannel"
        const val AUTO_SCRIPTING           = "autoScripting"
        const val TRACK_LIGHTS_OUT        = "trackLightsOut"

        // Breakfast — user-controlled guard prefs (match desktop names exactly)
        const val HARVEST_GARDEN_SOFTCORE   = "harvestGardenSoftcore"   // "none"|"any"; default "none" (matches desktop)
        const val HARVEST_GARDEN_HARDCORE   = "harvestGardenHardcore"   // "none"|"any"; default "none"
        const val VISIT_RUMPUS_SOFTCORE     = "visitRumpusSoftcore"     // boolean; default true
        const val VISIT_RUMPUS_HARDCORE     = "visitRumpusHardcore"     // boolean; default true
        const val VISIT_LOUNGE_SOFTCORE     = "visitLoungeSoftcore"     // boolean; default true
        const val VISIT_LOUNGE_HARDCORE     = "visitLoungeHardcore"     // boolean; default true
        const val READ_MANUAL_SOFTCORE      = "readManualSoftcore"      // boolean; default true
        const val READ_MANUAL_HARDCORE      = "readManualHardcore"      // boolean; default true
        const val CHECK_JACKASS_SOFTCORE    = "checkJackassSoftcore"    // boolean; default true
        const val CHECK_JACKASS_HARDCORE    = "checkJackassHardcore"    // boolean; default true
        const val COLLECT_SEA_JELLY_SOFTCORE = "collectSeaJellySoftcore" // boolean; default true
        const val COLLECT_SEA_JELLY_HARDCORE = "collectSeaJellyHardcore" // boolean; default true

        // Breakfast — done-today sentinels (cleared at rollover)
        const val BREAKFAST_COMPLETED       = "breakfastCompleted"      // boolean
        const val GARDEN_HARVESTED          = "_gardenHarvested"        // boolean
        const val BREAKFAST_RUMPUS          = "_breakfastRumpus"        // boolean
        const val GUILD_MANUAL_USED         = "_guildManualUsed"        // boolean
        const val DELUXE_KLAW_SUMMONS       = "_deluxeKlawSummons"      // int 0–3
        const val LOOKING_GLASS             = "_lookingGlass"           // boolean
        const val FIREWORKS_SHOP            = "_fireworksShop"          // boolean
        const val POOL_GAME_RESULT          = "_poolGames"              // int; 0 = not played, 1+ = played

        // Rollover gating
        const val LAST_DAYCOUNT             = "lastBreakfastDaycount"   // int; -1 = never stored
        const val LAST_ASCENSION_NUMBER     = "lastAscensionNumber"     // int; -1 = never stored

        // Rufus / Shadow Rift
        const val RUFUS_QUEST_TYPE          = "_rufusQuestType"         // string: "entity"|"artifact"|"monument"
        const val RUFUS_QUEST_TARGET        = "_rufusQuestTarget"       // string: target name after quest accepted

        // Villain Lair / Shadow Rifts
        const val VILLAIN_LAIR_COLOR        = "_villainLairColor"       // string: door color to pick

        // VampOut / Interview With You (a Vampire) — daily tracking
        const val INTERVIEW_VLAD        = "_interviewVlad"        // boolean; true = visited
        const val INTERVIEW_ISABELLA    = "_interviewIsabella"     // boolean; true = visited
        const val INTERVIEW_MASQUERADE  = "_interviewMasquerade"   // boolean; true = visited

        // BreakfastManager new sentinels (Phase 13)
        const val CLOVER_SOUGHT              = "_cloverSought"              // boolean; hermit clover trade done today
        const val APRIL_SHOWER_GLOBS         = "_aprilShowerGlobsCollected" // boolean
        const val BOOK_OF_EVERY_SKILL_USED   = "_bookOfEverySkillUsed"      // boolean
        const val REPLICA_SNOWCONE_USED      = "_replicaSnowconeTomeUsed"   // boolean
        const val REPLICA_RESOLUTION_USED    = "_replicaResolutionLibramUsed" // boolean
        const val REPLICA_SMITH_USED         = "_replicaSmithsTomeUsed"     // boolean
        const val HAND_RADIO_USED            = "_handRadioUsed"             // boolean
        const val ANTICHEESE_COLLECTED       = "_anticheeseCollected"       // boolean
        const val LAST_ANTICHEESE_DAY        = "lastAnticheeseDay"          // int; global day when anticheese last used
        const val BATTERIES_HARVESTED        = "_batteriesHarvested"        // boolean
        const val POCKET_WISHES_USED         = "_pocketWishesUsed"          // boolean
        const val BOXING_DAYDREAM            = "_boxingDaydream"            // boolean
        const val SPINNING_WHEEL_USED        = "_spinningWheelUsed"         // boolean
        const val BIG_ISLAND_VISITED         = "_bigIslandVisited"          // boolean
        const val VOLCANO_ISLAND_VISITED     = "_volcanoIslandVisited"      // boolean
        const val HARDWOOD_COLLECTED         = "_hardwoodCollected"         // boolean
        const val MR_STORE_CREDITS_COLLECTED = "_2002MrStoreCreditsCollected" // boolean
        const val SERVER_ROOM_VISITED        = "_serverRoomVisited"         // boolean
        const val JACKASS_PLUMBER_USED       = "_jackassPlumberGame"        // boolean; daily limit sentinel
        const val SEA_JELLY_COLLECTED        = "_seaJellyCollected"         // boolean
        // Per-toy sentinels are dynamic: "_toyUsed_$toyId" — no compile-time constant needed
    }
}
