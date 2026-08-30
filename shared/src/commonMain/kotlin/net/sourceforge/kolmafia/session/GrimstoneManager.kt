package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [GrimstoneManager] — grimstone mask psychoses hub (choices 822–842, 829)
 * plus fight-turn counters (Phases 3201–3215). Gnome path delegates to [RumpleManager].
 *
 * Not [GrimChoiceSync] / Barely Tales choice 835.
 */
object GrimstoneManager {

    const val MASK_CHOICE = 829

    private val STEPMOTHER_CHOICES = setOf(822, 823, 824, 825, 826, 827)
    private val WOLF_CHOICES = setOf(830, 832, 833, 834)
    private val WITCH_CHOICES = setOf(831, 837, 838, 839, 840, 841, 842)
    private val GNOME_CHOICES = setOf(844, 845, 846, 847, 848, 849, 850)

    private val CINDERELLA_SCORE = Regex("""score (?:is now|was) <b>(\d+)</b>""")
    private val FINAL_CANDY = Regex("""Your final candy total is: <b>(\d+)!</b>""")
    private val LOSE_CANDY = Regex("""<b>-(\d+) Candy</b>""")
    private val CINDERELLA_TIME = Regex("""<i>It is (\d+) minute(?:s)? to midnight\.</i>""")

    /** Desktop AdventurePool IDs for grimstone psychoses. */
    private val GRIMSTONE_SNARFBLATS = setOf(
        369, 370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380,
    )

    fun isGrimstoneAdventure(adventureId: Int?, adventureSource: String? = null): Boolean {
        if (adventureId != null && adventureId in GRIMSTONE_SNARFBLATS) return true
        if (adventureSource?.contains("ioty2014_wolf", ignoreCase = true) == true) return true
        return false
    }

    fun incrementFights(adventureId: Int, preferences: Preferences?) {
        preferences ?: return
        when (adventureId) {
            380 -> preferences.increment("rumpelstiltskinTurnsUsed", 1)
            369 -> preferences.increment("wolfTurnsUsed", 1)
            373 -> preferences.increment("hareTurnsUsed", 1)
            370, 371, 372 -> preferences.increment("candyWitchTurnsUsed", 1)
        }
    }

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        preferences ?: return false
        return when (choiceId) {
            in STEPMOTHER_CHOICES -> {
                preferences.setString("grimstoneMaskPath", "stepmother")
                parseCinderellaTime(html, preferences)
                true
            }
            MASK_CHOICE -> true
            in WOLF_CHOICES -> {
                preferences.setString("grimstoneMaskPath", "wolf")
                true
            }
            in WITCH_CHOICES -> {
                preferences.setString("grimstoneMaskPath", "witch")
                true
            }
            in GNOME_CHOICES -> {
                preferences.setString("grimstoneMaskPath", "gnome")
                RumpleManager.visitChoice(choiceId, html, preferences)
                true
            }
            else -> false
        }
    }

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        decision: Int = 0,
        inventory: InventoryManager? = null,
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        preferences ?: return false
        return when (choiceId) {
            in STEPMOTHER_CHOICES -> {
                if (!parseCinderellaTime(html, preferences)) {
                    preferences.decrement("cinderellaMinutesToMidnight", 1)
                }
                CINDERELLA_SCORE.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                    preferences.setInt("cinderellaScore", it)
                }
                if (html.contains("Your final score was")) {
                    if (html.contains("reduced to <b>0</b> due to failure")) {
                        preferences.setInt("cinderellaScore", 0)
                    }
                    preferences.setInt("cinderellaMinutesToMidnight", 0)
                    preferences.setString("grimstoneMaskPath", "")
                }
                true
            }
            MASK_CHOICE -> {
                if (decision != 6) {
                    ResultProcessor.processItem(ItemPool.GRIMSTONE_MASK, -1, inventory = inventory)
                    preferences.setInt("cinderellaMinutesToMidnight", 0)
                    RumpleManager.reset(decision, inventory, preferences)
                }
                when (decision) {
                    1 -> {
                        preferences.setInt("cinderellaMinutesToMidnight", 30)
                        preferences.setInt("cinderellaScore", 0)
                        preferences.setString("grimstoneMaskPath", "stepmother")
                        preferences.setBoolean("grimstoneAvailable", true)
                    }
                    2 -> {
                        preferences.setInt("wolfPigsEvicted", 0)
                        preferences.setInt("wolfTurnsUsed", 0)
                        preferences.setString("grimstoneMaskPath", "wolf")
                        preferences.setBoolean("grimstoneAvailable", true)
                    }
                    3 -> {
                        preferences.setInt("candyWitchCandyTotal", 0)
                        preferences.setInt("candyWitchTurnsUsed", 0)
                        preferences.setString("grimstoneMaskPath", "witch")
                        preferences.setBoolean("grimstoneAvailable", true)
                    }
                    4 -> {
                        preferences.setString("grimstoneMaskPath", "gnome")
                        preferences.setBoolean("grimstoneAvailable", true)
                    }
                    5 -> {
                        preferences.setInt("hareMillisecondsSaved", 0)
                        preferences.setInt("hareTurnsUsed", 0)
                        preferences.setString("grimstoneMaskPath", "hare")
                        preferences.setBoolean("grimstoneAvailable", true)
                    }
                }
                true
            }
            830 -> true
            in setOf(832, 833, 834) -> {
                preferences.increment("wolfTurnsUsed", 1)
                true
            }
            831 -> {
                if (decision == 1) {
                    LOSE_CANDY.findAll(html).forEach { match ->
                        val candy = match.groupValues[1].toIntOrNull() ?: return@forEach
                        preferences.decrement("candyWitchCandyTotal", candy)
                    }
                    if (html.contains("Your final candy total is")) {
                        FINAL_CANDY.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                            preferences.setInt("candyWitchCandyTotal", it)
                        }
                        preferences.setString("grimstoneMaskPath", "")
                    }
                }
                true
            }
            in setOf(837, 838, 839, 840, 841, 842) -> {
                preferences.increment("candyWitchTurnsUsed", 1)
                true
            }
            in GNOME_CHOICES -> {
                RumpleManager.postChoice(choiceId, decision, html, preferences, inventory, sessionLogger)
                true
            }
            else -> false
        }
    }

    fun zoneGateOpen(preferences: Preferences?): Boolean {
        preferences ?: return false
        return preferences.getBoolean("grimstoneAvailable", false) ||
            preferences.getString("grimstoneMaskPath", "").isNotBlank() ||
            preferences.getString("grimstoneZone", "").isNotBlank()
    }

    private fun parseCinderellaTime(html: String, preferences: Preferences): Boolean {
        CINDERELLA_TIME.findAll(html).forEach { match ->
            val time = match.groupValues[1].toIntOrNull() ?: return@forEach
            preferences.setInt("cinderellaMinutesToMidnight", time)
            return true
        }
        return false
    }

    private fun Preferences.increment(key: String, amount: Int) {
        setInt(key, getInt(key, 0) + amount)
    }

    private fun Preferences.decrement(key: String, amount: Int) {
        setInt(key, (getInt(key, 0) - amount).coerceAtLeast(0))
    }
}
