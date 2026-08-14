package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [TerminalRequest] — visit Source terminal + POST choice 1191 command input. */
class TerminalRequest(
    private val client: HttpClient,
    private val campgroundRequest: CampgroundRequest,
    private val falloutShelterRequest: FalloutShelterRequest,
) {
    suspend fun extrude(
        command: String,
        state: CharacterState?,
        preferences: Preferences?,
        accessibleCount: (Int) -> Int = { 0 },
    ): Result<String> = runCommand(command, state, preferences, accessibleCount)

    /**
     * Desktop [TerminalCommand] enhance path — resolve enhance target, gate uses, run
     * `enhance <file.enh>`, then increment `_sourceTerminalEnhanceUses`.
     */
    suspend fun enhance(
        target: String,
        state: CharacterState?,
        preferences: Preferences?,
        accessibleCount: (Int) -> Int = { 0 },
    ): Result<String> {
        if (!hasTerminal(state, preferences, accessibleCount)) {
            return Result.failure(IllegalStateException("You don't have a Source terminal."))
        }
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        val limit = enhanceLimit(prefs)
        val used = prefs.getInt(ENHANCE_USES_PREF, 0)
        if (used >= limit) {
            return Result.failure(IllegalStateException("Source Terminal enhance limit reached"))
        }
        val command = resolveEnhanceCommand(target, prefs)
            ?: return Result.failure(
                IllegalArgumentException("$target is not a valid enhance target."),
            )
        return runCommand(command, state, preferences, accessibleCount).onSuccess {
            prefs.setInt(ENHANCE_USES_PREF, used + 1)
        }
    }

    suspend fun enquiry(
        target: String,
        state: CharacterState?,
        preferences: Preferences?,
        accessibleCount: (Int) -> Int = { 0 },
    ): Result<String> {
        val command = resolveEnquiryCommand(target, preferences)
            ?: return Result.failure(
                IllegalArgumentException("$target is not a valid enquiry target."),
            )
        return runCommand(command, state, preferences, accessibleCount)
    }

    suspend fun educate(
        target: String,
        state: CharacterState?,
        preferences: Preferences?,
        accessibleCount: (Int) -> Int = { 0 },
    ): Result<String> {
        val command = resolveEducateCommand(target, preferences)
            ?: return Result.failure(
                IllegalArgumentException("$target is not a valid educate target."),
            )
        return runCommand(command, state, preferences, accessibleCount)
    }

    /**
     * Desktop [TerminalCommand] extrude path — resolve target, gate daily uses, run
     * `extrude -f <file.ext>`, then increment `_sourceTerminalExtrudes`.
     */
    suspend fun cliExtrude(
        target: String,
        state: CharacterState?,
        preferences: Preferences?,
        accessibleCount: (Int) -> Int = { 0 },
    ): Result<String> {
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        val used = prefs.getInt(EXTRUDE_USES_PREF, 0)
        if (used >= EXTRUDE_DAILY_LIMIT) {
            return Result.failure(IllegalStateException("Source Terminal extrude limit reached"))
        }
        val command = resolveExtrudeCommand(target, prefs)
            ?: return Result.failure(
                IllegalArgumentException("$target is not a valid extrude target."),
            )
        return runCommand(command, state, preferences, accessibleCount).onSuccess {
            prefs.setInt(EXTRUDE_USES_PREF, used + 1)
        }
    }

    suspend fun runCommand(
        input: String,
        state: CharacterState?,
        preferences: Preferences?,
        accessibleCount: (Int) -> Int = { 0 },
    ): Result<String> {
        if (!hasTerminal(state, preferences, accessibleCount)) {
            return Result.failure(IllegalStateException("You don't have a Source terminal."))
        }
        val visit = if (state?.inNuclearAutumn == true) {
            falloutShelterRequest.visitTerminal()
        } else {
            campgroundRequest.visitTerminal()
        }
        visit.exceptionOrNull()?.let { return Result.failure(it) }
        return postTerminalCommand(input)
    }

    private suspend fun postTerminalCommand(input: String): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/choice.php",
            formParameters = terminalCommandForm(input),
        )
        Result.success(response.bodyAsText())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    companion object {
        private const val SOURCE_TERMINAL = 9033
        private const val REPLICA_SOURCE_TERMINAL = 11231
        private const val TERMINAL_CHOICE = 1191
        const val ENHANCE_USES_PREF = "_sourceTerminalEnhanceUses"
        const val ENHANCE_KNOWN_PREF = "sourceTerminalEnhanceKnown"
        const val ENQUIRY_KNOWN_PREF = "sourceTerminalEnquiryKnown"
        const val EDUCATE_KNOWN_PREF = "sourceTerminalEducateKnown"
        const val EXTRUDE_KNOWN_PREF = "sourceTerminalExtrudeKnown"
        const val EXTRUDE_USES_PREF = "_sourceTerminalExtrudes"
        const val CHIPS_PREF = "sourceTerminalChips"
        const val EXTRUDE_DAILY_LIMIT = 3

        internal fun hasTerminal(
            state: CharacterState?,
            preferences: Preferences?,
            accessibleCount: (Int) -> Int,
        ): Boolean {
            if (CampgroundItemSync.hasSourceTerminal(preferences)) return true
            if (accessibleCount(SOURCE_TERMINAL) > 0) return true
            if (state?.inLegacyOfLoathing == true && accessibleCount(REPLICA_SOURCE_TERMINAL) > 0) {
                return true
            }
            return state?.inNuclearAutumn == true && accessibleCount(SOURCE_TERMINAL) > 0
        }

        fun enhanceLimit(preferences: Preferences?): Int {
            var limit = 1
            val chips = preferences?.getString(CHIPS_PREF, "").orEmpty()
            if (chips.contains("CRAM", ignoreCase = true)) limit++
            if (chips.contains("SCRAM", ignoreCase = true)) limit++
            return limit
        }

        /**
         * Desktop [TerminalCommand] enhance target aliases → full `enhance <file>` command.
         * Returns null when the target is invalid (or soft-gated known file is missing).
         */
        fun resolveEnhanceCommand(target: String, preferences: Preferences?): String? {
            val input = target.trim().lowercase()
            if (input.isEmpty()) return null
            val files = preferences?.getString(ENHANCE_KNOWN_PREF, "").orEmpty()
            return when {
                input.startsWith("item") -> "enhance items.enh"
                input.startsWith("init") -> "enhance init.enh"
                input.startsWith("meat") -> "enhance meat.enh"
                input.startsWith("sub") &&
                    (files.isEmpty() || files.contains("substats.enh")) -> "enhance substats.enh"
                input.startsWith("damage") &&
                    (files.isEmpty() || files.contains("damage.enh")) -> "enhance damage.enh"
                input.startsWith("crit") &&
                    (files.isEmpty() || files.contains("critical.enh")) -> "enhance critical.enh"
                input.endsWith(".enh") -> "enhance ${input.removePrefix("enhance ").trim()}"
                else -> null
            }
        }

        fun resolveEnquiryCommand(target: String, preferences: Preferences?): String? {
            val input = target.trim().lowercase()
            if (input.isEmpty()) return null
            val files = preferences?.getString(ENQUIRY_KNOWN_PREF, "").orEmpty()
            return when {
                input.startsWith("fam") -> "enquiry familiar.enq"
                input.startsWith("mon") -> "enquiry monsters.enq"
                input.startsWith("protect") && knownFile(files, "protect.enq") ->
                    "enquiry protect.enq"
                input.startsWith("stat") && knownFile(files, "stats.enq") ->
                    "enquiry stats.enq"
                else -> null
            }
        }

        fun resolveEducateCommand(target: String, preferences: Preferences?): String? {
            val input = target.trim().lowercase()
            if (input.isEmpty()) return null
            val files = preferences?.getString(EDUCATE_KNOWN_PREF, "").orEmpty()
            return when {
                input.startsWith("compr") && knownFile(files, "compress.edu") ->
                    "educate compress.edu"
                input.startsWith("digit") -> "educate digitize.edu"
                input.startsWith("dup") && knownFile(files, "duplicate.edu") ->
                    "educate duplicate.edu"
                input.startsWith("extr") -> "educate extract.edu"
                input.startsWith("port") && knownFile(files, "portscan.edu") ->
                    "educate portscan.edu"
                input.startsWith("turbo") && knownFile(files, "turbo.edu") ->
                    "educate turbo.edu"
                else -> null
            }
        }

        fun resolveExtrudeCommand(target: String, preferences: Preferences?): String? {
            val input = target.trim().lowercase()
            if (input.isEmpty()) return null
            val files = preferences?.getString(EXTRUDE_KNOWN_PREF, "").orEmpty()
            return when {
                input.startsWith("booze") || input.contains("gibson") -> "extrude -f booze.ext"
                input.startsWith("food") || input.contains("cookie") -> "extrude -f food.ext"
                input.startsWith("fam") && knownFile(files, "familiar.ext") ->
                    "extrude -f familiar.ext"
                input.startsWith("goggles") -> "extrude -f goggles.ext"
                input.startsWith("cram") && knownFile(files, "cram.ext") -> "extrude -f cram.ext"
                input.startsWith("dram") && knownFile(files, "dram.ext") -> "extrude -f dram.ext"
                input.startsWith("gram") && knownFile(files, "gram.ext") -> "extrude -f gram.ext"
                input.startsWith("pram") && knownFile(files, "pram.ext") -> "extrude -f pram.ext"
                input.startsWith("spam") && knownFile(files, "spam.ext") -> "extrude -f spam.ext"
                input.startsWith("tram") && knownFile(files, "tram.ext") -> "extrude -f tram.ext"
                else -> null
            }
        }

        private fun knownFile(files: String, filename: String): Boolean =
            files.isEmpty() || files.contains(filename, ignoreCase = true)

        internal fun terminalCommandForm(input: String): Parameters =
            parameters {
                append("whichchoice", TERMINAL_CHOICE.toString())
                append("option", "1")
                append("input", input)
            }
    }
}
