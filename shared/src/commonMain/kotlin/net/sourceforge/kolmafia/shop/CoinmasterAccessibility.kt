package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Per-coinmaster zone/quest gates for [CoinmasterManager.isAccessible].
 * Returns null when accessible, or a human-readable reason when not.
 */
object CoinmasterAccessibility {

    private const val SPINMASTER_LATHE = 10582
    private const val SEPTEMBER_CENSER = 11642
    private const val WORSE_HOMES_GARDENS = 6731
    private const val MILD_MANNERED_PROFESSOR = 2897

    fun inaccessibleReason(
        master: CoinmasterData,
        char: CharacterState,
        prefs: Preferences? = null,
        accessibleCount: (Int) -> Int = { 0 },
        hasEffect: (Int) -> Boolean = { false },
    ): String? {
        if (!master.hasShopEndpoint()) return "Shop not available"
        for (nick in master.allNicknames) {
            ruleFor(nick.lowercase(), prefs, accessibleCount, hasEffect, char.limitMode)?.invoke(char)
                ?.let { return it }
        }
        return null
    }

    fun isAccessible(
        master: CoinmasterData,
        char: CharacterState,
        prefs: Preferences? = null,
        accessibleCount: (Int) -> Int = { 0 },
        hasEffect: (Int) -> Boolean = { false },
    ): Boolean = inaccessibleReason(master, char, prefs, accessibleCount, hasEffect) == null

    private fun ruleFor(
        nickname: String,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
        hasEffect: (Int) -> Boolean,
        limitMode: String,
    ): ((CharacterState) -> String?)? = when (nickname) {
        "dimemaster", "dmt" ->
            { cs -> if (!cs.kingLiberated) "King Ralph must be freed first" else null }
        "shore" -> { cs ->
            when {
                cs.level < 4 -> "Requires level 4"
                !DesertBeachAccessibility.isAvailable(cs, prefs) ->
                    "You can't get to the desert beach"
                else -> null
            }
        }
        "mystic" -> { cs ->
            when {
                cs.level < 6 -> "Requires level 6"
                cs.isKingdomOfExploathing ->
                    "The Kingdom has exploded, and the mystic is nowhere to be found."
                else -> null
            }
        }
        "starchart" ->
            { cs -> if (cs.level < 8) "Requires level 8" else null }
        "hunter", "bhh" ->
            { cs -> if (cs.isHardcore || cs.isInRonin) "Not accessible during HC/Ronin" else null }
        "jarl" ->
            { cs ->
                if (cs.ascensionPath != AscensionPath.AVATAR_OF_JARLSBERG) {
                    "You are not an Avatar of Jarlsberg"
                } else {
                    null
                }
            }
        "swagger" ->
            { cs ->
                when {
                    cs.isHardcore -> "Characters in Hardcore or Ronin cannot redeem Swagger"
                    cs.isInRonin -> "Characters in Hardcore or Ronin cannot redeem Swagger"
                    else -> null
                }
            }
        "mrreplica" ->
            { cs ->
                if (!cs.inLegacyOfLoathing) {
                    "Only Legacy Loathers can buy replica Mr. Items"
                } else {
                    null
                }
            }
        "5dprinter" -> { _ ->
            if (!FiveDPrinterAccessibility.isShopAccessible(accessibleCount)) {
                "You do not have a Xiblaxian 5D printer."
            } else {
                null
            }
        }
        "piraterealm", "piraterealmfunalog" -> { _ ->
            QuestShopAccessibility.funALogInaccessible(accessibleCount)
        }
        in TimeTowerSync.CHRONER_SHOP_IDS -> { _ ->
            TimeTowerAccessibility.inaccessibleReason(nickname, prefs)
        }
        "trapper" -> { cs ->
            when {
                cs.level < 8 -> "You haven't met the Trapper yet"
                prefs?.getInt("lastTr4pz0rQuest", -1) != cs.ascensionNumber ->
                    "You have unfinished business with the Trapper"
                cs.inZombiecore -> "The trapper won't be back for quite a while"
                else -> null
            }
        }
        "lathe" -> { _ ->
            if (accessibleCount(SPINMASTER_LATHE) <= 0) {
                "You don't own a SpinMaster\u2122 lathe"
            } else {
                null
            }
        }
        "september" -> { cs ->
            when {
                accessibleCount(SEPTEMBER_CENSER) <= 0 ->
                    "You need a Sept-Ember Censer in order to shop here."
                cs.isKingdomOfExploathing -> "Your Censer has exploaded."
                else -> null
            }
        }
        "junkmagazine" -> { _ ->
            if (accessibleCount(WORSE_HOMES_GARDENS) <= 0) {
                "You can't make that without a copy of Worse Homes and Gardens."
            } else {
                null
            }
        }
        "sbb_brogurt", "brogurt", "sbb_taco", "taco_dan", "sbb_jimmy", "buffjimmy" -> { _ ->
            SpringBreakBeachAccessibility.inaccessibleReason(prefs, limitMode)
        }
        // Phase 6731–6750 — elemental airport shops (Behavioral Deepen XLVIII Track A).
        "infernodisco", "discogiftco" -> { _ ->
            AirportShopAccessibility.hotInaccessible(prefs, limitMode)
        }
        "glaciest", "walmart", "wal-mart" -> { _ ->
            AirportShopAccessibility.coldInaccessible(prefs, limitMode)
        }
        "landfillstore", "dinseystore" -> { _ ->
            AirportShopAccessibility.stenchInaccessible(prefs, limitMode)
        }
        "airport" -> { _ ->
            AirportShopAccessibility.dutyFreeInaccessible(prefs)
        }
        "si_shop1", "shawarma" -> { _ ->
            AirportShopAccessibility.shawarmaInaccessible(prefs, limitMode)
        }
        "si_shop2", "canteen" -> { _ ->
            AirportShopAccessibility.canteenInaccessible(prefs, limitMode)
        }
        // si_shop3 / thearmory only — Armory & Leggery uses shopId "armory"; do not match bare "armory".
        "si_shop3", "thearmory" -> { _ ->
            AirportShopAccessibility.spacegateArmoryInaccessible(prefs, limitMode)
        }
        // Phase 6751–6770 — inventory-token shops (Behavioral Deepen XLVIII Track B).
        "toxic", "toxicchemistry",
        "fishbones", "fishbonery",
        "guzzlr",
        "warbear", "warbearbox",
        "showerthoughts",
        "fdkol",
        -> { _ ->
            InventoryTokenShopAccessibility.inaccessibleReason(nickname, accessibleCount)
        }
        // Phase 6771–6790 — KOLHS + Batfellow shops (Behavioral Deepen XLVIII Track C).
        "kolhs_art" -> { cs ->
            KolhsShopAccessibility.inaccessibleReason(
                cs,
                prefs,
                unlockPref = "lastKOLHSArtClassUnlockAdventure",
                classLabel = "Art Class",
            )
        }
        "kolhs_chem" -> { cs ->
            KolhsShopAccessibility.inaccessibleReason(
                cs,
                prefs,
                unlockPref = "lastKOLHSChemClassUnlockAdventure",
                classLabel = "Chemistry Class",
            )
        }
        "kolhs_shop" -> { cs ->
            KolhsShopAccessibility.inaccessibleReason(
                cs,
                prefs,
                unlockPref = "lastKOLHSShopClassUnlockAdventure",
                classLabel = "Shop Class",
            )
        }
        "batman_chemicorp" -> { cs ->
            BatCoinmasterAccessibility.downtownInaccessibleReason(
                cs.limitMode,
                BatCoinmasterAccessibility.CHEMICORP,
            )
        }
        "batman_orphanage" -> { cs ->
            BatCoinmasterAccessibility.downtownInaccessibleReason(
                cs.limitMode,
                BatCoinmasterAccessibility.ORPHANAGE,
            )
        }
        "batman_pd" -> { cs ->
            BatCoinmasterAccessibility.downtownInaccessibleReason(
                cs.limitMode,
                BatCoinmasterAccessibility.PD,
            )
        }
        // Phase 6791–6810 — path/pref shops (Behavioral Deepen XLIX Track A).
        "olivers", "speakeasy", "fancydan" -> { _ ->
            PathShopAccessibility.fancyDanInaccessible(prefs)
        }
        "exploathing", "cosmicraysbazaar" -> { cs ->
            PathShopAccessibility.cosmicRaysInaccessible(cs)
        }
        "pokefam", "pokemporium" -> { cs ->
            PathShopAccessibility.pokemporiumInaccessible(cs)
        }
        "mutate", "geneticfiddling" -> { cs ->
            PathShopAccessibility.geneticFiddlingInaccessible(cs)
        }
        "mariogear", "marioitems", "plumbergear", "plumberitem" -> { cs ->
            PathShopAccessibility.plumberInaccessible(cs)
        }
        "edunder_shopshop", "edshop" -> { cs ->
            PathShopAccessibility.edShopInaccessible(cs)
        }
        "detective", "precinct" -> { _ ->
            PathShopAccessibility.precinctInaccessible(prefs)
        }
        "rumple" -> { _ ->
            PathShopAccessibility.rumpleInaccessible(prefs)
        }
        "spacegate" -> { _ ->
            PathShopAccessibility.spacegateInaccessible(prefs)
        }
        "campfire" -> { cs ->
            PathShopAccessibility.campfireInaccessible(cs, prefs)
        }
        "cindy", "boutique" -> { _ ->
            PathShopAccessibility.boutiqueInaccessible(accessibleCount)
        }
        // Phase 6811–6830 — quest / Bat fabricator (XLIX Track B).
        "grandma" -> { _ ->
            QuestShopAccessibility.grandmaInaccessible(prefs)
        }
        "blackmarket" -> { cs ->
            QuestShopAccessibility.blackMarketInaccessible(cs, prefs)
        }
        "batman_cave" -> { cs ->
            BatCoinmasterAccessibility.fabricatorInaccessibleReason(cs.limitMode)
        }
        // Phase 6851–6870 — specialty IoTM / zone shops (HTTP Residual L Track A).
        "shadowforge" -> { cs ->
            SpecialtyShopAccessibility.shadowForgeInaccessible(cs, prefs)
        }
        "fantasyrealm", "rubee" -> { _ ->
            SpecialtyShopAccessibility.fantasyRealmInaccessible(prefs)
        }
        "cyber_dedigitizer", "dedigitizer" -> { _ ->
            SpecialtyShopAccessibility.dedigitizerInaccessible(prefs)
        }
        "sandpenny", "wetcrap" -> { cs ->
            SpecialtyShopAccessibility.sandPennyInaccessible(cs)
        }
        "topiary", "nugglet", "nuggletcrafting" -> { _ ->
            SpecialtyShopAccessibility.topiaryInaccessible(accessibleCount)
        }
        // Phase 6871–6890 — legacy token + Mr Store 2002 (HTTP Residual L Track B).
        "mrstore2002" -> { cs ->
            LegacyCoinmasterAccessibility.mrStore2002Inaccessible(cs, accessibleCount)
        }
        "burt" -> { _ ->
            LegacyCoinmasterAccessibility.burtInaccessible(accessibleCount)
        }
        "fudge", "fudgewand" -> { _ ->
            LegacyCoinmasterAccessibility.fudgeWandInaccessible(accessibleCount)
        }
        "hermit" -> { cs ->
            LegacyCoinmasterAccessibility.hermitInaccessible(cs)
        }
        "gameshoppe", "gamestore" -> { cs ->
            LegacyCoinmasterAccessibility.gameShoppeInaccessible(cs)
        }
        "damachine", "vendingmachine" -> { cs ->
            if (cs.isKingdomOfExploathing) {
                "The vending machine exploded"
            } else {
                null
            }
        }
        "wereprofessor_tinker" -> { _ ->
            if (!hasEffect(MILD_MANNERED_PROFESSOR)) {
                "Only a mild-mannered professor can work at their Tinkering Bench."
            } else {
                null
            }
        }
        "flowertradein" -> { _ ->
            if (!FlowerTradeinAccessibility.hasTradeFlower(accessibleCount)) {
                "You have no roses or tulips"
            } else {
                null
            }
        }
        "crimbo23_elf_armory", "crimbo23_pirate_armory",
        "crimbo23_elf_bar", "crimbo23_pirate_bar",
        "crimbo23_elf_cafe", "crimbo23_pirate_cafe",
        "crimbo23_elf_factory", "crimbo23_pirate_factory",
        -> { _ ->
            Crimbo23ShopAccessibility.inaccessibleReason(nickname, prefs)
        }
        else -> null
    }
}

private fun CoinmasterData.hasShopEndpoint(): Boolean =
    shopId != null || buyUrl != null || sellUrl != null
