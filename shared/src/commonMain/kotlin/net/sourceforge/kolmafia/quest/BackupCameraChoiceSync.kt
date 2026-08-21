package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Set Backup Camera Mode choice 1449 —
 * visit infer mode/reverser + post decisions.
 */
object BackupCameraChoiceSync {

    const val CHOICE_ID = 1449

    const val MODE_PREF = "backupCameraMode"
    const val REVERSER_PREF = "backupCameraReverserEnabled"

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val setting = when {
            !html.contains("Warning Beep") -> "ml"
            !html.contains("Infrared Spectrum") -> "meat"
            !html.contains("Maximum Framerate") -> "init"
            else -> ""
        }
        preferences.setString(MODE_PREF, setting)
        preferences.setBoolean(REVERSER_PREF, html.contains("Disable Reverser"))
        return true
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        return when (decision) {
            1 -> {
                preferences.setString(MODE_PREF, "ml")
                true
            }
            2 -> {
                preferences.setString(MODE_PREF, "meat")
                true
            }
            3 -> {
                preferences.setString(MODE_PREF, "init")
                true
            }
            4 -> {
                preferences.setBoolean(REVERSER_PREF, true)
                true
            }
            5 -> {
                preferences.setBoolean(REVERSER_PREF, false)
                true
            }
            else -> false
        }
    }
}
