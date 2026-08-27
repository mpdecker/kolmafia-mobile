package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.skill.SkillData
import net.sourceforge.kolmafia.skill.SkillManager

/**
 * Desktop [BatManager] engine subset (Phases 1341–1360):
 * begin/end, upgrades/stats, zone tracking, charpane, fight bonuses.
 */
object BatManager {

    const val BAT_OOMERANG = 8797
    const val BAT_JUTE = 8798
    const val BAT_O_MITE = 8799

    val ITEM_IDS: IntRange = 8797..8815

    const val BAT_PUNCH = 7255
    const val BAT_KICK = 7256
    const val SKILL_BAT_OOMERANG = 7257
    const val SKILL_BAT_JUTE = 7258
    const val SKILL_BAT_O_MITE = 7259
    const val ULTRACOAGULATOR = 7260
    const val KICKBALL = 7261
    const val BAT_GLUE = 7262
    const val BAT_BEARING = 7263
    const val USE_BAT_AID = 7264

    private val COMBAT_SKILL_IDS = listOf(
        BAT_PUNCH, BAT_KICK, SKILL_BAT_OOMERANG, SKILL_BAT_JUTE, SKILL_BAT_O_MITE,
        ULTRACOAGULATOR, KICKBALL, BAT_GLUE, BAT_BEARING, USE_BAT_AID,
    )

    const val GOTPORK_CITY = "Somewhere in Gotpork City"
    const val BAT_CAVERN = "Bat-Cavern"
    const val CENTER_PARK = "Center Park (Low Crime)"
    const val SLUMS = "Slums (Moderate Crime)"
    const val INDUSTRIAL_DISTRICT = "Industrial District (High Crime)"
    const val DOWNTOWN = "Downtown"

    data class BatUpgrade(val option: Int, val name: String)

    private val SUIT_UPGRADES = listOf(
        BatUpgrade(1, "Hardened Knuckles"),
        BatUpgrade(2, "Steel-Toed Bat-Boots"),
        BatUpgrade(3, "Extra-Swishy Cloak"),
        BatUpgrade(4, "Pec-Guards"),
        BatUpgrade(5, "Kevlar Undergarments"),
        BatUpgrade(6, "Improved Cowl Optics"),
        BatUpgrade(7, "Asbestos Lining"),
        BatUpgrade(8, "Utility Belt First Aid Kit"),
    )
    private val SEDAN_UPGRADES = listOf(
        BatUpgrade(1, "Rocket Booster"),
        BatUpgrade(2, "Glove Compartment First-Aid Kit"),
        BatUpgrade(3, "Street Sweeper"),
        BatUpgrade(4, "Advanced Air Filter"),
        BatUpgrade(5, "Orphan Scoop"),
        BatUpgrade(6, "Spotlight"),
        BatUpgrade(7, "Bat-Freshener"),
        BatUpgrade(8, "Loose Bearings"),
    )
    private val CAVERN_UPGRADES = listOf(
        BatUpgrade(1, "Really Long Winch"),
        BatUpgrade(2, "Improved 3-D Bat-Printer"),
        BatUpgrade(3, "Transfusion Satellite"),
        BatUpgrade(4, "Surveillance Network"),
        BatUpgrade(5, "Blueprints Database"),
        BatUpgrade(7, "Snugglybear Nightlight"),
        BatUpgrade(8, "Glue Factory"),
    )

    private val upgrades = linkedSetOf<String>()
    private val stats = linkedMapOf<String, Int>()
    private var batMinutes: Int = 0
    private var dwayneCoFunds: Int = 0
    private var dwayneCoBonusFunds: Int = 0
    private var batZone: String = GOTPORK_CITY

    private val TIME = Regex(
        """Time until Gotpork City explodes.*?(?:(\d+)\s*h\.\s*)?(\d+)\s*m\.""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val FUNDS = Regex("""DwayneCo funds.*?(\d+)\s*bn\.""", RegexOption.DOT_MATCHES_ALL)
    private val BAT_HP = Regex("""Bat-Health.*?<b>(\d+)/(\d+)</b>""", RegexOption.DOT_MATCHES_ALL)

    fun resetForTest() {
        upgrades.clear()
        stats.clear()
        batMinutes = 0
        dwayneCoFunds = 0
        dwayneCoBonusFunds = 0
        batZone = GOTPORK_CITY
    }

    fun begin(
        preferences: Preferences?,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
        skillManager: SkillManager? = null,
    ) {
        preferences ?: return
        resetInternal(active = true, preferences, inventory)
        inventory?.gainItemLocally(BAT_OOMERANG, 1)
        inventory?.gainItemLocally(BAT_JUTE, 1)
        inventory?.gainItemLocally(BAT_O_MITE, 1)
        preferences.setInt("batmanTimeLeft", 600)
        batMinutes = 600
        dwayneCoBonusFunds = preferences.getInt("batmanBonusInitialFunds", 0)
        dwayneCoFunds = 3 + dwayneCoBonusFunds
        preferences.setInt("batmanFundsAvailable", dwayneCoFunds)
        setBatZone(BAT_CAVERN, preferences)
        setCombatSkills(skillManager)
    }

    fun end(
        preferences: Preferences?,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
        skillManager: SkillManager? = null,
    ) {
        preferences ?: return
        resetInternal(active = false, preferences, inventory)
        clearCombatSkills(skillManager)
    }

    private fun resetInternal(
        active: Boolean,
        preferences: Preferences,
        inventory: InventoryManager?,
    ) {
        preferences.setInt("batmanTimeLeft", 0)
        batMinutes = 0
        preferences.setInt("batmanFundsAvailable", 0)
        dwayneCoFunds = 0
        dwayneCoBonusFunds = preferences.getInt("batmanBonusInitialFunds", 0)
        preferences.setString("batmanStats", "")
        stats.clear()
        if (active) {
            stats["Bat-Health"] = 30
            stats["Maximum Bat-Health"] = 30
            stats["Bat-Punch"] = 5
            stats["Bat-Kick"] = 5
            stats["Bat-Investigation Progress"] = 3
        }
        preferences.setString("batmanUpgrades", "")
        upgrades.clear()
        resetItems(inventory)
        setBatZone(GOTPORK_CITY, preferences)
        writeStatsPref(preferences)
    }

    private fun resetItems(inventory: InventoryManager?) {
        inventory ?: return
        for (id in ITEM_IDS) {
            if (id == 8800) continue
            val qty = inventory.state.value.items[id]?.quantity ?: 0
            if (qty > 0) inventory.consumeItemLocally(id, qty)
        }
    }

    fun setCombatSkills(skillManager: SkillManager?) {
        skillManager ?: return
        for (id in COMBAT_SKILL_IDS) {
            skillManager.learnLocalSkill(
                SkillData(
                    id = id,
                    name = "Bat skill $id",
                    type = net.sourceforge.kolmafia.skill.SkillType.COMBAT,
                    mpCost = 0,
                    dailyLimit = 0,
                    timesCast = 0,
                ),
            )
        }
    }

    fun clearCombatSkills(skillManager: SkillManager?) {
        skillManager ?: return
        for (id in COMBAT_SKILL_IDS) {
            skillManager.forgetLocalSkill(id)
        }
    }

    fun hasUpgrade(name: String): Boolean =
        upgrades.any { it.equals(name, ignoreCase = true) }

    fun batSuitUpgrade(option: Int, preferences: Preferences?): Boolean {
        val upgrade = SUIT_UPGRADES.firstOrNull { it.option == option } ?: return false
        if (!addUpgrade(upgrade, preferences)) return false
        when (upgrade.name) {
            "Hardened Knuckles" -> setStat("Bat-Punch Multiplier", 2, preferences)
            "Steel-Toed Bat-Boots" -> setStat("Bat-Kick Multiplier", 2, preferences)
            "Pec-Guards" -> incrementStat("Bat-Armor", 3, preferences)
            "Kevlar Undergarments" -> incrementStat("Bat-Bulletproofing", 3, preferences)
            "Asbestos Lining" -> incrementStat("Bat-Heat Resistance", 50, preferences)
        }
        return true
    }

    fun batSedanUpgrade(option: Int, preferences: Preferences?): Boolean {
        val upgrade = SEDAN_UPGRADES.firstOrNull { it.option == option } ?: return false
        if (!addUpgrade(upgrade, preferences)) return false
        when (upgrade.name) {
            "Spotlight" -> incrementStat("Bat-Investigation Progress", 1, preferences)
            "Bat-Freshener" -> incrementStat("Bat-Stench Resistance", 50, preferences)
        }
        return true
    }

    fun batCavernUpgrade(option: Int, preferences: Preferences?): Boolean {
        val upgrade = CAVERN_UPGRADES.firstOrNull { it.option == option } ?: return false
        if (!addUpgrade(upgrade, preferences)) return false
        when (upgrade.name) {
            "Snugglybear Nightlight" -> incrementStat("Bat-Spooky Resistance", 50, preferences)
            "Blueprints Database" -> incrementStat("Bat-Investigation Progress", 1, preferences)
            "Transfusion Satellite" -> incrementStat("Bat-Health Regeneration", 5, preferences)
        }
        return true
    }

    private fun addUpgrade(upgrade: BatUpgrade, preferences: Preferences?): Boolean {
        if (hasUpgrade(upgrade.name)) return false
        upgrades.add(upgrade.name)
        preferences?.setString("batmanUpgrades", upgrades.joinToString(";"))
        val funds = preferences?.getInt("batmanFundsAvailable", dwayneCoFunds) ?: dwayneCoFunds
        if (funds > 0) {
            dwayneCoFunds = funds - 1
            preferences?.setInt("batmanFundsAvailable", dwayneCoFunds)
        }
        return true
    }

    fun restoreUpgradesFromPref(preferences: Preferences?) {
        upgrades.clear()
        preferences?.getString("batmanUpgrades", "")
            ?.split(';')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.forEach { upgrades.add(it) }
        dwayneCoFunds = preferences?.getInt("batmanFundsAvailable", 0) ?: 0
    }

    fun setStat(name: String, value: Int, preferences: Preferences?) {
        stats[name] = value
        writeStatsPref(preferences)
    }

    fun incrementStat(name: String, delta: Int, preferences: Preferences?) {
        stats[name] = (stats[name] ?: 0) + delta
        writeStatsPref(preferences)
    }

    fun getStat(name: String): Int = stats[name] ?: 0

    private fun writeStatsPref(preferences: Preferences?) {
        preferences ?: return
        preferences.setString(
            "batmanStats",
            stats.entries.joinToString(";") { "${it.key}=${it.value}" },
        )
    }

    fun setBatZone(zone: String, preferences: Preferences?) {
        batZone = zone
        preferences?.setString("batmanZone", zone)
    }

    fun currentBatZone(): String = batZone

    fun placeToBatZone(place: String): String = when {
        place.contains("batman_cave") -> BAT_CAVERN
        place.contains("batman_downtown") -> DOWNTOWN
        place.contains("batman_park") -> CENTER_PARK
        place.contains("batman_slums") -> SLUMS
        place.contains("batman_industrial") -> INDUSTRIAL_DISTRICT
        else -> GOTPORK_CITY
    }

    fun parsePlaceResponse(url: String, preferences: Preferences?): Boolean {
        if (!url.contains("batman_")) return false
        setBatZone(placeToBatZone(url), preferences)
        return true
    }

    fun parseTopMenu(html: String, preferences: Preferences?): Boolean {
        val m = Regex("""whichplace=([a-z0-9_]+)""").find(html) ?: return false
        setBatZone(placeToBatZone(m.groupValues[1]), preferences)
        return true
    }

    fun parseBatSedan(html: String, decision: Int, preferences: Preferences?): Boolean {
        // Choice 1135 — zone map buttons; decision maps to crime district
        val zone = when (decision) {
            1 -> CENTER_PARK
            2 -> SLUMS
            3 -> INDUSTRIAL_DISTRICT
            4 -> DOWNTOWN
            5 -> BAT_CAVERN
            else -> return false
        }
        setBatZone(zone, preferences)
        return true
    }

    fun parseCharpane(html: String, preferences: Preferences?, character: KoLCharacter? = null): Boolean {
        if (!html.contains("You're Batfellow") && !html.contains("Gotpork City explodes")) {
            return false
        }
        preferences ?: return false
        var changed = false
        TIME.find(html)?.let { m ->
            val hours = m.groupValues[1].toIntOrNull() ?: 0
            val mins = m.groupValues[2].toIntOrNull() ?: 0
            batMinutes = hours * 60 + mins
            preferences.setInt("batmanTimeLeft", batMinutes)
            changed = true
        }
        FUNDS.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { funds ->
            dwayneCoFunds = funds
            preferences.setInt("batmanFundsAvailable", funds)
            if (funds > 3) {
                preferences.setInt("batmanBonusInitialFunds", funds - 3)
            }
            changed = true
        }
        BAT_HP.find(html)?.let { m ->
            val cur = m.groupValues[1].toIntOrNull() ?: return@let
            val max = m.groupValues[2].toIntOrNull() ?: return@let
            setStat("Bat-Health", cur, preferences)
            setStat("Maximum Bat-Health", max, preferences)
            character?.updateHpMp(cur, max, character.state.value.currentMp, character.state.value.maxMp)
            changed = true
        }
        return changed
    }

    fun changeBatHealth(delta: Int, preferences: Preferences?) {
        incrementStat("Bat-Health", delta, preferences)
    }

    fun wonFight(monsterName: String, html: String, preferences: Preferences?): Boolean {
        preferences ?: return false
        var changed = false
        when (monsterName.lowercase()) {
            "vicious plant creature" ->
                if (html.contains("(+1 Bat-Health regeneration per fight)")) {
                    incrementStat("Bat-Health Regeneration", 1, preferences)
                    changed = true
                }
            "giant mosquito" ->
                if (html.contains("(+3 Maximum Bat-Health)")) {
                    incrementStat("Maximum Bat-Health", 3, preferences)
                    changed = true
                }
            "walking skeleton" ->
                if (html.contains("(+1 Bat-Armor)")) {
                    incrementStat("Bat-Armor", 1, preferences)
                    changed = true
                }
            "former guard" ->
                if (html.contains("(+1 Bat-Bulletproofing)")) {
                    incrementStat("Bat-Bulletproofing", 1, preferences)
                    changed = true
                }
            "plumber's helper" ->
                if (html.contains("(+10% Bat-Stench Resistance)")) {
                    incrementStat("Bat-Stench Resistance", 10, preferences)
                    changed = true
                }
            "very [adjective] henchman" ->
                if (html.contains("(+10% Bat-Spooky Resistance)")) {
                    incrementStat("Bat-Spooky Resistance", 10, preferences)
                    changed = true
                }
            "time bandit" ->
                if (html.contains("(+10 Bat-Minutes)")) {
                    batMinutes += 10
                    preferences.setInt("batmanTimeLeft", batMinutes)
                    changed = true
                }
            "burner" ->
                if (html.contains("(+10% Bat-Heat Resistance)")) {
                    incrementStat("Bat-Heat Resistance", 10, preferences)
                    changed = true
                }
            "inquisitee" ->
                if (html.contains("(+1% Investigation Progress per fight)")) {
                    incrementStat("Bat-Investigation Progress", 1, preferences)
                    changed = true
                }
        }
        return changed
    }

    fun gainItem(itemId: Int, preferences: Preferences?): Boolean {
        return when (itemId) {
            ItemPool.EXPERIMENTAL_GENE_THERAPY -> {
                incrementStat("Maximum Bat-Health", 5, preferences)
                true
            }
            ItemPool.SELF_DEFENSE_TRAINING -> {
                incrementStat("Bat-Armor", 1, preferences)
                true
            }
            ItemPool.CONFIDENCE_BUILDING_HUG -> {
                incrementStat("Bat-Recharge", 5, preferences)
                true
            }
            else -> false
        }
    }

    fun getTimeLeft(): Int = batMinutes

    fun getTimeLeftString(): String {
        val hours = batMinutes / 60
        val mins = batMinutes % 60
        return if (hours > 0) "$hours h. $mins m." else "$mins m."
    }

    fun getFunds(): Int = dwayneCoFunds

    /** Fabricator printer upgrade halves some token costs. */
    fun hasImprovedPrinter(): Boolean = hasUpgrade("Improved 3-D Bat-Printer")
}
