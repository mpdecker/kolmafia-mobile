package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.data.ConcoctionBuyables
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Slimeling stack / fullness accounting used by `slime-stack` CLI and fight/feed hooks. */
object SlimeStackManager {
    const val STACKS_DROPPED_PREF = "slimelingStacksDropped"
    const val STACKS_DUE_PREF = "slimelingStacksDue"
    const val FULLNESS_PREF = "slimelingFullness"
    const val SLIMELING_FAMILIAR_ID = 112

    fun getSlimeStackTurns(n: Int): Int = n * (n + 1) / 2

    fun isMeatStackFeed(itemId: Int): Boolean =
        itemId == ItemPool.GNOLLISH_AUTOPLUNGER ||
            itemId == ConcoctionBuyables.MEAT_PASTE ||
            itemId == ConcoctionBuyables.MEAT_STACK ||
            itemId == ConcoctionBuyables.DENSE_MEAT_STACK

    fun status(preferences: Preferences): String {
        val got = preferences.getInt(STACKS_DROPPED_PREF, 0)
        val due = preferences.getInt(STACKS_DUE_PREF, 0)
        return when {
            due <= 0 ->
                "No slime stacks due. Feed your Slimeling with basic meat equipment or Gnollish autoplungers to receive slime stacks."
            got >= due ->
                "Got all $due expected slime stacks this ascension. Feed your Slimeling with basic meat equipment or Gnollish autoplungers to receive more."
            else -> {
                val missing = due - got
                val next = got + 1
                "$missing slime stacks queued. Next: #$next (expected after " +
                    "${getSlimeStackTurns(next)} total Slimeling combats)."
            }
        }
    }

    /** Desktop UseItemRequest Slimeling binge: autoplunger/meat-stack → due; else power/10 fullness. */
    fun recordFeed(itemId: Int, count: Int, preferences: Preferences?) {
        val prefs = preferences ?: return
        if (count <= 0) return
        if (isMeatStackFeed(itemId)) {
            prefs.setInt(STACKS_DUE_PREF, prefs.getInt(STACKS_DUE_PREF, 0) + count)
        } else {
            val charges = count * EquipmentDatabase.getPower(itemId) / 10.0f
            prefs.setFloat(FULLNESS_PREF, prefs.getFloat(FULLNESS_PREF, 0f) + charges)
        }
    }

    /** Desktop FightRequest: "[slimeling] leaps on your opponent..." fullness −1. */
    fun recordFightLeaps(html: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        if (!html.contains("leaps on your opponent")) return false
        val fullness = prefs.getFloat(FULLNESS_PREF, 0f)
        prefs.setFloat(FULLNESS_PREF, maxOf(fullness - 1.0f, 0f))
        return true
    }

    /** Desktop ResultProcessor SLIME_STACK drop while Slimeling is current familiar. */
    fun recordStackDrop(preferences: Preferences?, currentFamiliarId: Int): Boolean {
        val prefs = preferences ?: return false
        if (currentFamiliarId != SLIMELING_FAMILIAR_ID) return false
        val dropped = prefs.getInt(STACKS_DROPPED_PREF, 0) + 1
        prefs.setInt(STACKS_DROPPED_PREF, dropped)
        if (dropped > prefs.getInt(STACKS_DUE_PREF, 0)) {
            prefs.setInt(STACKS_DUE_PREF, dropped)
        }
        return true
    }
}
