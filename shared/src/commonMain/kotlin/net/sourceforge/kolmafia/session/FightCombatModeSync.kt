package net.sourceforge.kolmafia.session

/**
 * Desktop [FightRequest] combat-mode / initiative / round sync (Phases 1371–1385).
 */
object FightCombatModeSync {

    const val HAIKU_DUNGEON = "138"
    const val CLUMSINESS_GROVE = "277"
    const val MAELSTROM_OF_LOVERS = "278"
    const val GLACIER_OF_JERKS = "279"
    const val DEEP_MACHINE_TUNNELS = "458"

    const val HAIKU_EFFECT = "Haiku State of Mind"
    const val ANAPEST_EFFECT = "Just the Best Anapests"

    private val ONTURN = Regex("""onturn\s*=\s*(\d+)""")

    var haiku: Boolean = false
    var anapest: Boolean = false
    var machineElf: Boolean = false
    var pokefam: Boolean = false
    var lastWonInitiative: Boolean = false

    fun reset() {
        haiku = false
        anapest = false
        machineElf = false
        pokefam = false
        lastWonInitiative = false
    }

    val isGarbled: Boolean get() = haiku || anapest || machineElf

    /**
     * Detect modes at fight start from adventure id, active effects, and HTML.
     */
    fun detectModes(
        adventureId: String = "",
        locationName: String = "",
        html: String = "",
        activeEffects: Collection<String> = emptyList(),
        inPokefam: Boolean = false,
    ) {
        val effectsLower = activeEffects.map { it.lowercase() }
        haiku = adventureId == HAIKU_DUNGEON ||
            effectsLower.any { it.contains("haiku state of mind") } ||
            html.contains("Haiku State of Mind", ignoreCase = true)
        anapest = adventureId in setOf(CLUMSINESS_GROVE, MAELSTROM_OF_LOVERS, GLACIER_OF_JERKS) ||
            effectsLower.any { it.contains("just the best anapests") } ||
            html.contains("Just the Best Anapests", ignoreCase = true)
        machineElf = adventureId == DEEP_MACHINE_TUNNELS ||
            locationName.contains("Deep Machine Tunnels", ignoreCase = true)
        pokefam = inPokefam && !html.contains("fight.php")
        // Mid-macro effect acquisition
        if (html.contains("Haiku State of Mind")) haiku = true
        if (html.contains("Just the Best Anapests")) anapest = true
    }

    /** Desktop [FightRequest.synchronizeRoundNumber]. */
    fun synchronizeRoundNumber(html: String): Int? {
        if (html.contains("You twiddle your thumbs.")) {
            // Twiddle does not advance round; re-sync from onturn if present
            ONTURN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { round ->
                ChoiceCombatAshState.currentRound = round
                return round
            }
            return ChoiceCombatAshState.currentRound
        }
        val round = ONTURN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        ChoiceCombatAshState.currentRound = round
        return round
    }

    /** Desktop [FightRequest.wonInitiative] (String). */
    fun wonInitiative(html: String): Boolean {
        val text = html.lowercase()
        if (text.contains("you get the jump")) return true
        if (html.contains("The Jump: ")) return true
        if (html.contains("as quick as a wink")) return true
        if (html.contains("It wasn't your foe, so it must have been you")) return true
        if (html.contains("Your foe is so slow")) return true
        if (html.contains("You're sneaky like that.")) return true
        if (html.contains("and get the first strike.")) return true
        if (html.contains("Nice and sportsmanlike.")) return true
        if (html.contains("It hesitates.")) return true
        return false
    }

    /** Desktop [FightRequest.wonInitiative] () — round 1 only. */
    fun wonInitiativeThisFight(): Boolean =
        ChoiceCombatAshState.currentRound == 1 && (pokefam || lastWonInitiative)

    fun applyFightHtml(
        html: String,
        adventureId: String = "",
        locationName: String = "",
        activeEffects: Collection<String> = emptyList(),
        inPokefam: Boolean = false,
        isFightStart: Boolean = false,
    ) {
        if (isFightStart) {
            reset()
            detectModes(adventureId, locationName, html, activeEffects, inPokefam)
        } else {
            if (html.contains("Haiku State of Mind")) haiku = true
            if (html.contains("Just the Best Anapests")) anapest = true
        }
        synchronizeRoundNumber(html)
        if (ChoiceCombatAshState.currentRound == 1) {
            lastWonInitiative = wonInitiative(html)
        }
    }
}
