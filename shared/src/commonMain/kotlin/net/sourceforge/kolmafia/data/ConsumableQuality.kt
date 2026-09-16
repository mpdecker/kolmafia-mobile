package net.sourceforge.kolmafia.data

enum class ConsumableQuality { NONE, CRAPPY, DECENT, GOOD, AWESOME, EPIC, SUPER_EPIC, SUPER_MEGA_EPIC, SUPER_ULTRA_EPIC, SUPER_ULTRA_MEGA_EPIC, SUPER_ULTRA_MEGA_TURBO_EPIC, UNKNOWN;

    fun displayName(): String = when (this) {
        NONE -> ""
        CRAPPY -> "crappy"
        DECENT -> "decent"
        GOOD -> "good"
        AWESOME -> "awesome"
        EPIC -> "epic"
        SUPER_EPIC -> "super ultra epic"
        SUPER_MEGA_EPIC -> "super ultra mega turbo epic"
        SUPER_ULTRA_EPIC -> "super ultra EPIC"
        SUPER_ULTRA_MEGA_EPIC -> "super ultra mega EPIC"
        SUPER_ULTRA_MEGA_TURBO_EPIC -> "super ultra mega turbo EPIC"
        UNKNOWN -> ""
    }

    /** Desktop [ConsumableQuality.toString] dump name used in TCRS rows. */
    fun tcrsDumpName(): String = when (this) {
        NONE -> ""
        CRAPPY -> "crappy"
        DECENT -> "decent"
        GOOD -> "good"
        AWESOME -> "awesome"
        EPIC -> "EPIC"
        SUPER_EPIC -> "super EPIC"
        SUPER_MEGA_EPIC -> "super ultra mega turbo EPIC"
        SUPER_ULTRA_EPIC -> "super ultra EPIC"
        SUPER_ULTRA_MEGA_EPIC -> "super ultra mega EPIC"
        SUPER_ULTRA_MEGA_TURBO_EPIC -> "super ultra mega turbo EPIC"
        UNKNOWN -> ""
    }

    companion object {
        fun fromEnumName(name: String): ConsumableQuality? =
            entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

        fun fromString(s: String): ConsumableQuality = when (s.trim().lowercase()) {
            "crappy" -> CRAPPY
            "decent" -> DECENT
            "good" -> GOOD
            "awesome" -> AWESOME
            "epic" -> EPIC
            "super epic" -> SUPER_EPIC
            "super ultra epic" -> SUPER_ULTRA_EPIC
            "super ultra mega epic" -> SUPER_ULTRA_MEGA_EPIC
            "super ultra mega turbo epic" -> SUPER_ULTRA_MEGA_TURBO_EPIC
            else -> UNKNOWN
        }
    }
}
