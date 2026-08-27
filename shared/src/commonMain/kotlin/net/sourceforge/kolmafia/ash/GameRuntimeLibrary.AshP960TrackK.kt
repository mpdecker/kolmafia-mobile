package net.sourceforge.kolmafia.ash

/**
 * AshP960–967 Track K — Track D HTML writers.
 *
 * Sync helpers that WRITE the prefs read by Track D ASH (clanLounge, clanRumpus,
 * chateauMonster, shopInventory/shopPrices/shopLimits, _sessionItemTally,
 * _sessionResultTally).
 *
 * Phase 960: ClanLoungeVisitSync — parse clan_viplounge.php for furniture items
 * Phase 961: ClanRumpusVisitSync — parse clan_rumpus.php for installed items
 * Phase 962: ChateauVisitSync — parse place.php?whichplace=chateau for painted monster
 * Phase 963: ShopInventoryVisitSync — parse managestore.php for inventory/prices/limits
 * Phase 964: SessionItemTallySync — accumulate item gains on fight end / use
 * Phase 965: SessionResultTallySync — accumulate adventure results
 * Phase 966: SessionAdvSync — increment _sessionAdventuresUsed on adventure
 * Phase 967: processVisitResponseHooks wiring for K syncs
 */
internal fun GameRuntimeLibrary.registerAshP960TrackKBatch(scope: AshScope) {
    // Track K is pure sync — no new ASH functions. The syncs are embedded
    // in processVisitResponseHooks / AdventureManager fight-end hooks.
    // Registration here anchors the track in the build.
}

// ── Phase 960: ClanLoungeVisitSync ────────────────────────────────────────

object ClanLoungeVisitSync {
    private val LOUNGE_ITEM_REGEX = Regex("""<img[^>]+title="([^"]+)"[^>]*/?>""")

    fun parseAndWrite(html: String, prefs: net.sourceforge.kolmafia.preferences.Preferences) {
        if ("clan_viplounge.php" !in html && "\"lounge\"" !in html) return
        val items = mutableMapOf<String, Int>()
        for (match in LOUNGE_ITEM_REGEX.findAll(html)) {
            val name = match.groupValues[1]
            items[name] = (items[name] ?: 0) + 1
        }
        if (items.isNotEmpty()) {
            val serialized = items.entries.joinToString("|") { "${it.key}:${it.value}" }
            prefs.setString("clanLounge", serialized)
            if (net.sourceforge.kolmafia.clan.ClanManager.getClanId() != 0) {
                net.sourceforge.kolmafia.clan.ClanManager.clearLounge()
                net.sourceforge.kolmafia.clan.ClanManager.setLounge(items.map { it.key to it.value })
            }
        }
    }
}

// ── Phase 961: ClanRumpusVisitSync ───────────────────────────────────────

object ClanRumpusVisitSync {
    private val RUMPUS_ITEM_REGEX = Regex("""<b>([^<]+)</b>\s*\((\d+)\)""")
    private val RUMPUS_SINGLE_REGEX = Regex("""<b>([^<]+)</b>""")

    fun parseAndWrite(html: String, prefs: net.sourceforge.kolmafia.preferences.Preferences) {
        if ("clan_rumpus.php" !in html) return
        val items = mutableListOf<String>()
        for (match in RUMPUS_ITEM_REGEX.findAll(html)) {
            val name = match.groupValues[1]
            val count = match.groupValues[2]
            items.add("$name ($count)")
        }
        for (match in RUMPUS_SINGLE_REGEX.findAll(html)) {
            val name = match.groupValues[1]
            if (items.none { it.startsWith(name) }) {
                items.add(name)
            }
        }
        if (items.isNotEmpty()) {
            prefs.setString("clanRumpus", items.joinToString("|"))
            net.sourceforge.kolmafia.clan.ClanManager.setClanRumpus(items)
        }
    }
}

// ── Phase 962: ChateauVisitSync ──────────────────────────────────────────

object ChateauVisitSync {
    private val CHATEAU_MONSTER_REGEX = Regex("""painting.*?<b>([^<]+)</b>""", RegexOption.DOT_MATCHES_ALL)

    fun parseAndWrite(html: String, prefs: net.sourceforge.kolmafia.preferences.Preferences) {
        if ("whichplace=chateau" !in html && "chateau_painting" !in html) return
        val match = CHATEAU_MONSTER_REGEX.find(html) ?: return
        val monsterName = match.groupValues[1]
        prefs.setString("chateauMonster", monsterName)
    }
}

// ── Phase 963: ShopInventoryVisitSync ────────────────────────────────────

object ShopInventoryVisitSync {
    private val SHOP_ROW_REGEX = Regex(
        """<tr>.*?<b>([^<]+)</b>.*?qty=(\d+).*?price=(\d+).*?limit=(\d+)""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun parseAndWrite(html: String, prefs: net.sourceforge.kolmafia.preferences.Preferences) {
        if ("managestore.php" !in html) return
        val inventory = mutableListOf<String>()
        val prices = mutableListOf<String>()
        val limits = mutableListOf<String>()
        for (match in SHOP_ROW_REGEX.findAll(html)) {
            val name = match.groupValues[1]
            inventory.add("$name:${match.groupValues[2]}")
            prices.add("$name:${match.groupValues[3]}")
            limits.add("$name:${match.groupValues[4]}")
        }
        if (inventory.isNotEmpty()) {
            prefs.setString("shopInventory", inventory.joinToString("|"))
            prefs.setString("shopPrices", prices.joinToString("|"))
            prefs.setString("shopLimits", limits.joinToString("|"))
        }
    }
}

// ── Phase 964: SessionItemTallySync ──────────────────────────────────────

object SessionItemTallySync {
    fun addItem(itemName: String, count: Int, prefs: net.sourceforge.kolmafia.preferences.Preferences) {
        val existing = prefs.getString("_sessionItemTally", "")
        val map = parseTally(existing).toMutableMap()
        map[itemName] = (map[itemName] ?: 0) + count
        prefs.setString("_sessionItemTally", serializeTally(map))
    }

    private fun parseTally(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, Int>()
        for (entry in raw.split("|").filter { it.isNotBlank() }) {
            val sep = entry.lastIndexOf(':')
            if (sep < 0) continue
            val name = entry.substring(0, sep)
            val count = entry.substring(sep + 1).toIntOrNull() ?: 0
            map[name] = count
        }
        return map
    }

    private fun serializeTally(map: Map<String, Int>): String =
        map.entries.joinToString("|") { "${it.key}:${it.value}" }
}

// ── Phase 965: SessionResultTallySync ────────────────────────────────────

object SessionResultTallySync {
    fun addResult(label: String, count: Int, prefs: net.sourceforge.kolmafia.preferences.Preferences) {
        val existing = prefs.getString("_sessionResultTally", "")
        val map = parseTally(existing).toMutableMap()
        map[label] = (map[label] ?: 0) + count
        prefs.setString("_sessionResultTally", serializeTally(map))
    }

    private fun parseTally(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, Int>()
        for (entry in raw.split("|").filter { it.isNotBlank() }) {
            val sep = entry.lastIndexOf(':')
            if (sep < 0) continue
            val name = entry.substring(0, sep)
            val count = entry.substring(sep + 1).toIntOrNull() ?: 0
            map[name] = count
        }
        return map
    }

    private fun serializeTally(map: Map<String, Int>): String =
        map.entries.joinToString("|") { "${it.key}:${it.value}" }
}

// ── Phase 966: SessionAdvSync ────────────────────────────────────────────

object SessionAdvSync {
    fun incrementAdventures(prefs: net.sourceforge.kolmafia.preferences.Preferences, count: Int = 1) {
        val current = prefs.getInt("_sessionAdventuresUsed", 0)
        prefs.setInt("_sessionAdventuresUsed", current + count)
    }
}
