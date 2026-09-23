package net.sourceforge.kolmafia.webui

/** Desktop FightDecorator — end-of-fight / monster / location helpers. */
object FightDecorator {
    fun decorateEndOfFight(buffer: StringBuilder) {
        if (buffer.indexOf("You win the fight") < 0 &&
            buffer.indexOf("You lose") < 0 &&
            buffer.indexOf("You slap") < 0
        ) {
            return
        }
        if (buffer.indexOf("mafia-fight-end") >= 0) return
        val again = """<div class="mafia-fight-end"><a href="main.php">Back to Main</a></div>"""
        RequestEditorKit.insertBefore(buffer, "</body>", again)
    }

    fun decorateMonster(buffer: StringBuilder) {
        // Spot for monster-specific helpers; marker for tests.
        if (buffer.indexOf("Monsters of Loathing") >= 0 || buffer.indexOf("combatform") >= 0) {
            if (buffer.indexOf("mafia-fight-monster") < 0) {
                RequestEditorKit.insertBefore(
                    buffer,
                    "</body>",
                    """<span class="mafia-fight-monster"></span>""",
                )
            }
        }
    }

    fun decorateLocation(buffer: StringBuilder) {
        if (buffer.indexOf("mafia-fight-location") >= 0) return
        RequestEditorKit.insertBefore(
            buffer,
            "</body>",
            """<span class="mafia-fight-location"></span>""",
        )
    }
}
