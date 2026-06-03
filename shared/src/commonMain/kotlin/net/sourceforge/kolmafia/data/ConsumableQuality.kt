package net.sourceforge.kolmafia.data

enum class ConsumableQuality { CRAPPY, DECENT, GOOD, AWESOME, EPIC, SUPER_EPIC, SUPER_MEGA_EPIC, UNKNOWN;
    companion object {
        fun fromString(s: String): ConsumableQuality = when (s.trim().lowercase()) {
            "crappy" -> CRAPPY
            "decent" -> DECENT
            "good" -> GOOD
            "awesome" -> AWESOME
            "epic" -> EPIC
            "super ultra epic" -> SUPER_EPIC
            "super ultra mega turbo epic" -> SUPER_MEGA_EPIC
            else -> UNKNOWN
        }
    }
}
