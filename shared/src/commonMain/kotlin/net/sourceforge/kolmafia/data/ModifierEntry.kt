package net.sourceforge.kolmafia.data

data class ModifierEntry(
    val entityType: String,     // "Item", "Effect", "Skill", "Outfit", "Familiar", etc.
    val name: String,
    val modifiers: String       // raw modifier string, e.g. "Muscle Percent: +15, Initiative: +30"
)
