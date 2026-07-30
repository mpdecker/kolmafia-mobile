package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.RestoreDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.TCRSDatabase
import net.sourceforge.kolmafia.modifiers.ExpressionContext
import net.sourceforge.kolmafia.modifiers.StringModifier
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ItemUseLimitsContext
import net.sourceforge.kolmafia.request.maximumUses
import net.sourceforge.kolmafia.shop.CoinmasterRegistry

/**
 * Resolves `$item[field]` bracket access. Mirrors desktop ItemProxy consumable v1,
 * restore v2, flag v3, metadata v4, dailyusesleft v5, shop/skill v6, and tcrs_name v7 fields.
 */
internal object ItemEntityFields {

    fun resolve(
        itemName: String,
        fieldName: String,
        gameDatabase: GameDatabase?,
        expressionContext: ExpressionContext = ExpressionContext.EMPTY,
        characterState: CharacterState = CharacterState(),
        preferences: Preferences? = null,
        itemUseLimitsContext: ItemUseLimitsContext? = null,
    ): AshValue {
        val item = gameDatabase?.item(itemName) ?: ItemDatabase.getByName(itemName)
        val itemId = item?.id ?: 0
        val resolvedName = item?.name ?: itemName
        return when (fieldName.lowercase()) {
            "id" -> AshValue.of(itemId.toLong())
            "name" -> AshValue.of(item?.name ?: itemName)
            "plural" -> AshValue.of(
                item?.let { ItemDatabase.getPluralName(it.id) } ?: "",
            )
            "descid" -> AshValue.of(item?.descId ?: "")
            "image" -> AshValue.of(item?.image ?: "")
            "smallimage" -> AshValue.of(ItemDatabase.getSmallImage(itemId))
            "levelreq" -> AshValue.of((ConsumableDatabase.getLevelReqByName(itemName) ?: 0).toLong())
            "quality" -> AshValue.of(ConsumableDatabase.getQualityName(itemName))
            "adventures" -> AshValue.of(ConsumableDatabase.getAdventureRange(itemName))
            "muscle" -> AshValue.of(ConsumableDatabase.getMuscleRange(itemName))
            "mysticality" -> AshValue.of(ConsumableDatabase.getMysticalityRange(itemName))
            "moxie" -> AshValue.of(ConsumableDatabase.getMoxieRange(itemName))
            "fullness" -> AshValue.of(ConsumableDatabase.getFullnessByName(itemName).toLong())
            "inebriety" -> AshValue.of(ConsumableDatabase.getInebrietyByName(itemName).toLong())
            "spleen" -> AshValue.of(ConsumableDatabase.getSpleenByName(itemName).toLong())
            "minhp" -> AshValue.of(RestoreDatabase.getHpMinByName(itemName, expressionContext).toLong())
            "maxhp" -> AshValue.of(RestoreDatabase.getHpMaxByName(itemName, expressionContext).toLong())
            "minmp" -> AshValue.of(RestoreDatabase.getMpMinByName(itemName, expressionContext).toLong())
            "maxmp" -> AshValue.of(RestoreDatabase.getMpMaxByName(itemName, expressionContext).toLong())
            "quest" -> AshValue.of(ItemDatabase.isQuestItem(itemId))
            "gift", "giftable" -> AshValue.of(ItemDatabase.isGiftItem(itemId))
            "tradeable" -> AshValue.of(ItemDatabase.isTradeable(itemId))
            "discardable" -> AshValue.of(ItemDatabase.isDiscardable(itemId))
            "usable" -> AshValue.of(ItemDatabase.isUsable(itemId))
            "multi" -> AshValue.of(ItemDatabase.isMultiUsable(itemId))
            "reusable" -> AshValue.of(ItemDatabase.isReusable(itemId))
            "combat" -> AshValue.of(ItemDatabase.isCombatUsable(itemId))
            "combat_reusable" -> AshValue.of(ItemDatabase.isCombatReusable(itemId))
            "fancy" -> AshValue.of(ItemDatabase.isFancyItem(itemId))
            "pasteable" -> AshValue.of(ItemDatabase.isPasteable(itemId))
            "smithable" -> AshValue.of(ItemDatabase.isSmithable(itemId))
            "cookable" -> AshValue.of(ItemDatabase.isCookable(itemId))
            "mixable" -> AshValue.of(ItemDatabase.isMixable(itemId))
            "notes" -> AshValue.of(ConsumableDatabase.getNotesByName(itemName))
            "potion" -> AshValue.of(ItemDatabase.isPotion(itemId))
            "chocolate" -> AshValue.of(ItemDatabase.isChocolateItem(itemId))
            "candy" -> AshValue.of(ItemDatabase.isCandyItem(itemId))
            "candy_type" -> AshValue.of(ItemDatabase.getCandyTypeName(itemId))
            "name_length" -> AshValue.of(ItemDatabase.getNameLength(itemId).toLong())
            "dailyusesleft" -> AshValue.of(
                maximumUses(
                    itemId,
                    resolvedName,
                    itemUseLimitsContext ?: ItemUseLimitsContext(
                        characterState,
                        preferences,
                        expressionContext,
                    ),
                ).toLong(),
            )
            "seller" -> AshValue(
                AshType.COINMASTER,
                CoinmasterRegistry.findSeller(itemId)?.masterName ?: "",
            )
            "buyer" -> AshValue(
                AshType.COINMASTER,
                CoinmasterRegistry.findBuyer(itemId)?.masterName ?: "",
            )
            "skill" -> {
                val skillName = ModifierDatabase.getStringModifier(resolvedName, StringModifier.SKILL)
                AshValue(AshType.SKILL, skillName)
            }
            "recipe" -> {
                val recipeName = ModifierDatabase.getStringModifier(resolvedName, StringModifier.RECIPE)
                AshValue(AshType.ITEM, recipeName)
            }
            "noob_skill" -> {
                val noobSkillId = ItemDatabase.getNoobSkillId(itemId)
                val skillName = SkillDefinitionDatabase.getById(noobSkillId)?.name ?: ""
                AshValue(AshType.SKILL, skillName)
            }
            "tcrs_name" -> AshValue.of(TCRSDatabase.getTCRSName(itemId))
            else -> throw ScriptException("item has no field '$fieldName'")
        }
    }
}
