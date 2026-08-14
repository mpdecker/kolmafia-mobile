package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [BeachManager] static beach-head data for maximizer availability. */
object BeachHeadAvailability {

    const val BEACH_COMB_ID = 10258
    const val DRIFTWOOD_BEACH_COMB_ID = 10291
    const val HEADS_USED_PREF = "_beachHeadsUsed"
    const val HEADS_UNLOCKED_PREF = "beachHeadsUnlocked"

    data class BeachHead(
        val id: Int,
        val effect: String,
        val beach: Int,
        val coords: String,
        val desc: String,
    )

    val BEACH_HEADS = arrayOf(
        BeachHead(1, "Hot-Headed", 420, "8,4197", "hot"),
        BeachHead(2, "Cold as Nice", 2323, "8,23222", "cold"),
        BeachHead(3, "A Brush with Grossness", 4242, "8,42412", "stench"),
        BeachHead(4, "Does It Have a Skull In There??", 6969, "8,69682", "spooky"),
        BeachHead(5, "Oiled, Slick", 8888, "8,88879", "sleaze"),
        BeachHead(6, "Lack of Body-Building", 37, "8,368", "muscle"),
        BeachHead(7, "We're All Made of Starfish", 3737, "8,37368", "mysticality"),
        BeachHead(8, "Pomp & Circumsands", 7114, "8,71138", "moxie"),
        BeachHead(9, "Resting Beach Face", 5555, "9,55549", "initiative"),
        BeachHead(10, "Do I Know You From Somewhere?", 1111, "9,11109", "familiar"),
        BeachHead(11, "You Learned Something Maybe!", 9696, "9,96958", "experience"),
    )

    private val effectToBeachHead: Map<String, BeachHead> =
        BEACH_HEADS.associateBy { it.effect.lowercase() }

    private val idToBeachHead: Map<Int, BeachHead> =
        BEACH_HEADS.associateBy { it.id }

    fun parseBeachHeadsUsed(prefs: Preferences?): Set<Int> =
        parseIdSet(prefs?.getString(HEADS_USED_PREF, "").orEmpty())

    fun parseBeachHeadsUnlocked(prefs: Preferences?): Set<Int> =
        parseIdSet(prefs?.getString(HEADS_UNLOCKED_PREF, "").orEmpty())

    fun headAvailable(effectName: String, prefs: Preferences?): Boolean {
        val head = effectToBeachHead[effectName.lowercase()] ?: return false
        return head.id !in parseBeachHeadsUsed(prefs)
    }

    fun headById(id: Int): BeachHead? = idToBeachHead[id]

    /** Resolve NUM, effect name, or keyword (hot/cold/…) to a beach head. */
    fun resolveHead(query: String): BeachHead? {
        val q = query.trim()
        if (q.isEmpty()) return null
        q.toIntOrNull()?.let { return idToBeachHead[it] }
        effectToBeachHead[q.lowercase()]?.let { return it }
        val descMatches = BEACH_HEADS.filter { it.desc.startsWith(q, ignoreCase = true) }
        if (descMatches.size == 1) return descMatches[0]
        val effectMatches = BEACH_HEADS.filter {
            it.effect.contains(q, ignoreCase = true)
        }
        return effectMatches.singleOrNull()
    }

    fun markHeadUsed(prefs: Preferences, headId: Int) {
        val used = parseBeachHeadsUsed(prefs).toMutableSet()
        if (used.add(headId)) {
            prefs.setString(HEADS_USED_PREF, used.sorted().joinToString(","))
        }
    }

    private fun parseIdSet(raw: String): Set<Int> {
        if (raw.isBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { token ->
                token.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
            }
            .toSet()
    }
}
