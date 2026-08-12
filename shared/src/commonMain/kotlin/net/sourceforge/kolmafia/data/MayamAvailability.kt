package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [MayamCommand.availableResonances] pref logic for Mayam Calendar resonances. */
object MayamAvailability {

    private val SYMBOL_POSITIONS = listOf(
        listOf("yam", "sword", "vessel", "fur", "chair", "eye"),
        listOf("yam", "lightning", "bottle", "wood", "meat"),
        listOf("yam", "eyepatch", "cheese", "wall"),
        listOf("yam", "clock", "explosion"),
    )

    private val RESONANCES = mapOf(
        "mayam spinach" to "eye yam eyepatch yam",
        "yam and swiss" to "yam meat cheese yam",
        "yam cannon" to "sword yam eyepatch explosion",
        "tiny yam cannon" to "fur lightning eyepatch yam",
        "yam battery" to "yam lightning yam clock",
        "stuffed yam stinkbomb" to "vessel yam cheese explosion",
        "furry yam buckler" to "fur yam wall yam",
        "thanksgiving bomb" to "yam yam yam explosion",
        "yamtility belt" to "yam meat eyepatch yam",
        "caught yam-handed" to "chair yam yam clock",
        "memories of cheesier age" to "yam yam cheese clock",
    )

    const val SYMBOLS_USED_PREF = "_mayamSymbolsUsed"

    fun availableResonances(prefs: Preferences?): List<String> {
        val ring1 = unusedForRing(prefs, 1)
        val ring2 = unusedForRing(prefs, 2)
        val ring3 = unusedForRing(prefs, 3)
        val ring4 = unusedForRing(prefs, 4)
        val available = mutableListOf<String>()
        for ((name, symbols) in RESONANCES) {
            val rings = symbols.split(" ")
            if (!ring1.contains(rings[0])) continue
            if (!ring2.contains(rings[1])) continue
            if (!ring3.contains(rings[2])) continue
            if (!ring4.contains(rings[3])) continue
            available += name
        }
        return available
    }

    fun symbolsFor(resonance: String): List<String>? =
        RESONANCES[resonance.lowercase()]?.split(" ")

    /** Exact key or unique substring match (desktop MayamCommand.resonance). */
    fun resolveResonance(query: String, prefs: Preferences?): String? {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return null
        if (RESONANCES.containsKey(q)) return q
        val potentials = RESONANCES.keys.filter { it.contains(q) }
        return potentials.singleOrNull()
    }

    fun positionOnRing(ringIndex: Int, symbol: String): Int? {
        val ring = SYMBOL_POSITIONS.getOrNull(ringIndex) ?: return null
        val pos = ring.indexOf(symbol.lowercase())
        return if (pos >= 0) pos else null
    }

    fun markSymbolsUsed(prefs: Preferences, symbols: List<String>) {
        val used = prefs.getString(SYMBOLS_USED_PREF, "")
            .split(",")
            .filter { it.isNotBlank() }
            .toMutableList()
        symbols.forEachIndexed { index, symbol ->
            val fromTop = index + 1
            val nameInPref = if (symbol == "yam") "yam$fromTop" else symbol
            if (!used.contains(nameInPref)) used += nameInPref
        }
        prefs.setString(SYMBOLS_USED_PREF, used.joinToString(","))
    }

    private fun unusedForRing(prefs: Preferences?, ringNumber: Int): List<String> {
        val symbolsUsed = prefs?.getString(SYMBOLS_USED_PREF, "").orEmpty()
            .split(",")
            .filter { it.isNotBlank() }
        val unused = mutableListOf<String>()
        val ring = SYMBOL_POSITIONS[ringNumber - 1]
        var currentRing = ringNumber
        for (symbol in ring) {
            val isYam = symbol == "yam"
            val nameInPref = if (isYam) symbol + currentRing else symbol
            if (!symbolsUsed.contains(nameInPref)) {
                unused += symbol
            }
            currentRing++
        }
        return unused
    }
}
