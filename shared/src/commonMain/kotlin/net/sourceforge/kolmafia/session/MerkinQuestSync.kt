package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.session.ChoiceControl] Mer-kin boss victory choices 709/713/717. */
object MerkinQuestSync {
    private val CHOICE_PATTERN = Regex("""whichchoice=(\d+)""", RegexOption.IGNORE_CASE)

    fun choiceIdFromUrl(url: String): Int? =
        CHOICE_PATTERN.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()

    fun applyFromChoice(
        choiceId: Int,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        val prefs = preferences ?: return
        when (choiceId) {
            709 -> {
                prefs.setBoolean("shubJigguwattDefeated", true)
                prefs.setString("merkinQuestPath", "done")
                sessionLogger?.appendRawLine("Mer-kin quest: Shub-Jigguwatt defeated")
            }
            713 -> {
                prefs.setBoolean("yogUrtDefeated", true)
                prefs.setString("merkinQuestPath", "done")
                sessionLogger?.appendRawLine("Mer-kin quest: Yog-Urt defeated")
            }
            717 -> {
                prefs.setString("merkinQuestPath", "done")
                sessionLogger?.appendRawLine("Mer-kin quest complete")
            }
        }
    }

    fun applyFromUrl(
        url: String?,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        when (val choiceId = url?.let(::choiceIdFromUrl)) {
            709, 713, 717 -> applyFromChoice(choiceId, preferences, sessionLogger)
        }
    }
}
