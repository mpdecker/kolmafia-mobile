package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.CrystalBallManager
import net.sourceforge.kolmafia.session.EncounterManager
import net.sourceforge.kolmafia.track.TrackManager

/**
 * Desktop [net.sourceforge.kolmafia.persistence.AdventureQueueDatabase] —
 * rolling combat encounter queue per zone for stateful appearance rates.
 *
 * Persistence is pref-backed JSON-ish (`zone=m1|m2|…` lines) rather than Java
 * serialization so mobile can keep queues across sessions without binary blobs.
 */
object AdventureQueueDatabase {
    private const val COMBAT_PREF = "adventureCombatQueue"
    private const val NONCOMBAT_PREF = "adventureNoncombatQueue"
    private const val QUEUE_SIZE = 5

    private val combatQueue = linkedMapOf<String, ArrayDeque<String>>()
    private val noncombatQueue = linkedMapOf<String, ArrayDeque<String>>()

    fun resetQueue() {
        combatQueue.clear()
        noncombatQueue.clear()
        checkZones()
    }

    fun enqueue(locationName: String?, monsterName: String?) {
        if (locationName.isNullOrBlank() || monsterName.isNullOrBlank()) return
        val canonical = canonicalizeMonsterName(monsterName) ?: return
        val q = combatQueue.getOrPut(locationName) { ArrayDeque() }
        q.addLast(canonical)
        while (q.size > QUEUE_SIZE) q.removeFirst()
    }

    fun enqueueNoncombat(locationName: String?, encounterName: String?) {
        if (locationName.isNullOrBlank() || encounterName.isNullOrBlank()) return
        val q = noncombatQueue.getOrPut(locationName) { ArrayDeque() }
        q.addLast(encounterName)
        while (q.size > QUEUE_SIZE) q.removeFirst()
    }

    fun getZoneQueue(locationName: String): List<String> =
        combatQueue[locationName]?.toList().orEmpty()

    fun getZoneNoncombatQueue(locationName: String): List<String> =
        noncombatQueue[locationName]?.toList().orEmpty()

    /**
     * Desktop [AdventureQueueDatabase.checkZones] — ensure combat zones from
     * [AdventureDatabase] / [CombatDatabase] exist as queue keys.
     */
    fun checkZones() {
        for (zone in AdventureDatabase.all()) {
            val name = zone.locationName
            if (name.isNotBlank()) combatQueue.putIfAbsent(name, ArrayDeque())
        }
        for (data in CombatDatabase.all()) {
            val loc = data.locationName
            if (loc.isNotBlank()) combatQueue.putIfAbsent(loc, ArrayDeque())
        }
    }

    /** Desktop enqueue canonicalize: MonsterDatabase lookup + strip leading "the ". */
    internal fun canonicalizeMonsterName(monsterName: String): String? {
        MonsterDatabase.getByName(monsterName)?.name?.let { return it }
        if (monsterName.startsWith("the ", ignoreCase = true) && monsterName.length > 4) {
            MonsterDatabase.getByName(monsterName.substring(4))?.name?.let { return it }
        }
        // Fall back to trimmed input so unknown monsters still queue for tests / new content.
        return monsterName.trim().takeIf { it.isNotEmpty() }
    }

    /**
     * Desktop [AdventureQueueDatabase.applyQueueEffects] —
     * rate for monster IN queue is 1/(4a−3b); NOT IN queue is 4/(4a−3b).
     *
     * [numerator] is already `combatPercent * weighting` (desktop pre-division form).
     */
    fun applyQueueEffects(
        numerator: Double,
        monsterName: String,
        locationName: String,
        totalWeighting: Int,
        weightOf: (String) -> Int,
        preferences: Preferences? = null,
        turnsPlayed: Int = 0,
        areaCombatPercent: Double = 100.0,
        crystalBallEquipped: Boolean = false,
    ): Double {
        // Desktop saber force: 100% forced monster / 0% others
        if (EncounterManager.isSaberForceZone(locationName, preferences)) {
            return if (EncounterManager.isSaberForceMonster(monsterName, locationName, preferences)) {
                100.0
            } else {
                0.0
            }
        }
        // Desktop crystal ball: predicted monster gets zone combat%; others 0
        if (CrystalBallManager.isCrystalBallZone(locationName, crystalBallEquipped)) {
            return if (CrystalBallManager.isCrystalBallMonster(
                    monsterName, locationName, crystalBallEquipped,
                )
            ) {
                areaCombatPercent
            } else {
                0.0
            }
        }
        val denom = totalWeighting.toDouble()
        if (denom <= 0) return 0.0
        val zoneQueue = combatQueue[locationName]
        if (zoneQueue == null || zoneQueue.isEmpty()) {
            return numerator / denom
        }
        val zoneSet = zoneQueue.map { it.lowercase() }.toSet()
        var queueWeight = 0
        for (mon in zoneQueue.toSet()) {
            val w = weightOf(mon)
            val olfacted = preferences?.let {
                TrackManager.isQueueIgnored(it, mon, turnsPlayed)
            } == true
            if (w > 0 && !olfacted) queueWeight += w
        }
        val olfacted = preferences?.let {
            TrackManager.isQueueIgnored(it, monsterName, turnsPlayed)
        } == true
        val inQueue = zoneSet.contains(monsterName.lowercase()) && !olfacted
        val newNumerator = numerator * (if (inQueue) 1.0 else 4.0)
        val newDenominator = (4.0 * denom - 3.0 * queueWeight)
        if (newDenominator <= 0) return 0.0
        return newNumerator / newDenominator
    }

    fun serialize(preferences: Preferences?) {
        preferences ?: return
        preferences.setString(COMBAT_PREF, encode(combatQueue))
        preferences.setString(NONCOMBAT_PREF, encode(noncombatQueue))
    }

    fun deserialize(preferences: Preferences?) {
        preferences ?: return
        combatQueue.clear()
        noncombatQueue.clear()
        decode(preferences.getString(COMBAT_PREF, ""), combatQueue)
        decode(preferences.getString(NONCOMBAT_PREF, ""), noncombatQueue)
        checkZones()
    }

    private fun encode(map: Map<String, ArrayDeque<String>>): String =
        map.entries.joinToString("\n") { (zone, q) ->
            "$zone=${q.joinToString("|")}"
        }

    private fun decode(raw: String, into: MutableMap<String, ArrayDeque<String>>) {
        for (line in raw.lines()) {
            val eq = line.indexOf('=')
            if (eq <= 0) continue
            val zone = line.substring(0, eq)
            val monsters = line.substring(eq + 1).split('|').filter { it.isNotBlank() }
            into[zone] = ArrayDeque(monsters.takeLast(QUEUE_SIZE))
        }
    }
}
