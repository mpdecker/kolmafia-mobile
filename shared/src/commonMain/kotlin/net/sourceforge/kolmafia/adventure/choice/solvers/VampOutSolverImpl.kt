package net.sourceforge.kolmafia.adventure.choice.solvers

import net.sourceforge.kolmafia.adventure.choice.ChoiceUtilities
import net.sourceforge.kolmafia.preferences.Preferences

class VampOutSolverImpl(private val preferences: Preferences) : VampOutSolver {

    // Indexed 0-12 (goal 1 = index 0). '0' at position 0 = location-select page.
    // Port of desktop VampOutManager.VampOutScript[].
    private val scripts = arrayOf(
        "022121221",    // goal 1: Mistified
        "02212111",     // goal 2: Bat Attitude
        "02231111",     // goal 3: There Wolf
        "011",          // goal 4: Muscle substats
        "0131",         // goal 5: Mysticality substats
        "01221",        // goal 6: Moxie substats
        "01232",        // goal 7: Meat
        "031241mtbv11", // goal 8: Prince + Sword (Brouhaha)
        "042112mvtb11", // goal 9: Prince + Sceptre (Torremolinos)
        "014423vmbt11", // goal 10: Prince + Medallion (Ventrilo)
        "023334tvbm11", // goal 11: Prince + Chalice (Malkovich)
        "031241vmtb11", // goal 12: Pride + Interview
        "031241vbtm11", // goal 13: Black heart
    )

    private val vladGoals     = 0..2
    private val isabellaGoals = 3..6
    // masquerade = 7..12

    override fun autoVampOut(preference: Int, stepCount: Int, responseText: String): Int? {
        val goalIdx = preference - 1
        val script  = scripts.getOrNull(goalIdx) ?: return null
        if (stepCount < 0 || stepCount >= script.length) return null

        // Step 0 on the location-select page
        if (stepCount == 0 && responseText.contains("Finally, the sun has set.")) {
            return pickStartingLocation(goalIdx, responseText)
        }

        val ch = script[stepCount]
        // '0' at position 0 but NOT the location page → can't continue
        if (ch == '0') return null

        return resolveScriptChar(ch, responseText)
    }

    private fun pickStartingLocation(goalIdx: Int, responseText: String): Int {
        val vladAvailable       = responseText.contains("Visit Vlad's Boutique")
        val isabellaAvailable   = responseText.contains("Visit Isabella's")
        val masqueradeAvailable = responseText.contains("Visit The Masquerade")

        preferences.setBoolean(Preferences.INTERVIEW_VLAD,       !vladAvailable)
        preferences.setBoolean(Preferences.INTERVIEW_ISABELLA,   !isabellaAvailable)
        preferences.setBoolean(Preferences.INTERVIEW_MASQUERADE, !masqueradeAvailable)

        // Desktop behavior: on a 4th visit when all 3 locations have already been used,
        // return option 1 (the "skip" / advance choice on that no-op page).
        if (!vladAvailable && !isabellaAvailable && !masqueradeAvailable) return 1

        val vladChoice       = if (vladAvailable) 1 else 0
        val isabellaChoice   = if (isabellaAvailable) 2 - (if (vladAvailable) 0 else 1) else 0
        val masqueradeChoice =
            if (masqueradeAvailable) 3 - (if (isabellaAvailable) 0 else 1) - (if (vladAvailable) 0 else 1) else 0

        return when (goalIdx) {
            in vladGoals     -> vladChoice.coerceAtLeast(firstAvailable(vladAvailable, isabellaAvailable, masqueradeAvailable))
            in isabellaGoals -> isabellaChoice.coerceAtLeast(firstAvailable(isabellaAvailable, masqueradeAvailable, vladAvailable))
            else             -> masqueradeChoice.coerceAtLeast(firstAvailable(masqueradeAvailable, isabellaAvailable, vladAvailable))
        }
    }

    private fun firstAvailable(vararg flags: Boolean): Int =
        when {
            flags.getOrNull(0) == true -> 1
            flags.getOrNull(1) == true -> if (flags.getOrNull(0) != true) 1 else 2
            flags.getOrNull(2) == true -> {
                var option = 1
                if (flags.getOrNull(0) == true) option++
                if (flags.getOrNull(1) == true) option++
                option
            }
            else -> 1
        }

    private fun resolveScriptChar(ch: Char, responseText: String): Int? {
        if (ch.isDigit()) return ch.digitToInt()
        val keyword = when (ch) {
            'm' -> "Malkovich"
            'b' -> "Brouhaha"
            't' -> "Torremolinos"
            'v' -> "Ventrilo"
            else -> return null
        }
        return findChoiceDecisionIndex(keyword, responseText)
    }

    /** Returns the option number whose text contains [text], or null if not found. */
    private fun findChoiceDecisionIndex(text: String, responseText: String): Int? {
        return ChoiceUtilities.parseChoices(responseText)
            .entries
            .firstOrNull { it.value.contains(text) }
            ?.key
    }
}
