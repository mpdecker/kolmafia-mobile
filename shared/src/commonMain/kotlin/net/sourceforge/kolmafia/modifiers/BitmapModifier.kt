package net.sourceforge.kolmafia.modifiers

enum class BitmapModifier(val tag: String) {
    BRIMSTONE("Brimstone"),
    CLOATHING("Cloathing"),
    SYNERGETIC("Synergetic"),
    SURGEONOSITY("Surgeonosity"),
    CLOWNINESS("Clowniness"),
    RAVEOSITY("Raveosity"),
    MCHUGELARGE("McHugeLarge"),
    STINKYCHEESE("Stinky Cheese"),
    MUTEX("Mutually Exclusive"),
    MUTEX_VIOLATIONS("Mutex Violations");

    companion object {
        private val byTagLower: Map<String, BitmapModifier> =
            entries.associateBy { it.tag.lowercase() }

        fun byTag(tag: String): BitmapModifier? = byTagLower[tag.lowercase()]
    }
}
