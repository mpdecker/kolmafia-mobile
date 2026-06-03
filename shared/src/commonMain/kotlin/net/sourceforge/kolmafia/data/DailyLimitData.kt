package net.sourceforge.kolmafia.data

data class DailyLimitData(
    val type: String,              // "Cast", "Use", "Buy", "Free"
    val name: String,
    val trackingProperty: String,  // preference key
    val maxValue: Int              // -1 = determined by property/game
)
