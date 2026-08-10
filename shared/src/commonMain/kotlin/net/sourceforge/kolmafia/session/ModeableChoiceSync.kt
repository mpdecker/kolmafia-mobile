package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.equipment.Modeable
import net.sourceforge.kolmafia.preferences.Preferences

/** Parses modeable choice responses to keep prefs in sync (desktop choice hooks). */
object ModeableChoiceSync {

    fun applyFromChoiceUrl(url: String, responseText: String, preferences: Preferences?) {
        if (preferences == null) return
        when {
            url.contains("whichchoice=1466") -> parseUmbrella(url, responseText, preferences)
            url.contains("whichchoice=1481") -> parseParka(url, preferences)
            url.contains("whichchoice=1449") -> parseBackupCamera(url, preferences)
            url.contains("whichchoice=1063") -> parseEdpiece(url, preferences)
            url.contains("whichchoice=640") -> parseSnowsuit(url, preferences)
            url.contains("whichchoice=1509") -> parseLedCandle(url, preferences)
        }
    }

    private fun parseUmbrella(url: String, responseText: String, preferences: Preferences) {
        val modes = listOf(
            1 to Triple("broken", "howling mass of chaos", "broken"),
            2 to Triple("forward-facing", "the stuff in front of you", "forward-facing"),
            3 to Triple("bucket style", "dangle by the handle", "bucket style"),
            4 to Triple("pitchfork style", "pops inside out", "pitchfork style"),
            5 to Triple("constantly twirling", "evenly distributes the curse", "constantly twirling"),
            6 to Triple("cocoon", "step inside it", "cocoon"),
        )
        for ((option, triple) in modes) {
            if (url.contains("option=$option") && responseText.contains(triple.second, ignoreCase = true)) {
                preferences.setString("umbrellaState", triple.third)
                return
            }
        }
    }

    private fun parseParka(url: String, preferences: Preferences) {
        val modes = mapOf(
            1 to "kachungasaur",
            2 to "dilophosaur",
            3 to "spikolodon",
            4 to "ghostasaurus",
            5 to "pterodactyl",
        )
        for ((option, mode) in modes) {
            if (url.contains("option=$option")) {
                preferences.setString("parkaMode", mode)
                return
            }
        }
    }

    private fun parseBackupCamera(url: String, preferences: Preferences) {
        val modes = mapOf(
            1 to "ml",
            2 to "meat",
            3 to "init",
        )
        for ((option, mode) in modes) {
            if (url.contains("option=$option")) {
                preferences.setString("backupCameraMode", mode)
                return
            }
        }
    }

    private fun parseEdpiece(url: String, preferences: Preferences) {
        val modes = mapOf(
            1 to "bear",
            2 to "owl",
            3 to "puma",
            4 to "hyena",
            5 to "mouse",
            6 to "weasel",
            7 to "fish",
        )
        for ((option, mode) in modes) {
            if (url.contains("option=$option")) {
                preferences.setString("edPiece", mode)
                return
            }
        }
    }

    private fun parseSnowsuit(url: String, preferences: Preferences) {
        val modes = mapOf(
            3 to "nose",
            4 to "goatee",
            5 to "hat",
        )
        for ((option, mode) in modes) {
            if (url.contains("option=$option")) {
                preferences.setString("snowsuit", mode)
                return
            }
        }
    }

    private fun parseLedCandle(url: String, preferences: Preferences) {
        val modes = mapOf(
            1 to "disco",
            2 to "ultraviolet",
            3 to "reading",
            4 to "red light",
        )
        for ((option, mode) in modes) {
            if (url.contains("option=$option")) {
                preferences.setString("ledCandleMode", mode)
                return
            }
        }
    }

    fun writeModePref(preferences: Preferences?, modeable: Modeable, mode: String) {
        if (preferences == null) return
        when (modeable) {
            Modeable.RETROCAPE -> {
                val parts = mode.trim().split(Regex("\\s+"), limit = 2)
                if (parts.size == 2) {
                    preferences.setString("retroCapeSuperhero", parts[0])
                    preferences.setString("retroCapeWashingInstructions", parts[1])
                }
            }
            else -> {
                val pref = modeable.statePref ?: return
                preferences.setString(pref, mode)
            }
        }
    }
}
