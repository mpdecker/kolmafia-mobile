package net.sourceforge.kolmafia.session

/**
 * Desktop [EncounterManager.EncounterType] — special encounter classifications from encounters.txt.
 */
enum class EncounterType(val isAutostop: Boolean = false) {
    NONE,
    STOP(true),
    LUCKY,
    GLYPH(true),
    TURTLE,
    SEAL,
    FIST,
    BORIS(true),
    BADMOON(true),
    BUGBEAR,
    MAYO,
    CLEAVER,
    HALLOWIENER,
    VIOLET_FOG,
    BAT_WINGS,
    WANDERER,
    SUPERLIKELY,
    ULTRARARE,
    FREE_COMBAT,
    NOWANDER,
    ;

    companion object {
        fun fromToken(raw: String): EncounterType {
            val key = raw.trim().uppercase()
            return entries.firstOrNull { it.name == key } ?: NONE
        }
    }
}
