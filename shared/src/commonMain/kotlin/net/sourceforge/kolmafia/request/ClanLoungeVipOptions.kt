package net.sourceforge.kolmafia.request

/** Desktop ClanLoungeRequest shower / swimming option tables. */
object ClanLoungeVipOptions {

    data class ShowerOption(val temp: String, val effect: String, val index: Int)
    data class SwimmingOption(val action: String, val effect: String, val index: Int)

    const val COLD_SHOWER = 1
    const val COOL_SHOWER = 2
    const val LUKEWARM_SHOWER = 3
    const val WARM_SHOWER = 4
    const val HOT_SHOWER = 5

    const val CANNONBALL = 1
    const val LAPS = 2
    const val SPRINTS = 3

    val SHOWER_OPTIONS: List<ShowerOption> = listOf(
        ShowerOption("cold", "ice", COLD_SHOWER),
        ShowerOption("cool", "moxie", COOL_SHOWER),
        ShowerOption("lukewarm", "mysticality", LUKEWARM_SHOWER),
        ShowerOption("warm", "muscle", WARM_SHOWER),
        ShowerOption("hot", "mp", HOT_SHOWER),
    )

    val SWIMMING_OPTIONS: List<SwimmingOption> = listOf(
        SwimmingOption("cannonball", "item", CANNONBALL),
        SwimmingOption("laps", "ml", LAPS),
        SwimmingOption("sprints", "noncombat", SPRINTS),
    )

    fun findShowerOption(tag: String): Int {
        val normalized = tag.trim().lowercase()
        if (normalized.isEmpty()) return 0
        for (option in SHOWER_OPTIONS) {
            if (option.temp.startsWith(normalized) || option.effect.startsWith(normalized)) {
                return option.index
            }
        }
        return 0
    }

    fun findSwimmingOption(tag: String): Int {
        val normalized = tag.trim().lowercase()
        if (normalized.isEmpty()) return 0
        for (option in SWIMMING_OPTIONS) {
            if (option.action.startsWith(normalized) || option.effect.startsWith(normalized)) {
                return option.index
            }
        }
        return 0
    }

    fun swimmingSubaction(option: Int): String? = when (option) {
        CANNONBALL -> "screwaround"
        LAPS -> "laps"
        SPRINTS -> "submarine"
        else -> null
    }
}
