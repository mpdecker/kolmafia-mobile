package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.modifiers.DoubleModifier

/** Parses desktop-style maximize goal strings into modifier tags. */
object MaximizeGoal {

    fun parse(goal: String): DoubleModifier? {
        val normalized = goal.trim().lowercase()
        return when (normalized) {
            "all" -> DoubleModifier.MUS
            "mus", "muscle", "muscularity" -> DoubleModifier.MUS
            "mys", "myst", "mysticality" -> DoubleModifier.MYS
            "mox", "moxie" -> DoubleModifier.MOX
            "hp", "hit points", "maxhp" -> DoubleModifier.HP
            "mp", "mana", "maxmp" -> DoubleModifier.MP
            "init", "initiative" -> DoubleModifier.INITIATIVE
            "meat", "meat drop" -> DoubleModifier.MEATDROP
            "item", "item drop" -> DoubleModifier.ITEMDROP
            "exp", "experience" -> DoubleModifier.EXPERIENCE
            "abs", "absorb" -> DoubleModifier.ABSORB_STAT
            else -> DoubleModifier.byTag(goal.trim())
        }
    }
}
