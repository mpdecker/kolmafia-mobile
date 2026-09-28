package net.sourceforge.kolmafia.webui

/**
 * Desktop StationaryButtonDecorator — persistent combat/choice action buttons + hotkey script.
 */
object StationaryButtonDecorator {
    private val skillButtons = linkedSetOf<String>()

    fun addSkillButton(skillId: String) {
        if (skillId.isNotBlank()) skillButtons.add(skillId)
    }

    fun clearSkillButtons() {
        skillButtons.clear()
    }

    fun decorate(location: String, buffer: StringBuilder) {
        if (buffer.indexOf("mafia-stationary") >= 0) return
        val isFight = location.startsWith("fight.php")
        val isChoice = location.startsWith("choice.php")
        val isAdventure = location.startsWith("adventure.php")
        if (!isFight && !isChoice && !isAdventure) return

        val buttons = buildString {
            append("""<div class="mafia-stationary" id="stationarybuttons" style="margin:6px;">""")
            when {
                isFight -> {
                    append(button("attack", "Attack", "fight.php?action=attack"))
                    append(button("steal", "Pickpocket", "fight.php?action=steal"))
                    append(button("runaway", "Run Away", "fight.php?action=runaway"))
                    append(button("custom", "script", "fight.php?action=custom"))
                    skillButtons.take(6).forEach { id ->
                        append(button("skill$id", "skill $id", "fight.php?action=skill&whichskill=$id"))
                    }
                }
                isChoice -> {
                    append(button("auto", "auto", "choice.php?action=auto"))
                }
                isAdventure -> {
                    append(button("again", "Adventure Again", location.substringBefore('?')))
                }
            }
            append("</div>")
            append("""<link rel="stylesheet" href="/stationarybuttons.2.css" />""")
            append("""<script src="/stationarybuttons.2.js"></script>""")
            append("""<script src="/hotkeys.js"></script>""")
        }
        val formIdx = buffer.indexOf("<form")
        if (formIdx >= 0) {
            buffer.insert(formIdx, buttons)
        } else {
            RequestEditorKit.insertBefore(buffer, "</body>", buttons)
        }
    }

    private fun button(id: String, label: String, href: String): String =
        """<input type="button" class="button" id="button$id" value="$label" onClick="document.location='$href';">"""
}
