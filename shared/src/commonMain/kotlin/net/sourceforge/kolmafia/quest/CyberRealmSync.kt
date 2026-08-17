package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleCyberRealmChange] + [QuestManager.handleServerRoom] +
 * monorail `_crToday`.
 */
object CyberRealmSync {

    const val CYBER_ZONE_1 = 585
    const val CYBER_ZONE_2 = 586
    const val CYBER_ZONE_3 = 587

    private val fileDrawerPattern = Regex(
        """<b>Owner:</b>\s*(.*?)<br>Security Level:\s*(\d)<br>Countermeasures:\s*(.*?)<br>Active Intrusion:\s*(.*?)<br>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun applyFromAdventure(
        adventureId: String?,
        html: String,
        preferences: Preferences?,
        url: String? = null,
    ): Boolean {
        if (preferences == null) return false
        if (!html.contains("You've already hacked this system.")) return false
        val area = adventureId?.toIntOrNull()
            ?: Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(url.orEmpty())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        val property = when (area) {
            CYBER_ZONE_1 -> "_cyberZone1Turns"
            CYBER_ZONE_2 -> "_cyberZone2Turns"
            CYBER_ZONE_3 -> "_cyberZone3Turns"
            else -> return false
        }
        preferences.setInt(property, 20)
        return true
    }

    fun applyFromServerRoom(url: String?, html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        val location = url.orEmpty()
        if (!location.contains("serverroom", ignoreCase = true) &&
            !location.contains("whichplace=serverroom", ignoreCase = true) &&
            !html.contains("serverroom", ignoreCase = true)
        ) {
            // Still allow action=serverroom_* on place.php
            if (!location.contains("action=serverroom", ignoreCase = true)) return false
        }
        val action = Regex("""(?:^|[?&])action=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(location)?.groupValues?.getOrNull(1)
            ?: return false
        return when (action) {
            "serverroom_drawer1", "serverroom_drawer2", "serverroom_drawer3" -> false
            "serverroom_chipdrawer" -> {
                preferences.setBoolean("cyberDatastickCollected", true)
                true
            }
            "serverroom_filedrawer" -> {
                var changed = false
                for (match in fileDrawerPattern.findAll(html)) {
                    val owner = match.groupValues[1]
                    val defense = match.groupValues[3]
                    val hacker = when (match.groupValues[4]) {
                        "redhat" -> "redhat hacker"
                        "bluehat" -> "bluehat hacker"
                        "greenhat" -> "greenhat hacker"
                        "purplehat" -> "purplehat hacker"
                        "blackhat" -> "greyhat hacker"
                        else -> match.groupValues[4]
                    }
                    val prefix = "_cyberZone${match.groupValues[2]}"
                    preferences.setString("${prefix}Owner", owner)
                    preferences.setString("${prefix}Defense", defense)
                    preferences.setString("${prefix}Hacker", hacker)
                    changed = true
                }
                changed
            }
            "serverroom_trash1", "serverroom_trash2" -> {
                preferences.setBoolean("_cyberTrashCollected", true)
                true
            }
            else -> false
        }
    }

    fun applyFromMonorail(url: String?, html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        if (url != null && !url.contains("whichplace=monorail", ignoreCase = true)) return false
        if (!html.contains("Server Room")) return false
        if (preferences.getBoolean("crAlways", false)) return false
        preferences.setBoolean("_crToday", true)
        return true
    }

    /** Desktop ChoiceControl cases 1545–1550. */
    fun applyFromChoice(choiceId: Int, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        val property = when (choiceId) {
            1545 -> "_cyberZone1Turns" to 10
            1546 -> "_cyberZone1Turns" to 20
            1547 -> "_cyberZone2Turns" to 10
            1548 -> "_cyberZone2Turns" to 20
            1549 -> "_cyberZone3Turns" to 10
            1550 -> "_cyberZone3Turns" to 20
            else -> return false
        }
        preferences.setInt(property.first, property.second)
        return true
    }
}
