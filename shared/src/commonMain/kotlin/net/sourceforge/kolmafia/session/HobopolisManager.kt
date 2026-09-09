package net.sourceforge.kolmafia.session

/**
 * Desktop [HobopolisDecorator.handleTownSquare] Hodgman boss-drop parse.
 *
 * When the clan Hobopolis Town Square boss is killed, the fight HTML contains
 * "WINWINWIN" and a flavour sentence that identifies which of the six hobo
 * boss items Richard collects.
 */
object HobopolisManager {

    private val BOSS_DROPS: List<Pair<String, String>> = listOf(
        "wrinkly heap on the ground" to "hobo skin",
        "smoking pair of boots" to "charred hobo boots",
        "pair of frozen eyeballs" to "frozen hobo eyeballs",
        "pile of foul-smelling guts" to "stinking hobo guts",
        "he left his skull behind" to "creepy hobo skull",
        "he ran off without his crotch" to "hobo crotch",
    )

    /**
     * Parses Hodgman Town Square boss-kill from fight HTML.
     *
     * @return the item name if a boss drop was detected, `null` otherwise.
     */
    fun parseTownSquareWin(html: String): String? {
        if (!html.contains("WINWINWIN")) return null
        for ((snippet, item) in BOSS_DROPS) {
            if (html.contains(snippet)) return item
        }
        return null
    }
}
