package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ConcoctionBuyables
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [CreateItemRequest.parseCrafting] (Phases 2151–2165).
 * Consumes ingredients from `<!-- cr:NxA,B=C -->` and applies free-craft prefs.
 */
object CreateItemCraftSync {
    private val CRAFT_COMMENT = Regex("""<!-- ?cr:(\d+)x(-?\d+),(-?\d+)=(\d+) ?-->""")
    private val MODE_PATTERN = Regex("""[?&]mode=([^&]+)""", RegexOption.IGNORE_CASE)
    private val DISCOVERY_PATTERN = Regex(
        """descitem\.php\?whichitem=([a-f0-9]+)""",
        RegexOption.IGNORE_CASE,
    )

    data class CraftResult(
        val created: Int = 0,
        val discoveriesCleared: Int = 0,
        val servantBlewUp: Boolean = false,
    )

    /**
     * @return total created count from craft comments (0 on failure / non-craft URL)
     */
    fun parseCrafting(
        location: String,
        responseText: String,
        inventory: InventoryManager? = null,
        preferences: Preferences? = null,
        characterState: CharacterState? = null,
        sessionLogger: SessionLogger? = null,
    ): CraftResult {
        if (!location.contains("craft.php", ignoreCase = true)) {
            return CraftResult()
        }
        if (location.contains("action=pulverize", ignoreCase = true)) {
            return CraftResult()
        }

        val mode = MODE_PATTERN.find(location)?.groupValues?.get(1)?.lowercase().orEmpty()
        if (mode == "discoveries") {
            return parseDiscoveries(responseText, preferences)
        }

        if (CraftRequest.isCraftFailure(responseText)) {
            return CraftResult()
        }

        val matches = CRAFT_COMMENT.findAll(responseText).toList()
        if (matches.isEmpty()) {
            return CraftResult()
        }

        val paste = mode == "combine" &&
            (characterState == null || !characterState.knollAvailable || characterState.inZombiecore)

        var totalCreated = 0
        val commentStarts = mutableListOf<Pair<Int, Int>>()

        for (m in matches) {
            val qty = m.groupValues[1].toIntOrNull() ?: 0
            val item1 = m.groupValues[2].toIntOrNull() ?: 0
            val item2 = m.groupValues[3].toIntOrNull() ?: 0
            val resultId = m.groupValues[4].toIntOrNull() ?: 0

            if (item1 > 0) inventory?.consumeItemLocally(item1, qty)
            if (item2 > 0) inventory?.consumeItemLocally(item2, qty)
            if (paste && qty > 0) {
                inventory?.consumeItemLocally(ConcoctionBuyables.MEAT_PASTE, qty)
            }

            logCraftUse(qty, item1, item2, sessionLogger)

            val pref = "unknownRecipe$resultId"
            if (preferences?.getBoolean(pref, false) == true) {
                preferences.setBoolean(pref, false)
                ConcoctionDatabase.markRefreshNeeded()
            }

            if (ItemDatabase.isFancyItem(item1) || ItemDatabase.isFancyItem(item2)) {
                when {
                    mode == "cook" && preferences?.getBoolean("hasChef", false) == true ->
                        preferences.setInt(
                            "chefTurnsUsed",
                            preferences.getInt("chefTurnsUsed", 0) + qty,
                        )
                    mode == "cocktail" && preferences?.getBoolean("hasBartender", false) == true ->
                        preferences.setInt(
                            "bartenderTurnsUsed",
                            preferences.getInt("bartenderTurnsUsed", 0) + qty,
                        )
                }
            }

            totalCreated += qty
            commentStarts.add(m.range.first to qty)
        }

        val servantBlewUp = responseText.contains("Smoke", ignoreCase = false)
        if (servantBlewUp && preferences != null) {
            when (mode) {
                "cook" -> preferences.setBoolean("hasChef", false)
                "cocktail" -> preferences.setBoolean("hasBartender", false)
            }
            val servant = when (mode) {
                "cook" -> "chef"
                "cocktail" -> "bartender"
                else -> "servant"
            }
            RequestLogger.updateSessionLog("Your $servant blew up", sessionLogger)
        }

        applyFreeCraftPrefs(mode, responseText, commentStarts, preferences)

        return CraftResult(created = totalCreated, servantBlewUp = servantBlewUp)
    }

    private fun parseDiscoveries(html: String, preferences: Preferences?): CraftResult {
        var cleared = 0
        for (m in DISCOVERY_PATTERN.findAll(html)) {
            val descId = m.groupValues[1]
            val id = ItemDatabase.getByDescId(descId)?.id ?: continue
            val pref = "unknownRecipe$id"
            if (preferences?.getBoolean(pref, false) == true) {
                preferences.setBoolean(pref, false)
                cleared++
                ConcoctionDatabase.markRefreshNeeded()
            }
        }
        return CraftResult(discoveriesCleared = cleared)
    }

    private fun logCraftUse(qty: Int, item1: Int, item2: Int, sessionLogger: SessionLogger?) {
        val msg = when {
            item1 < 0 -> "Crafting used $qty${ItemDatabase.getItemName(item2)}"
            item2 < 0 -> "Crafting used $qty${ItemDatabase.getItemName(item1)}"
            else ->
                "Crafting used $qty each of ${ItemDatabase.getItemName(item1)} and ${ItemDatabase.getItemName(item2)}"
        }
        RequestLogger.updateSessionLog(msg, sessionLogger)
    }

    private fun applyFreeCraftPrefs(
        mode: String,
        responseText: String,
        comments: List<Pair<Int, Int>>,
        preferences: Preferences?,
    ) {
        if (preferences == null || comments.isEmpty()) return
        val ends = comments.map { it.first } + responseText.length
        for (i in comments.indices) {
            val start = comments[i].first
            val created = comments[i].second
            val end = ends.getOrElse(i + 1) { responseText.length }
            val section = responseText.substring(start, end.coerceAtMost(responseText.length))
            var turnsSaved = 0

            if (mode == "smith") {
                if (section.contains("jackhammer lets you finish your smithing in record time")) {
                    val saved = minOf(3 - preferences.getInt("_legionJackhammerCrafting", 0), created - turnsSaved)
                    if (saved > 0) {
                        preferences.setInt(
                            "_legionJackhammerCrafting",
                            (preferences.getInt("_legionJackhammerCrafting", 0) + saved).coerceAtMost(3),
                        )
                        turnsSaved += saved
                    }
                }
                if (section.contains("auto-anvil handles some of the smithing")) {
                    val saved = minOf(5 - preferences.getInt("_warbearAutoAnvilCrafting", 0), created - turnsSaved)
                    if (saved > 0) {
                        preferences.setInt(
                            "_warbearAutoAnvilCrafting",
                            (preferences.getInt("_warbearAutoAnvilCrafting", 0) + saved).coerceAtMost(5),
                        )
                        turnsSaved += saved
                    }
                }
                if (section.contains("use Thor's Pliers to do the job super fast")) {
                    val saved = minOf(10 - preferences.getInt("_thorsPliersCrafting", 0), created - turnsSaved)
                    if (saved > 0) {
                        preferences.setInt(
                            "_thorsPliersCrafting",
                            (preferences.getInt("_thorsPliersCrafting", 0) + saved).coerceAtMost(10),
                        )
                        turnsSaved += saved
                    }
                }
            }

            fun bumpDaily(pref: String, max: Int, signal: String) {
                if (!section.contains(signal)) return
                val saved = minOf(max - preferences.getInt(pref, 0), created - turnsSaved)
                if (saved > 0) {
                    preferences.setInt(pref, (preferences.getInt(pref, 0) + saved).coerceAtMost(max))
                    turnsSaved += saved
                }
            }

            if (section.contains("You are so relaxed that your crafting takes hardly any time at all!")) {
                val charges = preferences.getInt("homebodylCharges", 0)
                val saved = minOf(charges, created - turnsSaved)
                if (saved > 0) {
                    preferences.setInt("homebodylCharges", (charges - saved).coerceAtLeast(0))
                    turnsSaved += saved
                }
            }
            if (section.contains("knock the job out in record time")) {
                val charges = preferences.getInt("craftingPlansCharges", 0)
                val saved = minOf(charges, created - turnsSaved)
                if (saved > 0) {
                    preferences.setInt("craftingPlansCharges", (charges - saved).coerceAtLeast(0))
                    turnsSaved += saved
                }
            }
            bumpDaily("_cookbookbatCrafting", 5, "The advice from your cookbookbat is really saving time")
            bumpDaily("_elfGuardCookingUsed", 3, "That elf guard training has made you fast!")
            bumpDaily("_oldSchoolCocktailCraftingUsed", 3, "That old-school cocktail training has made you fast!")
            bumpDaily(
                "_rapidPrototypingUsed",
                5,
                "That rapid prototyping programming you downloaded is really paying dividends",
            )
            bumpDaily("_expertCornerCutterUsed", 5, "You really crafted that item the LyleCo way")
            bumpDaily(
                "_holidayMultitaskingUsed",
                3,
                "With your holiday multitasking skills, you finished that crafting in record time.",
            )
        }
    }
}
