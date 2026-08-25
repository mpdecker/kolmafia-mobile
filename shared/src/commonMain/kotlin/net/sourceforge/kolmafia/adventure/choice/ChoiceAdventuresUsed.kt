package net.sourceforge.kolmafia.adventure.choice

/**
 * Desktop [ChoiceControl.getAdventuresUsed] high-traffic choice-URL subset (Phases 1701–1715).
 */
object ChoiceAdventuresUsed {

    private val WHICH_CHOICE = Regex("""whichchoice=(\d+)""", RegexOption.IGNORE_CASE)
    private val OPTION = Regex("""option=(\d+)""", RegexOption.IGNORE_CASE)

    /**
     * Adventures consumed by submitting [urlString] for a choice.php action.
     * Returns 0 when unknown / free.
     */
    fun getAdventuresUsed(urlString: String): Int {
        if (!urlString.contains("choice.php", ignoreCase = true)) return 0
        val choice = WHICH_CHOICE.find(urlString)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return 0
        val option = OPTION.find(urlString)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        return adventuresForChoice(choice, option)
    }

    fun adventuresForChoice(choice: Int, option: Int = 0): Int = when (choice) {
        // Deck of Every Card draws (high-traffic)
        1085, 1086 -> 1
        // Beach Comb / sandworm / etc. commonly 1
        1388, 1389, 1390, 1391 -> 1
        // Reminisce / Locket
        1463 -> 1
        // Deferred temple/black forest already billed via DeferredChoice
        125, 1018, 1019 -> 1
        // Hedge maze rooms take a turn
        in 1005..1013 -> 1
        // Gym workouts
        770, 792 -> if (option > 0) 1 else 0
        else -> 0
    }
}
