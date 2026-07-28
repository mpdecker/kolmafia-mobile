package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Per-coinmaster zone/quest gates for [CoinmasterManager.isAccessible].
 * Returns null when accessible, or a human-readable reason when not.
 */
object CoinmasterAccessibility {

    fun inaccessibleReason(
        master: CoinmasterData,
        char: CharacterState,
        prefs: Preferences? = null,
        accessibleCount: (Int) -> Int = { 0 },
    ): String? {
        if (!master.hasShopEndpoint()) return "Shop not available"
        for (nick in master.allNicknames) {
            ruleFor(nick.lowercase(), prefs, accessibleCount)?.invoke(char)?.let { return it }
        }
        return null
    }

    fun isAccessible(
        master: CoinmasterData,
        char: CharacterState,
        prefs: Preferences? = null,
        accessibleCount: (Int) -> Int = { 0 },
    ): Boolean = inaccessibleReason(master, char, prefs, accessibleCount) == null

    private fun ruleFor(
        nickname: String,
        prefs: Preferences?,
        accessibleCount: (Int) -> Int,
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
        else -> null
    }
}

private fun CoinmasterData.hasShopEndpoint(): Boolean =
    shopId != null || buyUrl != null || sellUrl != null
