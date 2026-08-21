package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Choose a Soundtrack choice 1312.
 */
object BoomBoxChoiceSync {

    const val CHOICE_ID = 1312
    const val SING_ALONG_SKILL_ID = 7297

    private val BOOMBOX_PATTERN = Regex("""you can do <b>(\d+)</b> more""")
    private val BOOMBOX_SONG_PATTERN = Regex("""&quot;(.*?)&quot;( \(Keep playing\)|)""")

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        BOOMBOX_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            preferences.setInt("_boomBoxSongsLeft", it)
        }
        preferences.setString("boomBoxSong", "")
        for (match in BOOMBOX_SONG_PATTERN.findAll(html)) {
            if (match.groupValues.getOrNull(2)?.contains("Keep playing") == true) {
                preferences.setString("boomBoxSong", match.groupValues[1])
            }
        }
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        learnSkill: (Int) -> Unit = {},
        removeSkill: (Int) -> Unit = {},
        sessionLog: (String) -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (html.contains("decide not to change the station")) return false
        val songChosen = when (decision) {
            1 -> "Eye of the Giger"
            2 -> "Food Vibrations"
            3 -> "Remainin' Alive"
            4 -> "These Fists Were Made for Punchin'"
            5 -> "Total Eclipse of Your Meat"
            else -> ""
        }
        if (songChosen.isNotEmpty()) {
            learnSkill(SING_ALONG_SKILL_ID)
            val current = preferences.getString("boomBoxSong", "")
            if (current != songChosen) {
                preferences.setString("boomBoxSong", songChosen)
                preferences.setInt(
                    "_boomBoxSongsLeft",
                    (preferences.getInt("_boomBoxSongsLeft", 0) - 1).coerceAtLeast(0),
                )
                val message = "Setting soundtrack to $songChosen"
                sessionLog(message)
            }
        } else {
            removeSkill(SING_ALONG_SKILL_ID)
            if (preferences.getString("boomBoxSong", "").isNotEmpty()) {
                preferences.setString("boomBoxSong", "")
                sessionLog("Switching soundtrack off")
            }
        }
        return true
    }
}
