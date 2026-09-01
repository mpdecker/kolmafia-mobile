package net.sourceforge.kolmafia.session

/** Formats the headless equivalent of the desktop `dad` command. */
object DadCliState {
    fun report(hasSkill: (String) -> Boolean = { false }): List<String> =
        (1..10).map { round ->
            val weakness = DadManager.weakness(round)
            "Round $round: ${DadManager.elementToName(weakness)} " +
                "(${DadManager.elementToSpell(weakness, hasSkill)})"
        }
}
