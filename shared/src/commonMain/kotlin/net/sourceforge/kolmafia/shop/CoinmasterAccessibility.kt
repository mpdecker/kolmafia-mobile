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
            if (accessibleCount(FunALogUnlockPrefs.PIRATE_REALM_FUN_LOG) <= 0) {
                "Need PirateRealm fun-a-log"
            } else {
                null
            }
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
