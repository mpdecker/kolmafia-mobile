package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState

/**
 * Per-coinmaster zone/quest gates for [CoinmasterManager.isAccessible].
 * Returns null when accessible, or a human-readable reason when not.
 */
object CoinmasterAccessibility {

    fun inaccessibleReason(master: CoinmasterData, char: CharacterState): String? {
        if (!master.hasShopEndpoint()) return "Shop not available"
        for (nick in master.allNicknames) {
            ruleFor(nick.lowercase())?.invoke(char)?.let { return it }
        }
        return null
    }

    fun isAccessible(master: CoinmasterData, char: CharacterState): Boolean =
        inaccessibleReason(master, char) == null

    private fun ruleFor(nickname: String): ((CharacterState) -> String?)? = when (nickname) {
        "dimemaster", "dmt" ->
            { cs -> if (!cs.kingLiberated) "King Ralph must be freed first" else null }
        "shore" ->
            { cs -> if (cs.level < 4) "Requires level 4" else null }
        "mystic" ->
            { cs -> if (cs.level < 6) "Requires level 6" else null }
        "starchart" ->
            { cs -> if (cs.level < 8) "Requires level 8" else null }
        "hunter", "bhh" ->
            { cs -> if (cs.isHardcore || cs.isInRonin) "Not accessible during HC/Ronin" else null }
        else -> null
    }
}

private fun CoinmasterData.hasShopEndpoint(): Boolean =
    shopId != null || buyUrl != null || sellUrl != null
