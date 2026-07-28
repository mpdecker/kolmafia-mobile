package net.sourceforge.kolmafia.character

/**
 * Parses class/path resources from charpane HTML. Mirrors desktop [CharPaneRequest]
 * fury/soulsauce/audience/paradoxicity/mask/absorbs checks.
 */
object ClassResourceCharpaneSync {

    data class ParsedValues(
        val fury: Int? = null,
        val soulsauce: Int? = null,
        val audience: Int? = null,
        val paradoxicity: Int? = null,
        val currentMask: String? = null,
        val absorbs: Int? = null,
    )

    private val furyPattern = Regex(""">(\d+) gal\.</span>""")
    private val soulsaucePattern = Regex(
        """auce:(?:</small>)?</td><td align=left><b><font color=black>(?:<span>)?(\d+)<""",
    )
    private val audiencePattern = Regex("""<b>(\d+ )?(Love|Hate|Bored)</td>""")
    private val paradoxicityPattern = Regex("""Para?doxicity.*?(\d+)""")
    private val disguisePattern = Regex("""masks/mask(\d+)\.png""")
    private val absorbsPattern = Regex("""<b>Absorptions:</b> (\d+) / (\d+)</span>""")

    fun parse(html: String, state: CharacterState): ParsedValues {
        var fury: Int? = null
        var soulsauce: Int? = null
        var audience: Int? = null
        var currentMask: String? = null
        var absorbs: Int? = null

        if (state.characterClassEnum == CharacterClass.SEAL_CLUBBER) {
            fury = furyPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        } else if (state.characterClassEnum == CharacterClass.SAUCEROR) {
            soulsauce = soulsaucePattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        } else if (state.isSneakyPete) {
            val match = audiencePattern.find(html)
            audience = if (match != null) {
                when (match.groupValues[2]) {
                    "Love" -> match.groupValues[1].trim().toIntOrNull() ?: 0
                    "Hate" -> -(match.groupValues[1].trim().toIntOrNull() ?: 0)
                    else -> 0
                }
            } else {
                0
            }
        } else if (state.inNoobcore) {
            absorbs = absorbsPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }

        val paradoxicity = paradoxicityPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()

        if (state.inDisguise) {
            val maskId = disguisePattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
            currentMask = maskId?.let(::maskNameForId)
        }

        return ParsedValues(
            fury = fury,
            soulsauce = soulsauce,
            audience = audience,
            paradoxicity = paradoxicity,
            currentMask = currentMask,
            absorbs = absorbs,
        )
    }

    fun apply(character: KoLCharacter, html: String) {
        val parsed = parse(html, character.state.value)
        character.updateClassResource(
            fury = parsed.fury,
            soulsauce = parsed.soulsauce,
            audience = parsed.audience,
            absorbs = parsed.absorbs,
            currentMask = parsed.currentMask,
            paradoxicity = parsed.paradoxicity,
        )
    }

    internal fun maskNameForId(id: Int): String? = when (id) {
        1 -> "Mr. Mask"
        2 -> "devil mask"
        3 -> "protest mask"
        4 -> "batmask"
        5 -> "punk mask"
        6 -> "hockey mask"
        7 -> "bandit mask"
        8 -> "plague doctor mask"
        9 -> "robot mask"
        10 -> "skull mask"
        11 -> "monkey mask"
        12 -> "luchador mask"
        13 -> "welding mask"
        14 -> "ninja mask"
        15 -> "snowman mask"
        16 -> "gasmask"
        17 -> "fencing mask"
        18 -> "opera mask"
        19 -> "scary mask"
        20 -> "alien mask"
        21 -> "murderer mask"
        22 -> "pumpkin mask"
        23 -> "rabbit mask"
        24 -> "ski mask"
        25 -> "tiki mask"
        26 -> "motorcycle mask"
        27 -> "magical cartoon princess mask"
        28 -> "catcher's mask"
        29 -> "\"sexy\" mask"
        30 -> "werewolf mask"
        100 -> "Bonerdagon mask"
        101 -> "Naughty Sorceress mask"
        102 -> "Groar mask"
        103 -> "Ed the Undying mask"
        104 -> "Big Wisniewski mask"
        105 -> "The Man mask"
        106 -> "Boss Bat mask"
        else -> null
    }
}
