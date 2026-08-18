package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.updateQuestData] airport / Dinsey combat-win writers.
 */
object AirportCombatSync {

    private val TACO_FISH_PATTERN = Regex("""gain (\d+) taco fish meat""")

    private val WAREHOUSE_MONSTERS = setOf(
        "warehouse guard",
        "warehouse janitor",
        "warehouse clerk",
    )

    fun apply(
        monster: String,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (monster.isBlank()) return false
        val lower = monster.trim().lowercase()
        return when {
            lower == "sloppy seconds burger" -> applyBurger(html, questDatabase, preferences)
            lower == "sloppy seconds cocktail" -> applyCocktail(html, questDatabase, preferences)
            lower == "sloppy seconds sundae" -> applySundae(html, questDatabase, preferences)
            lower == "taco fish" -> applyTacoFish(html, questDatabase, preferences)
            lower == "fun-guy playmate" -> applyFunGuy(html, questDatabase, preferences)
            lower in WAREHOUSE_MONSTERS -> applyWarehouse(preferences)
            lower == "e.v.e., the robot zombie" -> applyEve(questDatabase)
            lower == "nasty bear" -> applyNastyBear(questDatabase, preferences)
            else -> false
        }
    }

    private fun applyBurger(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        if (!html.contains("You consult the list and grab the next ingredient")) return false
        val next = preferences.getInt("buffJimmyIngredients", 0) + 1
        preferences.setInt("buffJimmyIngredients", next)
        if (next >= 15) {
            questDatabase?.setProgress(Quest.JIMMY_CHEESEBURGER, "step1")
        }
        return true
    }

    private fun applyCocktail(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        if (!html.contains("cocktail sauce bottle") && !html.contains("defeated foe with your bottle")) {
            return false
        }
        val next = preferences.getInt("tacoDanCocktailSauce", 0) + 1
        preferences.setInt("tacoDanCocktailSauce", next)
        if (next >= 15) {
            questDatabase?.setProgress(Quest.TACO_DAN_COCKTAIL, "step1")
        }
        return true
    }

    private fun applySundae(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        if (!html.contains("sprinkles off")) return false
        val next = preferences.getInt("brodenSprinkles", 0) + 1
        preferences.setInt("brodenSprinkles", next)
        if (next >= 15) {
            questDatabase?.setProgress(Quest.BRODEN_SPRINKLES, "step1")
        }
        return true
    }

    private fun applyTacoFish(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        val amount = TACO_FISH_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        val next = preferences.getInt("tacoDanFishMeat", 0) + amount
        preferences.setInt("tacoDanFishMeat", next)
        if (next >= 300) {
            questDatabase?.setProgress(Quest.TACO_DAN_FISH, "step1")
        }
        return true
    }

    private fun applyFunGuy(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        preferences.setInt("funGuyMansionKills", preferences.getInt("funGuyMansionKills", 0) + 1)
        if (html.contains("hot tub with some more bacteria")) {
            val next = preferences.getInt("brodenBacteria", 0) + 1
            preferences.setInt("brodenBacteria", next)
            if (next >= 10) {
                questDatabase?.setProgress(Quest.BRODEN_BACTERIA, "step1")
            }
        }
        return true
    }

    private fun applyWarehouse(preferences: Preferences?): Boolean {
        if (preferences == null) return false
        preferences.setInt("warehouseProgress", preferences.getInt("warehouseProgress", 0) + 1)
        return true
    }

    private fun applyEve(questDatabase: QuestDatabase?): Boolean {
        if (questDatabase == null) return false
        questDatabase.setProgress(Quest.EVE, "step1")
        return true
    }

    private fun applyNastyBear(
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        val next = (preferences.getInt("dinseyNastyBearsDefeated", 0) + 1).coerceAtMost(8)
        preferences.setInt("dinseyNastyBearsDefeated", next)
        questDatabase?.setProgress(Quest.NASTY_BEARS, if (next == 8) "step2" else "step1")
        return true
    }
}
