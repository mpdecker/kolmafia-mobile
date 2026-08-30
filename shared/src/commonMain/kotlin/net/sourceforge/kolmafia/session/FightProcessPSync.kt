package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [FightRequest.processP] high-traffic pref writers (Phases 1626–1640).
 * Regex scan of fight HTML — not a full TagStatus/processNode port.
 */
object FightProcessPSync {

    private val TRAINSET_MOVE = Regex(
        """Your toy train moves ahead to (?:the|some) (.+?)\.""",
    )

    fun containsMacroError(str: String): Boolean =
        str.contains("Macro Abort") ||
            str.contains("Macro abort") ||
            str.contains("macro abort") ||
            str.contains("Could not match item(s) for use") ||
            str.contains("Invalid Macro") ||
            str.contains("Invalid macro")

    fun apply(
        html: String,
        preferences: Preferences?,
        won: Boolean = false,
        sessionLogger: SessionLogger? = null,
        mildManneredProfessor: Boolean = false,
    ): Boolean {
        if (preferences == null || html.isBlank()) return false
        val plain = html.replace(Regex("<[^>]+>"), " ")
        var changed = false

        if (containsMacroError(plain)) {
            // Prefer first matching error-looking sentence
            val err = plain.lineSequence()
                .map { it.trim() }
                .firstOrNull { containsMacroError(it) }
                ?: plain.take(200).trim()
            preferences.setString("lastMacroError", err)
            changed = true
        }

        if (plain.contains("Back to yearbook club") ||
            (plain.contains("yearbook") && plain.contains("Camera flashes"))
        ) {
            preferences.setBoolean("yearbookCameraPending", true)
            changed = true
        }

        if (plain.contains("Your potted plant swallows")) {
            preferences.setInt(
                "_carnivorousPottedPlantWins",
                preferences.getInt("_carnivorousPottedPlantWins", 0) + 1,
            )
            FightSessionLog.logText(
                "Your potted plant swallows your opponent whole.",
                sessionLogger,
            )
            changed = true
        }

        if (plain.contains("You flap your bat wings gustily")) {
            preferences.setInt(
                "_batWingsFreeFights",
                preferences.getInt("_batWingsFreeFights", 0) + 1,
            )
            FightSessionLog.logText(
                "You flap your bat wings gustily and launch yourself to your next adventure in an instant.",
                sessionLogger,
            )
            changed = true
        }

        if (plain.contains("Having bent physics with your non-Euclidean curveball")) {
            preferences.setInt(
                "_curveballFightsLeft",
                (preferences.getInt("_curveballFightsLeft", 0) - 1).coerceAtLeast(0),
            )
            changed = true
        }

        TRAINSET_MOVE.find(plain)?.groupValues?.getOrNull(1)?.let { piece ->
            onTrainsetMove(piece, preferences)
            FightSessionLog.logText(
                "Your toy train moves ahead to the $piece.",
                sessionLogger,
            )
            changed = true
        }
        if (plain.contains("Your toy train moves ahead to some empty track.")) {
            onTrainsetMove("Empty track", preferences)
            changed = true
        }

        if (won && plain.contains("You grab a nearby elf")) {
            preferences.setInt("elfGratitude", preferences.getInt("elfGratitude", 0) + 1)
            FightSessionLog.logText("You've earned 1 Elf Gratitude.", sessionLogger)
            changed = true
        }

        if (won && plain.contains("handy-dandy hook")) {
            FightSessionLog.logText(
                plain.lineSequence().map { it.trim() }
                    .firstOrNull { it.contains("handy-dandy hook") }
                    ?: "You snag a nearby piece of luggage with your handy-dandy hook.",
                sessionLogger,
            )
            changed = true
        }

        if (won && plain.contains("Your familiar grabs you something")) {
            FightSessionLog.logText(
                plain.lineSequence().map { it.trim() }
                    .firstOrNull { it.contains("Your familiar grabs you something") }
                    ?: "Your familiar grabs you something from the dining car.",
                sessionLogger,
            )
            changed = true
        }

        changed = applyResearchPoints(plain, preferences, mildManneredProfessor, sessionLogger) ||
            changed
        changed = applyLuckyGoldRing(plain, preferences, sessionLogger) || changed
        changed = applySeadent(plain, preferences, sessionLogger) || changed

        // Fuzzy dice damage is handled by FightDamageParser; log warm-and-fuzzy turtle return
        if (plain.contains("you feel all warm and fuzzy")) {
            FightSessionLog.logText("A freed guard turtle returns.", sessionLogger)
            changed = true
        }

        return changed
    }

    /** Desktop [TrainsetManager.onTrainsetMove] — always increments position. */
    fun onTrainsetMove(pieceName: String, preferences: Preferences): Boolean =
        TrainsetManager.onTrainsetMove(pieceName, preferences)

    fun applyResearchPoints(
        text: String,
        preferences: Preferences,
        mildManneredProfessor: Boolean,
        sessionLogger: SessionLogger?,
    ): Boolean {
        if (!mildManneredProfessor &&
            !text.contains("research point", ignoreCase = true) &&
            !text.contains("jot down some notes")
        ) {
            return false
        }
        when {
            text.contains("You jot down some notes quickly, before the fight starts.") -> {
                preferences.setInt(
                    "wereProfessorResearchPoints",
                    preferences.getInt("wereProfessorResearchPoints", 0) + 1,
                )
                FightSessionLog.logText("(You gain 1 research point)", sessionLogger)
                return true
            }
            text.contains("(You gain 10 research points)") -> {
                preferences.setInt(
                    "wereProfessorResearchPoints",
                    preferences.getInt("wereProfessorResearchPoints", 0) + 10,
                )
                FightSessionLog.logText("(You gain 10 research points)", sessionLogger)
                return true
            }
            text.contains("(You gain 5 research points)") -> {
                preferences.setInt(
                    "wereProfessorResearchPoints",
                    preferences.getInt("wereProfessorResearchPoints", 0) + 5,
                )
                FightSessionLog.logText("(You gain 5 research points)", sessionLogger)
                return true
            }
        }
        return false
    }

    fun applyLuckyGoldRing(
        text: String,
        preferences: Preferences,
        sessionLogger: SessionLogger?,
    ): Boolean {
        if (!text.contains("Your lucky gold ring gets warmer for a moment.")) return false
        FightSessionLog.logText(
            "Your lucky gold ring gets warmer for a moment.",
            sessionLogger,
        )
        if (text.contains("You look down and find a Volcoino!")) {
            preferences.setBoolean("_luckyGoldRingVolcoino", true)
        }
        return true
    }

    fun applySeadent(
        text: String,
        preferences: Preferences,
        sessionLogger: SessionLogger?,
    ): Boolean {
        if (!text.contains(
                "tiny bits of their constituent construct parts are attracted to the magic of your spear",
            )
        ) {
            return false
        }
        preferences.setInt(
            "seadentConstructKills",
            preferences.getInt("seadentConstructKills", 0) + 1,
        )
        FightSessionLog.logText(text.take(160), sessionLogger)
        if (text.contains("Whoa, they formed a whole new tine!")) {
            preferences.setInt("seadentLevel", preferences.getInt("seadentLevel", 0) + 1)
        }
        return true
    }
}
