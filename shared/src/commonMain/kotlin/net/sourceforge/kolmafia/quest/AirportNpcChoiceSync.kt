package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Sleaze Airport NPC choices 915–917.
 */
object AirportNpcChoiceSync {

    const val JIMMY_CHOICE = 915
    const val TACO_DAN_CHOICE = 916
    const val BRODEN_CHOICE = 917

    const val PENCIL_THIN_MUSHROOM = 7421
    const val CHEESEBURGER_RECIPE = 7422
    const val SAILOR_SALT = 7423
    const val BROUPON = 7424
    const val TACO_DAN_SAUCE_BOTTLE = 7425
    const val SPRINKLE_SHAKER = 7426
    const val TACO_DAN_RECEIPT = 7428

    fun apply(
        choiceId: Int,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean = when (choiceId) {
        JIMMY_CHOICE -> applyJimmy(html, questDatabase, preferences, consumeItem)
        TACO_DAN_CHOICE -> applyTacoDan(html, questDatabase, preferences, consumeItem)
        BRODEN_CHOICE -> applyBroden(html, questDatabase, preferences, consumeItem)
        else -> false
    }

    private fun applyJimmy(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        if (questDatabase == null) return false
        return when {
            html.contains("skinny mushroom girls") -> {
                questDatabase.setProgress(Quest.JIMMY_MUSHROOM, QuestDatabase.STARTED)
                true
            }
            html.contains("But here's a few Beach Bucks as a token of my changes in gratitude") -> {
                questDatabase.setProgress(Quest.JIMMY_MUSHROOM, QuestDatabase.FINISHED)
                consumeItem(PENCIL_THIN_MUSHROOM, 10)
                true
            }
            html.contains("not really into moving out of this hammock") -> {
                questDatabase.setProgress(Quest.JIMMY_CHEESEBURGER, QuestDatabase.STARTED)
                preferences?.setInt("buffJimmyIngredients", 0)
                true
            }
            html.contains("So I'll just give you some Beach Bucks instead") -> {
                questDatabase.setProgress(Quest.JIMMY_CHEESEBURGER, QuestDatabase.FINISHED)
                preferences?.setInt("buffJimmyIngredients", 0)
                consumeItem(CHEESEBURGER_RECIPE, 1)
                true
            }
            html.contains("sons of sons of sailors are") -> {
                questDatabase.setProgress(Quest.JIMMY_SALT, QuestDatabase.STARTED)
                true
            }
            html.contains("So here's some Beach Bucks instead") -> {
                questDatabase.setProgress(Quest.JIMMY_SALT, QuestDatabase.FINISHED)
                consumeItem(SAILOR_SALT, 50)
                true
            }
            else -> false
        }
    }

    private fun applyTacoDan(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        if (questDatabase == null) return false
        return when {
            html.contains("find those receipts") -> {
                questDatabase.setProgress(Quest.TACO_DAN_AUDIT, QuestDatabase.STARTED)
                true
            }
            html.contains("Here's a little Taco Dan's Taco Stand gratitude for ya") -> {
                questDatabase.setProgress(Quest.TACO_DAN_AUDIT, QuestDatabase.FINISHED)
                consumeItem(TACO_DAN_RECEIPT, 10)
                true
            }
            html.contains("fill it up with as many cocktail drippings") -> {
                questDatabase.setProgress(Quest.TACO_DAN_COCKTAIL, QuestDatabase.STARTED)
                preferences?.setInt("tacoDanCocktailSauce", 0)
                true
            }
            html.contains("sample of Taco Dan's Taco Stand's Tacoriffic Cocktail Sauce") -> {
                questDatabase.setProgress(Quest.TACO_DAN_COCKTAIL, QuestDatabase.FINISHED)
                preferences?.setInt("tacoDanCocktailSauce", 0)
                consumeItem(TACO_DAN_SAUCE_BOTTLE, 1)
                true
            }
            html.contains("get enough taco fish") -> {
                questDatabase.setProgress(Quest.TACO_DAN_FISH, QuestDatabase.STARTED)
                preferences?.setInt("tacoDanFishMeat", 0)
                true
            }
            html.contains("batch of those Taco Dan's Taco Stand's Taco Fish Tacos") -> {
                questDatabase.setProgress(Quest.TACO_DAN_FISH, QuestDatabase.FINISHED)
                preferences?.setInt("tacoDanFishMeat", 0)
                true
            }
            else -> false
        }
    }

    private fun applyBroden(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        if (questDatabase == null) return false
        return when {
            html.contains("need about ten shots of it") -> {
                questDatabase.setProgress(Quest.BRODEN_BACTERIA, QuestDatabase.STARTED)
                preferences?.setInt("brodenBacteria", 0)
                true
            }
            html.contains("YOLO cup to spit the bacteria into") -> {
                questDatabase.setProgress(Quest.BRODEN_BACTERIA, QuestDatabase.FINISHED)
                preferences?.setInt("brodenBacteria", 0)
                true
            }
            html.contains("loan you my sprinkle shaker to fill up") -> {
                questDatabase.setProgress(Quest.BRODEN_SPRINKLES, QuestDatabase.STARTED)
                preferences?.setInt("brodenSprinkles", 0)
                true
            }
            html.contains("can sell some <i>deluxe</i> brogurts") -> {
                questDatabase.setProgress(Quest.BRODEN_SPRINKLES, QuestDatabase.FINISHED)
                preferences?.setInt("brodenSprinkles", 0)
                consumeItem(SPRINKLE_SHAKER, 1)
                true
            }
            html.contains("There were like fifteen of these guys") -> {
                questDatabase.setProgress(Quest.BRODEN_DEBT, QuestDatabase.STARTED)
                true
            }
            html.contains("And they all had broupons, huh") -> {
                questDatabase.setProgress(Quest.BRODEN_DEBT, QuestDatabase.FINISHED)
                consumeItem(BROUPON, 15)
                true
            }
            else -> false
        }
    }
}
