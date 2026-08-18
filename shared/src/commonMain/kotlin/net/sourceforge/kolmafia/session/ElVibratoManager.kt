package net.sourceforge.kolmafia.session

/**
 * Desktop [ElVibratoManager] punchcard table + whichcard consume.
 * Fight HTML decorate is a relay-only non-goal.
 */
object ElVibratoManager {

    data class Punchcard(val id: Int, val name: String, val alias: String, val tag: String)

    data class Command(
        val card1: Punchcard,
        val card2: Punchcard,
        val objectItemId: Int?,
        val desc: String,
    )

    val ATTACK = Punchcard(3146, "El Vibrato punchcard (115 holes)", "El Vibrato punchcard (ATTACK)", "ATTACK")
    val REPAIR = Punchcard(3147, "El Vibrato punchcard (97 holes)", "El Vibrato punchcard (REPAIR)", "REPAIR")
    val BUFF = Punchcard(3148, "El Vibrato punchcard (129 holes)", "El Vibrato punchcard (BUFF)", "BUFF")
    val MODIFY = Punchcard(3149, "El Vibrato punchcard (213 holes)", "El Vibrato punchcard (MODIFY)", "MODIFY")
    val BUILD = Punchcard(3150, "El Vibrato punchcard (165 holes)", "El Vibrato punchcard (BUILD)", "BUILD")
    val TARGET = Punchcard(3151, "El Vibrato punchcard (142 holes)", "El Vibrato punchcard (TARGET)", "TARGET")
    val SELF = Punchcard(3152, "El Vibrato punchcard (216 holes)", "El Vibrato punchcard (SELF)", "SELF")
    val FLOOR = Punchcard(3153, "El Vibrato punchcard (88 holes)", "El Vibrato punchcard (FLOOR)", "FLOOR")
    val DRONE = Punchcard(3154, "El Vibrato punchcard (182 holes)", "El Vibrato punchcard (DRONE)", "DRONE")
    val WALL = Punchcard(3155, "El Vibrato punchcard (176 holes)", "El Vibrato punchcard (WALL)", "WALL")
    val SPHERE = Punchcard(3156, "El Vibrato punchcard (104 holes)", "El Vibrato punchcard (SPHERE)", "SPHERE")

    val PUNCHCARDS = listOf(
        ATTACK, REPAIR, BUFF, MODIFY, BUILD,
        TARGET, SELF, FLOOR, DRONE, WALL, SPHERE,
    )

    private val allPunchcardIds = PUNCHCARDS.map { it.id }.toSet()

    val CARD_EXCHANGES = mapOf(
        ATTACK.id to TARGET.id,
        TARGET.id to ATTACK.id,
        REPAIR.id to SELF.id,
        SELF.id to REPAIR.id,
        FLOOR.id to BUFF.id,
        BUFF.id to FLOOR.id,
        DRONE.id to MODIFY.id,
        MODIFY.id to DRONE.id,
        BUILD.id to WALL.id,
        WALL.id to BUILD.id,
    )

    const val POWER_SPHERE = 3049
    const val EV_DRONE = 3157
    const val BROKEN_DRONE = 3165
    const val REPAIRED_DRONE = 3166
    const val AUGMENTED_DRONE = 3167

    enum class Construct(val type: String) {
        BIZARRE("bizarre"),
        HULKING("hulking"),
        INDUSTRIOUS("industrious"),
        LONELY("lonely"),
        MENACING("menacing"),
        TOWERING("towering"),
    }

    private val whichcardPattern = Regex("""whichcard=(\d+)""", RegexOption.IGNORE_CASE)

    private val commandsByConstruct: Map<Construct, List<Command>> = mapOf(
        Construct.BIZARRE to listOf(
            Command(BUFF, DRONE, REPAIRED_DRONE, "-> augmented drone"),
            Command(BUFF, TARGET, null, "10 turns of Fitter, Happier"),
            Command(BUFF, SELF, null, "augments construct"),
            Command(REPAIR, TARGET, null, "heals you"),
            Command(REPAIR, SELF, null, "heals construct"),
        ),
        Construct.HULKING to listOf(
            Command(ATTACK, FLOOR, null, "get punchcards"),
            Command(ATTACK, WALL, null, "get punchcards (can include SPHERE)"),
            Command(ATTACK, SELF, null, "destroys construct"),
            Command(ATTACK, TARGET, null, "damages you construct"),
            Command(BUILD, SELF, null, "augments construct"),
        ),
        Construct.INDUSTRIOUS to listOf(
            Command(BUFF, FLOOR, null, "no effect"),
            Command(BUFF, WALL, null, "no effect"),
            Command(BUFF, TARGET, null, "damages you"),
            Command(BUFF, SELF, null, "damages construct"),
        ),
        Construct.LONELY to listOf(
            Command(MODIFY, SPHERE, POWER_SPHERE, "-> overcharged power sphere"),
            Command(REPAIR, DRONE, BROKEN_DRONE, "-> repaired drone"),
            Command(REPAIR, SELF, null, "manipulates construct"),
            Command(REPAIR, TARGET, null, "damages you"),
        ),
        Construct.MENACING to listOf(
            Command(ATTACK, WALL, null, "no effect"),
            Command(ATTACK, FLOOR, null, "no effect"),
            Command(ATTACK, SELF, null, "damages construct"),
            Command(ATTACK, TARGET, null, "damages you"),
        ),
        Construct.TOWERING to listOf(
            Command(BUILD, DRONE, null, "get El Vibrato drone"),
            Command(BUILD, TARGET, null, "no effect"),
            Command(MODIFY, SELF, null, "transform into a new construct"),
            Command(MODIFY, DRONE, EV_DRONE, "-> broken drone"),
            Command(MODIFY, SPHERE, POWER_SPHERE, "-> El Vibrato outfit item"),
        ),
    )

    fun commandsFor(construct: Construct): List<Command> =
        commandsByConstruct[construct].orEmpty()

    fun extractCardId(url: String): Int? {
        val id = whichcardPattern.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        return id.takeIf { it in allPunchcardIds }
    }

    fun parseResponse(
        url: String?,
        html: String = "",
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        val cardId = extractCardId(url.orEmpty()) ?: return false
        consumeItem(cardId, 1)
        return true
    }
}
