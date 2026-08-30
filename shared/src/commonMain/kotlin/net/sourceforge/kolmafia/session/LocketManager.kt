package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [net.sourceforge.kolmafia.session.LocketManager] — Combat Lover's Locket catalog,
 * fought-today set, fight/phylum parse, and reminisce-page option parse.
 */
object LocketManager {
    const val PREF_FOUGHT = "_locketMonstersFought"
    const val PREF_PHYLUM = "locketPhylum"

    private val knownMonsters = sortedSetOf<Int>()
    private val OPTION = Regex("""<option value="(\d+)"""")
    private val LOCKET_PHYLUM_PREF = Regex("""pref\(locketPhylum,([^)]+)\)""")
    private val CONSTANT_MODS = setOf(
        "HP Regen Min", "HP Regen Max", "MP Regen Min", "MP Regen Max", "Single Equip",
    )

    fun clear() {
        knownMonsters.clear()
    }

    fun getMonsters(): Set<Int> = knownMonsters.toSet()

    fun rememberMonster(monsterId: Int) {
        if (monsterId > 0) knownMonsters.add(monsterId)
    }

    fun remembersMonster(monsterId: Int): Boolean = monsterId in knownMonsters

    fun foughtMonster(preferences: Preferences?, monsterId: Int): Boolean =
        monsterId in getFoughtMonsters(preferences)

    fun getFoughtMonsters(preferences: Preferences?): Set<Int> =
        preferences?.getString(PREF_FOUGHT, "")
            ?.split(',', '|')
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.filter { it > 0 }
            ?.toSet()
            .orEmpty()

    fun parseMonsters(html: String, preferences: Preferences?) {
        knownMonsters.clear()
        OPTION.findAll(html).forEach { match ->
            match.groupValues[1].toIntOrNull()?.let { knownMonsters.add(it) }
        }
        knownMonsters.addAll(getFoughtMonsters(preferences))
    }

    fun isLocketFight(html: String): Boolean =
        html.contains("loverslocketframe.png") ||
            html.contains("your locket changes to reflect", ignoreCase = true)

    fun parseFight(monsterName: String, preferences: Preferences?): Boolean {
        val monster = MonsterDatabase.getByName(monsterName) ?: return false
        addFoughtMonster(preferences, monster.id)
        rememberMonster(monster.id)
        EncounterManager.ignoreSpecialMonsters()
        val phylum = monster.phylum
        if (phylum.isNotBlank()) {
            preferences?.setString(PREF_PHYLUM, phylum)
        }
        return true
    }

    fun parseLocket(html: String, preferences: Preferences?): Boolean {
        LOCKET_PHYLUM_PREF.find(html)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let {
            preferences?.setString(PREF_PHYLUM, it)
            return true
        }
        // Fallback: first non-constant modifier line that names a known phylum.
        val indicative = PHYLA.firstOrNull { phylum ->
            html.contains(phylum, ignoreCase = true) &&
                CONSTANT_MODS.none { mod -> html.contains("$mod: $phylum", ignoreCase = true) }
        } ?: return false
        preferences?.setString(PREF_PHYLUM, indicative)
        return true
    }

    fun own(itemCount: (Int) -> Int): Boolean = itemCount(ItemPool.COMBAT_LOVERS_LOCKET) > 0

    fun onhand(inventoryCount: (Int) -> Int, equipped: Boolean): Boolean =
        inventoryCount(ItemPool.COMBAT_LOVERS_LOCKET) > 0 || equipped

    private fun addFoughtMonster(preferences: Preferences?, monsterId: Int) {
        if (monsterId <= 0 || preferences == null) return
        val fought = getFoughtMonsters(preferences).toMutableSet()
        fought.add(monsterId)
        preferences.setString(PREF_FOUGHT, fought.sorted().joinToString(","))
    }

    private val PHYLA = listOf(
        "beast", "bug", "constellation", "construct", "dude", "elemental", "elf", "fish",
        "goblin", "hippy", "hobo", "horror", "humanoid", "mer-kin", "orc", "penguin",
        "pirate", "plant", "slime", "undead", "weird",
    )
}
