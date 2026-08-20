package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [BugbearManager] mothership biodata + [FightRequest.handleKeyotron].
 */
object BugbearManager {

    data class Bugbear(
        val shipZone: String,
        val id: Int,
        val bugbear: String,
        val zones: List<String>,
        val level: Int,
        val status: String,
    )

    val BUGBEAR_DATA = listOf(
        Bugbear("Medbay", 1, "hypodermic bugbear", listOf("The Spooky Forest"), 1, "statusMedbay"),
        Bugbear(
            "Waste Processing",
            2,
            "scavenger bugbear",
            listOf("The Sleazy Back Alley"),
            1,
            "statusWasteProcessing",
        ),
        Bugbear("Sonar", 3, "batbugbear", listOf("Guano Junction"), 1, "statusSonar"),
        Bugbear(
            "Science Lab",
            4,
            "bugbear scientist",
            listOf("Cobb's Knob Laboratory"),
            2,
            "statusScienceLab",
        ),
        Bugbear(
            "Morgue",
            5,
            "bugaboo",
            listOf("The Defiled Nook", "Post-Cyrpt Cemetary"),
            2,
            "statusMorgue",
        ),
        Bugbear(
            "Special Ops",
            6,
            "Black Ops Bugbear",
            listOf("Lair of the Ninja Snowmen"),
            2,
            "statusSpecialOps",
        ),
        Bugbear(
            "Engineering",
            7,
            "Battlesuit Bugbear Type",
            listOf("The Penultimate Fantasy Airship"),
            3,
            "statusEngineering",
        ),
        Bugbear(
            "Navigation",
            8,
            "ancient unspeakable bugbear",
            listOf("The Haunted Gallery"),
            3,
            "statusNavigation",
        ),
        Bugbear(
            "Galley",
            9,
            "trendy bugbear chef",
            listOf("The Battlefield (Frat Uniform)", "The Battlefield (Hippy Uniform)"),
            3,
            "statusGalley",
        ),
    )

    private val KEYOTRON_PATTERN = Regex("""key-o-tron emits (\d) short""")

    fun bugbearToData(monsterName: String): Bugbear? {
        val name = monsterName.trim()
        if (name.isBlank()) return null
        return BUGBEAR_DATA.firstOrNull { it.bugbear.equals(name, ignoreCase = true) }
    }

    fun shipZoneToData(zone: String): Bugbear? {
        val name = zone.trim()
        if (name.isBlank()) return null
        return BUGBEAR_DATA.firstOrNull { it.shipZone.equals(name, ignoreCase = true) }
    }

    fun clearShipZone(zone: String, preferences: Preferences?) {
        if (preferences == null) return
        val data = shipZoneToData(zone) ?: return
        val statusSetting = data.status
        if (preferences.getString(statusSetting, "").equals("cleared", ignoreCase = true)) return
        preferences.setString(statusSetting, "cleared")
        val level = data.level
        val allCleared = BUGBEAR_DATA
            .filter { it.level == level }
            .all { preferences.getString(it.status, "").equals("cleared", ignoreCase = true) }
        if (!allCleared) return
        preferences.setInt("mothershipProgress", level)
        if (level == 3) return
        val nextLevel = level + 1
        for (zoneData in BUGBEAR_DATA) {
            if (zoneData.level != nextLevel) continue
            if (preferences.getString(zoneData.status, "").equals("unlocked", ignoreCase = true)) {
                preferences.setString(zoneData.status, "open")
            }
        }
    }

    fun setBiodata(data: Bugbear?, count: Int, preferences: Preferences) {
        if (data == null) return
        val statusSetting = data.status
        val level = data.level
        if (count < level * 3) {
            preferences.setString(statusSetting, count.toString())
            return
        }
        val currentStatus = preferences.getString(statusSetting, "0")
        if (currentStatus.toIntOrNull() == null) return
        val currentProgress = preferences.getInt("mothershipProgress", 0)
        val newStatus = if (level == currentProgress + 1) "open" else "unlocked"
        preferences.setString(statusSetting, newStatus)
    }

    fun handleKeyotron(html: String, monsterName: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        if (!html.contains("key-o-tron")) return false
        val data = bugbearToData(monsterName)
        if (html.contains("already collected")) {
            if (data != null) setBiodata(data, data.level * 3, preferences)
            return true
        }
        val match = KEYOTRON_PATTERN.find(html) ?: return true
        val count = match.groupValues[1].toIntOrNull() ?: return true
        setBiodata(data, count, preferences)
        return true
    }
}
