package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Latte Shop choice 1329 via [LatteRequest.parseVisitChoice]/[LatteRequest.parseResponse]/[LatteRequest.parseFight].
 */
object LatteChoiceSync {

    const val CHOICE_ID = 1329
    const val LATTE_MUG_ID = 9987
    const val LATTE_MUG_NAME = "latte lovers member's mug"

    const val THROW_LATTE_SKILL = 7301
    const val OFFER_LATTE_SKILL = 7302
    const val GULP_LATTE_SKILL = 7303

    private val REFILL_PATTERN = Regex("""You've got <b>(\d+)</b> refill""")
    private val LINE_PATTERN = Regex("""<tr style=.*?</tr>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val INPUT_PATTERN = Regex(
        """name=["']l(\d)["']\s+(?:checked\s+)?value=["'](.*?)["']>\s*(.*?)\s*</td>""",
        RegexOption.IGNORE_CASE,
    )
    private val RESULT_PATTERN = Regex(
        """You get your mug filled with a delicious (.*?) Latte (.*?)\.</span>""",
        RegexOption.IGNORE_CASE,
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        REFILL_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { remaining ->
            preferences.setInt("_latteRefillsUsed", 3 - remaining)
        }
        val unlocks = mutableListOf<String>()
        for (lineMatch in LINE_PATTERN.findAll(html)) {
            val line = lineMatch.value
            val input = INPUT_PATTERN.find(line) ?: continue
            val description = input.groupValues[3].trim()
            val entry = LatteIngredients.byFirst(description) ?: continue
            if (!line.contains("&Dagger;") && !line.contains("†")) {
                unlocks += entry.ingredient
            }
        }
        preferences.setString("latteUnlocks", unlocks.joinToString(","))
        return true
    }

    /** Desktop [LatteRequest.parseFight] — unlock ingredient when discovery text appears in location. */
    fun applyFight(
        location: String?,
        html: String?,
        preferences: Preferences?,
        sessionLog: (String) -> Unit = {},
    ): Boolean {
        if (location.isNullOrBlank() || html.isNullOrBlank() || preferences == null) return false
        val entry = LatteIngredients.ALL.firstOrNull {
            it.location != null && it.location == location
        } ?: return false
        val discovery = entry.discovery ?: return false
        if (!html.contains(discovery)) return false
        val unlocks = preferences.getString("latteUnlocks", "")
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toMutableList()
        if (unlocks.any { it.equals(entry.ingredient, ignoreCase = true) }) return false
        unlocks += entry.ingredient
        preferences.setString("latteUnlocks", unlocks.joinToString(","))
        sessionLog("Unlocked ${entry.ingredient} for Latte.")
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        sessionLog: (String) -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (decision != 1 && !choiceUrl.contains("option=1")) return false
        if (decision != 1) return false

        val matcher = RESULT_PATTERN.find(html) ?: return false
        val start = matcher.groupValues[1].trim()
        val end = matcher.groupValues[2].trim()

        var first: LatteIngredients.Entry? = null
        var middle: String? = null
        for (latte in LatteIngredients.ALL) {
            if (start.startsWith(latte.first)) {
                first = latte
                middle = start.removePrefix(latte.first).trim()
                break
            }
        }
        var second: LatteIngredients.Entry? = null
        var third: LatteIngredients.Entry? = null
        val mid = middle.orEmpty()
        for (latte in LatteIngredients.ALL) {
            if (second == null && mid == latte.second) {
                second = latte
            }
            if (third == null && end == latte.third) {
                third = latte
            }
            if (second != null && third != null) break
        }
        if (first == null || second == null || third == null) return false

        setLatteEnchantments(arrayOf(first.modifier, second.modifier, third.modifier), preferences)
        val used = preferences.getInt("_latteRefillsUsed", 0)
        preferences.setInt("_latteRefillsUsed", (used + 1).coerceAtMost(3))
        preferences.setBoolean("_latteBanishUsed", false)
        preferences.setBoolean("_latteCopyUsed", false)
        preferences.setBoolean("_latteDrinkUsed", false)
        preferences.setString(
            "latteIngredients",
            "${first.ingredient},${second.ingredient},${third.ingredient}",
        )
        sessionLog("Filled your mug with ${first.first} ${second.second} Latte ${third.third}.")
        return true
    }

    /** Mark daily latte combat skills used (dailylimits Cast rows). */
    fun applySkillCast(skillId: Int, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        return when (skillId) {
            THROW_LATTE_SKILL -> {
                preferences.setBoolean("_latteBanishUsed", true)
                true
            }
            OFFER_LATTE_SKILL -> {
                preferences.setBoolean("_latteCopyUsed", true)
                true
            }
            GULP_LATTE_SKILL -> {
                preferences.setBoolean("_latteDrinkUsed", true)
                true
            }
            else -> false
        }
    }

    fun listUnlocks(all: Boolean, preferences: Preferences?): String {
        val unlocked = preferences?.getString("latteUnlocks", "").orEmpty()
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
        val lines = mutableListOf("Ingredient | Unlock | Modifier")
        for (latte in LatteIngredients.ALL) {
            val isUnlocked = latte.ingredient.lowercase() in unlocked
            if (!all && !isUnlocked) continue
            val unlock = if (isUnlocked) {
                "unlocked"
            } else {
                "Unlock in ${latte.location ?: "?"}"
            }
            lines += "${latte.ingredient} | $unlock | ${latte.modifier}"
        }
        return lines.joinToString("\n")
    }

    fun setLatteEnchantments(mods: Array<String>, preferences: Preferences) {
        val value = mods.filter { it.isNotBlank() }.joinToString(", ")
        preferences.setString("latteModifier", value)
        ModifierDatabase.updateItem(LATTE_MUG_ID, value)
    }
}
