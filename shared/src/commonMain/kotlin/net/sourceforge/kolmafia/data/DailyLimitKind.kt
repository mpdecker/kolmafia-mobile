package net.sourceforge.kolmafia.data

enum class DailyLimitKind(val tag: String) {
    USE("use"),
    EAT("eat"),
    DRINK("drink"),
    CHEW("chew"),
    ;

    companion object {
        fun fromTag(tag: String): DailyLimitKind? =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
    }
}

data class DailyLimitEntry(
    val kind: DailyLimitKind,
    val itemId: Int,
    val trackingProperty: String,
    val maxValue: Int,
)
