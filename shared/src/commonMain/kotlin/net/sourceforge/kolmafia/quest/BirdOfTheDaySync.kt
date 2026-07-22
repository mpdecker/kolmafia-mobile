package net.sourceforge.kolmafia.quest

import kotlin.math.ln
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DynamicItemModifierSync.CheckContext
import net.sourceforge.kolmafia.quest.DynamicItemModifierSync.DescVisit

/** Desktop [InventoryManager.checkBirdOfTheDay]. */
object BirdOfTheDaySync {

    const val SEEK_OUT_A_BIRD_SKILL_ID = 7323
    private const val CALENDAR_ITEM_NAME = "Bird-a-Day calendar"
    private val BIRD_PATTERN = Regex("Seek out an? (.*)")
    private val NAME_PATTERN = Regex("<b>(.*?)</b>")
    private val MP_COST_PATTERN = Regex("<b>MP Cost:</b> (\\d+)")

    private val BIRD_EFFECT_NAMES = listOf(
        "Blessing of your favorite Bird",
        "Blessing of the Bird",
    )

    fun parseSkillName(html: String): String {
        val match = NAME_PATTERN.find(html) ?: return ""
        return match.groupValues[1].trim()
    }

    fun parseSkillMpCost(html: String): Long {
        val match = MP_COST_PATTERN.find(html) ?: return 0
        return match.groupValues[1].toLongOrNull() ?: 0
    }

    fun applySeekBirdSkillDescription(html: String, preferences: Preferences) {
        val skillName = parseSkillName(html)
        val birdMatcher = BIRD_PATTERN.find(skillName) ?: return
        val bird = birdMatcher.groupValues[1]
        preferences.setString("_birdOfTheDay", bird)
        if (!preferences.getBoolean("_canSeekBirds", false)) {
            preferences.setBoolean("_canSeekBirds", true)
        }
        val mp = parseSkillMpCost(html)
        if (mp > 0) {
            val casts = (ln(mp / 5.0) / ln(2.0)).toInt()
            preferences.setInt("_birdsSoughtToday", casts)
        }
    }

    fun checkBirdOfTheDay(
        context: CheckContext,
        gameDatabase: GameDatabase,
    ): List<DescVisit> {
        val calendar = gameDatabase.item(CALENDAR_ITEM_NAME) ?: return emptyList()
        if (!DynamicItemModifierSync.isAccessible(calendar.id, calendar.name, context)) {
            return emptyList()
        }
        val visits = mutableListOf<DescVisit>(DescVisit.Skill(SEEK_OUT_A_BIRD_SKILL_ID))
        for (effectName in BIRD_EFFECT_NAMES) {
            val effect = EffectDatabase.getByName(effectName) ?: continue
            if (effect.descId.isNotEmpty()) {
                visits.add(DescVisit.Effect(effect.descId))
            }
        }
        return visits
    }
}
