package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterRegistry

/**
 * Resolves `$coinmaster[field]` bracket access. Mirrors desktop CoinmasterProxy metadata.
 */
internal object CoinmasterEntityFields {

    fun resolve(
        coinmasterRef: String,
        fieldName: String,
        preferences: Preferences?,
        inventory: Map<Int, Int>,
    ): AshValue {
        val data = resolveCoinmaster(coinmasterRef)

        return when (fieldName.lowercase()) {
            "token" -> AshValue.of(data?.token ?: "")
            "item" -> {
                val itemName = data?.tokenItemId()?.let { ItemDatabase.getById(it)?.name } ?: ""
                AshValue(AshType.ITEM, itemName)
            }
            "property" -> AshValue.of(data?.property ?: "")
            "available_tokens" -> AshValue.of(
                (data?.availableTokens(preferences, inventory) ?: 0).toLong(),
            )
            "buys" -> AshValue.of(data?.sellItems?.isNotEmpty() == true)
            "sells" -> AshValue.of(data?.buyItems?.isNotEmpty() == true)
            "nickname" -> AshValue.of(data?.nickname ?: "")
            "shopid" -> AshValue.of(data?.shopId ?: "")
            else -> throw ScriptException("coinmaster has no field '$fieldName'")
        }
    }

    private fun resolveCoinmaster(ref: String): CoinmasterData? {
        val trimmed = ref.trim()
        if (trimmed.isEmpty()) return null
        return CoinmasterRegistry.findByMasterName(trimmed)
            ?: CoinmasterRegistry.findByNickname(trimmed)
    }
}
