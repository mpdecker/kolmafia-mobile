package net.sourceforge.kolmafia.character

// Matches the Gender enum in the desktop KoLCharacter.java.
// Modifier value mirrors the desktop: male = -1, female = +1 (used in modifier expressions).
enum class Gender(val modifierValue: Int, val apiString: String) {
    UNKNOWN(0, ""),
    MALE(-1,   "male"),
    FEMALE(1,  "female");

    companion object {
        fun fromApiString(s: String): Gender = when (s.lowercase().trim()) {
            "male",   "1"  -> MALE
            "female", "2"  -> FEMALE
            else            -> UNKNOWN
        }
    }
}
