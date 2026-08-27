package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [FightRequest.payActionCost] / [FightRequest.payItemCost] practical subset
 * (Phases 1251–1265): MP skill costs, class-resource HTML gains, combat-item consume,
 * and fight-flag prefs (`jiggledChefstaff`, chaos butterfly, cosmic bowling ball).
 */
object FightActionCostSync {

    const val CHAOS_BUTTERFLY = 615
    const val COSMIC_BOWLING_BALL = 10891

    /** Desktop [FightRequest.nextAction] — set before a round when known. */
    var nextAction: String = ""

    /** Desktop [FightRequest.jiggledChefstaff]. */
    var jiggledChefstaff: Boolean = false

    private val SOULSAUCE = Regex("""You absorb (\d+) Soulsauce""")
    private val THUNDER = Regex("""swallow <b>(\d+)</b> dB of it""")
    private val RAIN = Regex("""recovering <b>(\d+)</b> drops""")
    private val LIGHTNING = Regex("""recovering <b>(\d+)</b> bolts""")

    fun reset() {
        nextAction = ""
        jiggledChefstaff = false
    }

    fun alreadyJiggled(): Boolean = jiggledChefstaff

    /**
     * Apply action costs for [nextAction] against [html], plus HTML resource gains.
     * @return true if any session state mutated
     */
    fun payActionCost(
        html: String,
        character: KoLCharacter? = null,
        inventory: InventoryManager? = null,
        preferences: Preferences? = null,
        action: String = nextAction,
    ): Boolean {
        var changed = false
        changed = applyResourceGains(html, character) || changed

        val act = action.trim()
        if (act.isEmpty()) return changed

        when {
            act == "attack" || act == "runaway" || act == "abort" || act == "steal" ->
                return changed

            act == "jiggle" -> {
                jiggledChefstaff = true
                changed = true
                return changed
            }

            act.startsWith("skill") -> {
                if (html.contains("You don't have that skill")) return changed
                val skillId = act.removePrefix("skill").toIntOrNull() ?: return changed
                changed = paySkillCost(skillId, character) || changed
            }

            else -> {
                // Item use: "1234" or "1234,5678" funksling
                if (html.contains("You are too scared of Bs")) return changed
                val parts = act.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it > 0 }
                parts.forEachIndexed { index, itemId ->
                    val other = parts.getOrNull(1 - index) ?: -1
                    changed = payItemCost(itemId, other, html, inventory, preferences) || changed
                }
            }
        }
        return changed
    }

    fun paySkillCost(skillId: Int, character: KoLCharacter?): Boolean {
        if (skillId <= 0 || character == null) return false
        val mpCost = SkillDefinitionDatabase.getById(skillId)?.mpCost ?: 0
        if (mpCost <= 0) return false
        val state = character.state.value
        val next = (state.currentMp - mpCost).coerceAtLeast(0)
        if (next == state.currentMp) return false
        character.updateHpMp(state.currentHp, state.maxHp, next, state.maxMp)
        return true
    }

    fun payItemCost(
        itemId: Int,
        itemId2: Int = -1,
        html: String,
        inventory: InventoryManager? = null,
        preferences: Preferences? = null,
    ): Boolean {
        if (itemId <= 0) return false
        var changed = false
        when (itemId) {
            CHAOS_BUTTERFLY -> {
                if (html.contains("reality is altered in unpredictable ways") ||
                    isItemSuccess(html)
                ) {
                    preferences?.setBoolean("chaosButterflyThrown", true)
                    changed = true
                }
            }
            COSMIC_BOWLING_BALL -> {
                preferences?.setInt("cosmicBowlingBallReturnCombats", 0)
                if (html.contains("you hurl it down the ancient lanes")) {
                    preferences?.setInt(
                        "hiddenBowlingAlleyProgress",
                        (preferences.getInt("hiddenBowlingAlleyProgress", 0) + 1),
                    )
                }
                changed = true
            }
        }
        if (isItemConsumed(itemId, html) && inventory != null) {
            inventory.consumeItemLocally(itemId, 1)
            changed = true
        }
        return changed
    }

    fun applyResourceGains(html: String, character: KoLCharacter?): Boolean {
        if (character == null || html.isBlank()) return false
        var changed = false
        SOULSAUCE.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { gain ->
            val cur = character.state.value.soulsauce
            character.updateClassResource(soulsauce = cur + gain)
            changed = true
        }
        val thunderGain = THUNDER.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val rainGain = RAIN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val lightningGain = LIGHTNING.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        if (thunderGain > 0 || rainGain > 0 || lightningGain > 0) {
            val s = character.state.value
            character.updatePlumberResources(
                thunder = s.thunder + thunderGain,
                rain = s.rain + rainGain,
                lightning = s.lightning + lightningGain,
            )
            changed = true
        }
        return changed
    }

    private fun isItemSuccess(html: String): Boolean =
        html.contains("You use") ||
            html.contains("You throw") ||
            html.contains("You slap") ||
            html.contains("You hurl")

    /**
     * Desktop [FightRequest.isItemConsumed] practical default: consume unless the
     * server rejected the use with a known failure phrase.
     */
    fun isItemConsumed(itemId: Int, html: String): Boolean {
        if (itemId <= 0) return false
        if (html.contains("You don't have that item") ||
            html.contains("You can't use that item") ||
            html.contains("You are too scared of Bs")
        ) {
            return false
        }
        // Reusable combat items that should not be consumed on use
        if (itemId == COSMIC_BOWLING_BALL) return false
        return true
    }
}
