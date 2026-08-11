package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.math.max

/** Desktop [QuestManager.updateQuestData] oil/boo peak monster combat pref hooks. */
object ToppingPeakCombatSync {

    const val DRESS_PANTS_ID = 6030

    private const val DRESS_PANTS_BONUS = 6.34f
    private const val LOVEBUG_BONUS = 6.34f

    private val OIL_MONSTER_PROGRESS = mapOf(
        "oil slick" to 6.34f,
        "oil tycoon" to 19.02f,
        "oil baron" to 31.7f,
        "oil cartel" to 63.4f,
    )

    private val BOO_GHOST_MONSTERS = setOf(
        "battlie knight ghost",
        "claybender sorcerer ghost",
        "dusken raider ghost",
        "space tourist explorer ghost",
        "whatsian commando ghost",
    )

    fun applyCombatWin(
        preferences: Preferences?,
        monster: String,
        responseText: String,
        won: Boolean,
        hasItemEquipped: (Int) -> Boolean = { false },
    ): Boolean {
        if (preferences == null || !won || monster.isBlank()) return false
        var changed = applyOilMonsterWin(preferences, monster, responseText, hasItemEquipped)
        changed = applyBooGhostWin(preferences, monster) || changed
        return changed
    }

    fun applyOilMonsterWin(
        preferences: Preferences,
        monster: String,
        responseText: String,
        hasItemEquipped: (Int) -> Boolean,
    ): Boolean {
        val decrement = OIL_MONSTER_PROGRESS[monster.trim().lowercase()] ?: return false
        var total = decrement
        if (hasItemEquipped(DRESS_PANTS_ID)) {
            total += DRESS_PANTS_BONUS
        }
        if (responseText.contains("love oil beetle trundles up", ignoreCase = true)) {
            total += LOVEBUG_BONUS
        }
        val current = preferences.getString("oilPeakProgress", "0").toFloatOrNull() ?: 0f
        val next = max(0f, current - total)
        preferences.setString("oilPeakProgress", formatOilProgress(next))
        return true
    }

    fun applyBooGhostWin(
        preferences: Preferences,
        monster: String,
    ): Boolean {
        if (monster.trim().lowercase() !in BOO_GHOST_MONSTERS) return false
        val current = preferences.getInt("booPeakProgress", 0)
        preferences.setInt("booPeakProgress", max(0, current - 2))
        return true
    }

    private fun formatOilProgress(value: Float): String {
        val scaled = (value * 100f).toInt()
        val whole = scaled / 100
        val fraction = (scaled % 100).let { if (it < 0) -it else it }
        return "$whole.${fraction.toString().padStart(2, '0')}"
    }
}
