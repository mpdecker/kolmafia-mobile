package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [SpelunkyRequest] practical subset (Phases 1321–1340):
 * reset/status/fight unlocks/choice progression/upgrades.
 */
object SpelunkyRequest {

    /** Spelunky item id range (also LimitModeGates.limitItem). */
    val ITEM_IDS: IntRange = 8040..8062

    private val HP = Regex("""HP:.*?<b>(\d+)/(\d+)</b>""", RegexOption.DOT_MATCHES_ALL)
    private val TURNS = Regex(""">(\d+) turns? left""")
    private val GOLD = Regex(""">([\d,]+) gold""")
    private val BOMB = Regex(""">(\d+) bombs?""")
    private val ROPE = Regex(""">(\d+) ropes?""")
    private val KEY = Regex(""">(\d+) keys?""")
    private val BUDDY = Regex("""Buddy:.*?<b>(.*?)</b>""", RegexOption.DOT_MATCHES_ALL)
    private val GOLD_GAIN = Regex(
        """(?:goldnug\.gif|coinpurse\.gif|lolmecidol\.gif).*?<b>(\d+) Gold!</b>""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun reset(
        preferences: Preferences?,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
    ) {
        preferences ?: return
        preferences.setInt("spelunkyNextNoncombat", 0)
        preferences.setInt("spelunkySacrifices", 0)
        preferences.setString("spelunkyStatus", "")
        preferences.setInt("spelunkyWinCount", 0)
        resetItems(preferences, inventory, character)
    }

    fun resetItems(
        preferences: Preferences?,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
    ) {
        character?.let { clearSpelunkyEquipment(it) }
        inventory?.let { inv ->
            for (id in ITEM_IDS) {
                val qty = inv.state.value.items[id]?.quantity ?: 0
                if (qty > 0) inv.consumeItemLocally(id, qty)
            }
        }
    }

    private fun clearSpelunkyEquipment(character: KoLCharacter) {
        for (slot in EquipmentSlot.entries) {
            character.updateEquipment(slot, "")
        }
    }

    fun parseCharpane(
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
    ): Boolean {
        if (!html.contains(">Last Spelunk</a>")) return false
        preferences ?: return false
        var changed = false
        HP.find(html)?.let { m ->
            val cur = m.groupValues[1].toIntOrNull() ?: return@let
            val max = m.groupValues[2].toIntOrNull() ?: return@let
            character?.updateHpMp(cur, max, character.state.value.currentMp, character.state.value.maxMp)
            changed = true
        }
        TURNS.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            preferences.setInt("spelunkyTurnsLeft", it)
            character?.updateAdventuresLeft(it)
            changed = true
        }
        GOLD.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()?.let {
            preferences.setInt("spelunkyGold", it)
            changed = true
        }
        BOMB.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            preferences.setInt("spelunkyBombs", it)
            changed = true
        }
        ROPE.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            preferences.setInt("spelunkyRopes", it)
            changed = true
        }
        KEY.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            preferences.setInt("spelunkyKeys", it)
            changed = true
        }
        BUDDY.find(html)?.groupValues?.getOrNull(1)?.trim()?.let {
            preferences.setString("spelunkyBuddy", it)
            changed = true
        }
        return changed
    }

    fun parseStatus(jsonOrHtml: String, preferences: Preferences?): Boolean {
        // API status subset: reuse charpane-like markers when present
        return parseCharpane(jsonOrHtml, preferences, null)
    }

    fun parseResponse(url: String, html: String, preferences: Preferences?): Boolean {
        if (!url.contains("whichplace=spelunky") && !html.contains("spelunky")) return false
        preferences ?: return false
        var changed = false
        listOf(
            "The Jungle" to "Jungle",
            "The Ice Caves" to "Ice Caves",
            "The Temple Ruins" to "Temple Ruins",
            "Hell" to "Hell",
            "LOLmec's Lair" to "LOLmec's Lair",
        ).forEach { (log, pref) ->
            if (html.contains(log) || html.contains(pref)) {
                changed = unlock(log, pref, preferences) || changed
            }
        }
        return changed
    }

    fun wonFight(monsterName: String, html: String, preferences: Preferences?): Boolean {
        preferences ?: return false
        var changed = false
        if (html.contains("New Area Unlocked")) {
            if (html.contains("The Jungle")) changed = unlock("The Jungle", "Jungle", preferences) || changed
            if (html.contains("The Ice Caves")) changed = unlock("The Ice Caves", "Ice Caves", preferences) || changed
            if (html.contains("The Temple Ruins")) changed = unlock("The Temple Ruins", "Temple Ruins", preferences) || changed
            if (html.contains("LOLmec's Lair")) changed = unlock("LOLmec's Lair", "LOLmec's Lair", preferences) || changed
        }
        if (monsterName.equals("spider queen", ignoreCase = true)) {
            changed = spiderQueenDefeated(preferences) || changed
        }
        if (!monsterName.equals("shopkeeper", ignoreCase = true) &&
            !monsterName.equals("ghost (Spelunky)", ignoreCase = true)
        ) {
            incrementWinCount(preferences)
            changed = true
        }
        return changed
    }

    fun spiderQueenDefeated(preferences: Preferences): Boolean {
        val status = preferences.getString("spelunkyStatus", "")
        if (status.contains("Sticky Bombs")) return false
        preferences.setString(
            "spelunkyStatus",
            if (status.isBlank()) "Sticky Bombs" else "$status, Sticky Bombs",
        )
        return true
    }

    fun incrementNonCombatPhase(preferences: Preferences) {
        val next = preferences.getInt("spelunkyNextNoncombat", 0) + 1
        preferences.setInt("spelunkyNextNoncombat", if (next > 3) 1 else next)
    }

    fun incrementWinCount(preferences: Preferences) {
        val wins = preferences.getInt("spelunkyWinCount", 0) + 1
        if (wins == 6) {
            incrementNonCombatPhase(preferences)
            preferences.setInt("spelunkyWinCount", 3)
        } else {
            preferences.setInt("spelunkyWinCount", wins)
        }
    }

    fun parseChoice(
        choice: Int,
        html: String,
        decision: Int,
        preferences: Preferences?,
    ): Boolean {
        preferences ?: return false
        var changed = false
        if (choice != 1040 && choice != 1041) {
            val wins = preferences.getInt("spelunkyWinCount", 0)
            preferences.setInt("spelunkyWinCount", (wins - 3).coerceAtLeast(0))
            if (choice != 1028 || decision >= 5) {
                incrementNonCombatPhase(preferences)
            }
            changed = true
        }
        when (choice) {
            1030 -> {
                if (html.contains("The Spider Hole")) changed = unlock("The Spider Hole", "Spider Hole", preferences) || changed
                if (html.contains("The Snake Pit")) changed = unlock("The Snake Pit", "Snake Pit", preferences) || changed
            }
            1032 -> {
                if (html.contains("The Ancient Burial Ground")) {
                    changed = unlock("The Ancient Burial Ground", "Burial Ground", preferences) || changed
                }
                if (html.contains("The Beehive")) changed = unlock("The Beehive", "Beehive", preferences) || changed
            }
            1034 -> {
                if (html.contains("An Ancient Altar")) changed = unlock("An Ancient Altar", "Altar", preferences) || changed
                if (html.contains("The Crashed U. F. O.")) {
                    changed = unlock("The Crashed U. F. O.", "Crashed UFO", preferences) || changed
                }
            }
            1037 -> {
                if (html.contains("The City of Goooold")) {
                    changed = unlock("The City of Goooold", "City of Goooold", preferences) || changed
                }
            }
            1041 -> {
                if (decision == 1) {
                    preferences.setInt(
                        "spelunkySacrifices",
                        preferences.getInt("spelunkySacrifices", 0) + 1,
                    )
                    changed = true
                }
            }
            1042 -> {
                upgrade(decision, preferences)
                changed = true
            }
            1044 -> {
                if (html.contains("Hell") || decision > 0) {
                    changed = unlock("Hell", "Hell", preferences) || changed
                }
            }
        }
        gainGold(html, preferences)
        return changed
    }

    fun unlock(logLocation: String, prefLocation: String, preferences: Preferences): Boolean {
        val status = preferences.getString("spelunkyStatus", "")
        if (status.contains(prefLocation)) return false
        preferences.setString(
            "spelunkyStatus",
            if (status.isBlank()) prefLocation else "$status, $prefLocation",
        )
        return true
    }

    fun upgrade(choice: Int, preferences: Preferences) {
        var upgrades = preferences.getString("spelunkyUpgrades", "")
        if (upgrades.length < 9) {
            upgrades = upgrades.padEnd(9, 'N')
        }
        if (choice in 1..9 && upgrades != "YYYYYYYYY") {
            val chars = upgrades.toCharArray()
            chars[choice - 1] = 'Y'
            preferences.setString("spelunkyUpgrades", String(chars))
        }
    }

    fun gainGold(html: String, preferences: Preferences?): Int {
        preferences ?: return 0
        var total = 0
        GOLD_GAIN.findAll(html).forEach { m ->
            total += m.groupValues[1].toIntOrNull() ?: 0
        }
        if (total > 0) {
            preferences.setInt("spelunkyGold", preferences.getInt("spelunkyGold", 0) + total)
        }
        return total
    }

    fun getGold(preferences: Preferences?): Int = preferences?.getInt("spelunkyGold", 0) ?: 0
    fun getBombs(preferences: Preferences?): Int = preferences?.getInt("spelunkyBombs", 0) ?: 0
    fun getRopes(preferences: Preferences?): Int = preferences?.getInt("spelunkyRopes", 0) ?: 0
    fun getKeys(preferences: Preferences?): Int = preferences?.getInt("spelunkyKeys", 0) ?: 0
    fun getTurnsLeft(preferences: Preferences?): Int = preferences?.getInt("spelunkyTurnsLeft", 0) ?: 0
    fun getBuddyName(preferences: Preferences?): String =
        preferences?.getString("spelunkyBuddy", "") ?: ""

    /** Choice 993 enter Tales of Spelunking. */
    const val ENTER_CHOICE = 993
    /** Choice 1027 exit / leave. */
    const val EXIT_CHOICE = 1027
}
