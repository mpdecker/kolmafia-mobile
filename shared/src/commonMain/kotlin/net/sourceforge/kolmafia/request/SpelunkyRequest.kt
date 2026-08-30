package net.sourceforge.kolmafia.request

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [SpelunkyRequest] practical subset (Phases 1321–1340):
 * reset/status/fight unlocks/choice progression/upgrades.
 */
object SpelunkyRequest {

    /** Spelunky item id range (also LimitModeGates.limitItem). */
    val ITEM_IDS: IntRange = 8040..8062

    private val MUSCLE = Regex("""Mus:</td><td>(?:<font color=blue>)?<b>(\d+)(?:</font> \((\d+)\))?</b>""")
    private val MOXIE = Regex("""Mox:</td><td>(?:<font color=blue>)?<b>(\d+)(?:</font> \((\d+)\))?</b>""")
    private val HP = Regex("""HP:.*?<b>(\d+)\s*/\s*(\d+)(?:</b>)?""", RegexOption.DOT_MATCHES_ALL)
    private val TURNS = Regex("""(?:Ghost'>(?:</a>)?<br><b>(\d+)</b>|>(\d+) turns? left)""")
    private val GOLD = Regex("""(?:Gold: <b>\$([\d,]+)</b>|>([\d,]+) gold)""")
    private val BOMB = Regex("""(?:Bombs'[^>]*>.*?<b>(\d+)</b>|>(\d+) bombs?)""", RegexOption.DOT_MATCHES_ALL)
    private val ROPE = Regex("""(?:Ropes'[^>]*>.*?<b>(\d+)</b>|>(\d+) ropes?)""", RegexOption.DOT_MATCHES_ALL)
    private val KEY = Regex("""(?:Keys'[^>]*>.*?<b>(\d+)</b>|>(\d+) keys?)""", RegexOption.DOT_MATCHES_ALL)
    private val BUDDY = Regex("""(?:Buddy:</b.*?alt='(.*?)' |Buddy:.*?<b>(.*?)</b>)""", RegexOption.DOT_MATCHES_ALL)
    private val STATUS_NUMBER = Regex("""(?:^|, )%s: ([\d,]+)""")
    private val STATUS_BUDDY = Regex("""(?:^|, )Buddy: (.*?)(?:, Unlocks:|$)""")
    private val STATUS_UNLOCKS = Regex("""(?:^|, )Unlocks: (.*)$""")
    private val GEAR_SECTION = Regex("""Gear:</b(.*?)</table>""", RegexOption.DOT_MATCHES_ALL)
    private val EQUIPMENT = Regex("""descitem\((\d+)\)""")
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
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        if (!html.contains(">Last Spelunk</a>")) return false
        preferences ?: return false
        var changed = false
        MUSCLE.find(html)?.let { m ->
            preferences.setInt("spelunkyMuscle", m.groupValues[1].toInt())
            preferences.setInt("spelunkyBaseMuscle", m.groupValues[2].toIntOrNull() ?: m.groupValues[1].toInt())
            changed = true
        }
        MOXIE.find(html)?.let { m ->
            preferences.setInt("spelunkyMoxie", m.groupValues[1].toInt())
            preferences.setInt("spelunkyBaseMoxie", m.groupValues[2].toIntOrNull() ?: m.groupValues[1].toInt())
            changed = true
        }
        HP.find(html)?.let { m ->
            val cur = m.groupValues[1].toIntOrNull() ?: return@let
            val max = m.groupValues[2].toIntOrNull() ?: return@let
            character?.updateHpMp(cur, max, character.state.value.currentMp, character.state.value.maxMp)
            changed = true
        }
        TURNS.find(html)?.firstNumber()?.let {
            preferences.setInt("spelunkyTurnsLeft", it)
            character?.updateAdventuresLeft(it)
            changed = true
        }
        GOLD.find(html)?.firstNumber()?.let {
            preferences.setInt("spelunkyGold", it)
            changed = true
        }
        BOMB.find(html)?.firstNumber()?.let {
            preferences.setInt("spelunkyBombs", it)
            changed = true
        }
        ROPE.find(html)?.firstNumber()?.let {
            preferences.setInt("spelunkyRopes", it)
            changed = true
        }
        KEY.find(html)?.firstNumber()?.let {
            preferences.setInt("spelunkyKeys", it)
            changed = true
        }
        BUDDY.find(html)?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }?.trim()?.let {
            val oldBuddy = getBuddyName(preferences)
            preferences.setString("spelunkyBuddy", it)
            if (it.isNotEmpty() && it != oldBuddy && getTurnsLeft(preferences) != 40) {
                sessionLogger?.appendRawLine("You have found a new Buddy, $it")
            }
            changed = true
        }
        parseGear(html, preferences, character)
        val unlocks = linkedSetOf<String>().apply {
            addAll(statusUnlocks(preferences))
            if (html.contains("'Sticky Bombs'")) add("Sticky Bombs")
        }
        val due = html.contains("spelghostarms.gif")
        rebuildStatus(preferences, due, unlocks)
        inferTurnFortyUpgrades(html, preferences)
        return changed
    }

    fun parseStatus(jsonOrHtml: String, preferences: Preferences?): Boolean {
        preferences ?: return false
        if (!jsonOrHtml.trimStart().startsWith("{")) return parseCharpane(jsonOrHtml, preferences, null)
        val root = runCatching { Json.parseToJsonElement(jsonOrHtml).jsonObject }.getOrNull() ?: return false
        val spelunky = root["spelunky"] as? JsonObject
        spelunky?.int("turns")?.let { preferences.setInt("spelunkyTurnsLeft", it) }
        spelunky?.int("gold")?.let { preferences.setInt("spelunkyGold", it) }
        spelunky?.int("bombs")?.let { preferences.setInt("spelunkyBombs", it) }
        spelunky?.int("ropes")?.let { preferences.setInt("spelunkyRopes", it) }
        spelunky?.int("keys")?.let { preferences.setInt("spelunkyKeys", it) }
        spelunky?.get("buddy")?.jsonPrimitive?.content?.let { preferences.setString("spelunkyBuddy", it) }
        val equipment = root["equipment"] as? JsonObject
        equipment?.forEach { (slot, element) ->
            if (slot != "fakehands") {
                element.jsonPrimitive.intOrNull?.let { setEquipment(slot, it, preferences, null) }
            }
        }
        rebuildStatus(preferences, spelunky?.get("noncombatDue")?.jsonPrimitive?.content == "true", statusUnlocks(preferences))
        return spelunky != null || equipment != null
    }

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        if (!url.contains("whichplace=spelunky") && !html.contains("spelunky")) return false
        preferences ?: return false
        var changed = false
        UNLOCKS.forEach { unlock ->
            if (html.contains(unlock.display) || html.contains("spelunky/${unlock.image}")) {
                val log = unlock.logName
                val pref = unlock.statusName
                changed = unlock(log, pref, preferences) { sessionLogger?.appendRawLine(it) } || changed
            }
        }
        inferTurnFortyUpgrades(html, preferences)
        return changed
    }

    fun wonFight(
        monsterName: String,
        html: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        preferences ?: return false
        var changed = false
        if (html.contains("New Area Unlocked")) {
            if (html.contains("The Jungle")) changed = unlock("The Jungle", "Jungle", preferences) { sessionLogger?.appendRawLine(it) } || changed
            if (html.contains("The Ice Caves")) changed = unlock("The Ice Caves", "Ice Caves", preferences) { sessionLogger?.appendRawLine(it) } || changed
            if (html.contains("The Temple Ruins")) changed = unlock("The Temple Ruins", "Temple Ruins", preferences) { sessionLogger?.appendRawLine(it) } || changed
            if (html.contains("LOLmec's Lair")) changed = unlock("LOLmec's Lair", "LOLmec's Lair", preferences) { sessionLogger?.appendRawLine(it) } || changed
        }
        if (monsterName.equals("spider queen", ignoreCase = true)) {
            changed = spiderQueenDefeated(preferences) { sessionLogger?.appendRawLine(it) } || changed
        }
        if (!monsterName.equals("shopkeeper", ignoreCase = true) &&
            !monsterName.equals("ghost (Spelunky)", ignoreCase = true)
        ) {
            incrementWinCount(preferences)
            changed = true
        }
        return changed
    }

    fun spiderQueenDefeated(preferences: Preferences, log: (String) -> Unit = {}): Boolean {
        return unlock("Sticky Bombs", "Sticky Bombs", preferences, log)
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
        log: (String) -> Unit = {},
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
                    log("You have sacrificed your Buddy, ${getBuddyName(preferences)}")
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
        gainGold(html, preferences, log)
        return changed
    }

    fun unlock(
        logLocation: String,
        prefLocation: String,
        preferences: Preferences,
        log: (String) -> Unit = {},
    ): Boolean {
        val unlocks = statusUnlocks(preferences)
        if (!unlocks.add(prefLocation)) return false
        rebuildStatus(preferences, spelunkyNoncombatDue(preferences), unlocks)
        log("You have unlocked $logLocation")
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

    fun gainGold(html: String, preferences: Preferences?, log: (String) -> Unit = {}): Int {
        preferences ?: return 0
        var total = 0
        GOLD_GAIN.findAll(html).forEach { m ->
            total += m.groupValues[1].toIntOrNull() ?: 0
        }
        if (total > 0) {
            preferences.setInt("spelunkyGold", preferences.getInt("spelunkyGold", 0) + total)
            log("You gain $total gold")
        }
        return total
    }

    fun getGold(preferences: Preferences?): Int = statusInt(preferences, "Gold", "spelunkyGold")
    fun getBombs(preferences: Preferences?): Int = statusInt(preferences, "Bombs", "spelunkyBombs")
    fun getRopes(preferences: Preferences?): Int = statusInt(preferences, "Ropes", "spelunkyRopes")
    fun getKeys(preferences: Preferences?): Int = statusInt(preferences, "Keys", "spelunkyKeys")
    fun getTurnsLeft(preferences: Preferences?): Int = statusInt(preferences, "Turns", "spelunkyTurnsLeft")
    fun getBuddyName(preferences: Preferences?): String =
        preferences?.getString("spelunkyStatus", "")?.let {
            STATUS_BUDDY.find(it)?.groupValues?.get(1)
        } ?: preferences?.getString("spelunkyBuddy", "") ?: ""

    fun spelunkyNoncombatDue(preferences: Preferences?): Boolean =
        preferences?.getString("spelunkyStatus", "")?.contains("Non-combat Due") == true ||
            (preferences?.getInt("spelunkyWinCount", 0) ?: 0) >= 3

    fun registerRequest(url: String, preferences: Preferences?, sessionLogger: SessionLogger? = null): String? {
        preferences ?: return null
        val action = Regex("""(?:[?&])action=([^&]+)""").find(url)?.groupValues?.get(1) ?: return ""
        val location = when (action) {
            "spelunky_camp" -> if (url.contains("ghostyghostghost=clown")) "Base Camp" else "Rest at Base Camp"
            "spelunky_side6" -> "The Altar"
            "spelunky_quit" -> "Exit"
            "spelunky_board" -> return ""
            else -> return null
        }
        return "{${getTurnsLeft(preferences)}} $location".also { sessionLogger?.appendRawLine(it) }
    }

    fun logShop(html: String, decision: Int, preferences: Preferences?, sessionLogger: SessionLogger? = null): String? {
        preferences ?: return null
        val choices = Regex("""name=["']?option["']? value=["']?(\d+)["']?[^>]*>.*?value=["'](.*?)["']""", RegexOption.DOT_MATCHES_ALL)
        var selected: String? = null
        choices.findAll(html).forEach { match ->
            val option = match.groupValues[1].toIntOrNull() ?: return@forEach
            if (option == decision && option != 6) {
                selected = "Buying " + match.groupValues[2].removePrefix("Buy ").trim()
            }
            if (option == 4) upgrade(5, preferences)
        }
        selected?.let { sessionLogger?.appendRawLine(it) }
        return selected
    }

    private fun parseGear(html: String, preferences: Preferences, character: KoLCharacter?) {
        val gear = GEAR_SECTION.find(html)?.groupValues?.getOrNull(1) ?: return
        EQUIPMENT.findAll(gear)
            .mapNotNull { ItemDatabase.getByDescId(it.groupValues[1])?.id }
            .forEach { itemId ->
            val slot = when (itemId) {
                8040, 8045, 8046, 8047, 8058, 8059 -> "weapon"
                8041, 8042, 8043, 8055, 8056, 8060, 8062 -> "offhand"
                8044, 8049, 8050, 8061 -> "hat"
                8051, 8052 -> "container"
                8053, 8054, 8057 -> "acc1"
                else -> return@forEach
            }
            setEquipment(slot, itemId, preferences, character)
        }
    }

    private fun setEquipment(slotName: String, itemId: Int, preferences: Preferences, character: KoLCharacter?) {
        val slot = when (slotName.lowercase()) {
            "hat" -> EquipmentSlot.HAT
            "weapon" -> EquipmentSlot.WEAPON
            "offhand" -> EquipmentSlot.OFFHAND
            "container", "back" -> EquipmentSlot.CONTAINER
            "acc1", "accessory1" -> EquipmentSlot.ACC1
            else -> return
        }
        preferences.setInt("spelunkyEquipment${slot.name.lowercase().replaceFirstChar { it.uppercase() }}", itemId)
        character?.updateEquipment(slot, ItemDatabase.getById(itemId)?.name ?: itemId.toString())
        if (slot == EquipmentSlot.OFFHAND) {
            preferences.setString(
                "spelunkyThrowSkill",
                when (itemId) {
                    8041 -> "throw_skull"
                    8042 -> "throw_rock"
                    8043 -> "throw_pot"
                    8056 -> "throw_torch"
                    else -> ""
                },
            )
        }
    }

    private fun rebuildStatus(preferences: Preferences, due: Boolean, unlocks: Collection<String>) {
        val turns = getTurnsLeft(preferences)
        val gold = getGold(preferences)
        val bombs = getBombs(preferences)
        val ropes = getRopes(preferences)
        val keys = getKeys(preferences)
        val buddy = getBuddyName(preferences)
        preferences.setInt("spelunkyTurnsLeft", turns)
        preferences.setInt("spelunkyGold", gold)
        preferences.setInt("spelunkyBombs", bombs)
        preferences.setInt("spelunkyRopes", ropes)
        preferences.setInt("spelunkyKeys", keys)
        preferences.setString("spelunkyBuddy", buddy)
        preferences.setString(
            "spelunkyStatus",
            buildString {
                append("Turns: $turns")
                if (due) append(", Non-combat Due")
                append(", Gold: $gold")
                append(", Bombs: $bombs")
                append(", Ropes: $ropes")
                append(", Keys: $keys")
                append(", Buddy: $buddy")
                append(", Unlocks: ${unlocks.joinToString(", ")}")
            },
        )
    }

    private fun statusUnlocks(preferences: Preferences): LinkedHashSet<String> =
        LinkedHashSet(
            STATUS_UNLOCKS.find(preferences.getString("spelunkyStatus", ""))
                ?.groupValues?.get(1)?.split(", ")?.filter { it.isNotBlank() }.orEmpty(),
        )

    private fun statusInt(preferences: Preferences?, label: String, legacy: String): Int {
        preferences ?: return 0
        val pattern = Regex(STATUS_NUMBER.pattern.replace("%s", Regex.escape(label)))
        return pattern.find(preferences.getString("spelunkyStatus", ""))?.groupValues?.get(1)
            ?.replace(",", "")?.toIntOrNull() ?: preferences.getInt(legacy, 0)
    }

    private fun inferTurnFortyUpgrades(html: String, preferences: Preferences) {
        if (getTurnsLeft(preferences) != 40) return
        var value = preferences.getString("spelunkyUpgrades", "NNNNNNNNN").padEnd(9, 'N').take(9)
        val chars = value.toCharArray()
        if (statusUnlocks(preferences).contains("Ice Caves")) { chars[0] = 'Y'; chars[1] = 'Y' }
        else if (statusUnlocks(preferences).contains("Jungle")) chars[0] = 'Y'
        if (getGold(preferences) == 100) for (i in 3..5) chars[i] = 'Y'
        else if (getBombs(preferences) == 3) chars[3] = 'Y'
        if (getKeys(preferences) == 1) for (i in 6..8) chars[i] = 'Y'
        else if (html.contains("hobofedora.gif")) { chars[6] = 'Y'; chars[7] = 'Y' }
        else if (getRopes(preferences) == 3) chars[6] = 'Y'
        value = String(chars)
        preferences.setString("spelunkyUpgrades", value)
    }

    private fun MatchResult.firstNumber(): Int? =
        groupValues.drop(1).firstOrNull { it.isNotBlank() }?.replace(",", "")?.toIntOrNull()

    private fun JsonObject.int(name: String): Int? = get(name)?.jsonPrimitive?.intOrNull

    private data class Unlock(val display: String, val statusName: String, val image: String, val logName: String = display)
    private val UNLOCKS = listOf(
        Unlock("The Jungle", "Jungle", "jungle.gif"),
        Unlock("The Ice Caves", "Ice Caves", "icecaves.gif"),
        Unlock("The Temple Ruins", "Temple Ruins", "templeruins.gif"),
        Unlock("The Snake Pit", "Snake Pit", "snakepit.gif"),
        Unlock("The Spider Hole", "Spider Hole", "spiderhole.gif"),
        Unlock("The Ancient Burial Ground", "Burial Ground", "burialground.gif"),
        Unlock("The Beehive", "Beehive", "beehive.gif"),
        Unlock("The Crashed U.F.O.", "Crashed UFO", "ufo.gif"),
        Unlock("An Ancient Altar", "Altar", "altar.gif"),
        Unlock("The City of Goooold", "City of Goooold", "citygold.gif"),
        Unlock("LOLmec's Lair", "LOLmec's Lair", "lolmec.gif"),
        Unlock("Hell", "Hell", "heckofirezzz.gif"),
        Unlock("Yomama's Throne", "Yomama's Throne", "yomama.gif"),
    )

    /** Choice 993 enter Tales of Spelunking. */
    const val ENTER_CHOICE = 993
    /** Choice 1027 exit / leave. */
    const val EXIT_CHOICE = 1027
}
