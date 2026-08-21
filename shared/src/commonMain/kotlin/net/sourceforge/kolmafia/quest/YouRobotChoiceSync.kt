package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Pref-sync slice of desktop [YouRobotManager] for choices 1445 / 1447.
 * Does not port full YouRobot automation or CLI.
 */
object YouRobotChoiceSync {

    const val REASSEMBLY_CHOICE = 1445
    const val STATBOT_CHOICE = 1447

    private val AVATAR = Regex("""otherimages/robot/(left|right|top|bottom|body)(\d+)\.png""")
    private val CPU_INSTALLED = Regex(
        """<button.*?value="([a-z0-9_]+)"[^\(]+\(already installed\)""",
        RegexOption.IGNORE_CASE,
    )
    private val STATBOT_COST = Regex("""Current upgrade cost: <b>(\d+) energy</b>""")
    private val SHOW_FIELD = Regex("""(?:^|[?&])show=([^&]*)""", RegexOption.IGNORE_CASE)
    private val PART_FIELD = Regex("""(?:^|[?&])p=([^&]*)""", RegexOption.IGNORE_CASE)

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            REASSEMBLY_CHOICE -> {
                parseAvatar(html, preferences)
                if (choiceUrl.contains("show=cpus", ignoreCase = true) ||
                    html.contains("(already installed)")
                ) {
                    parseCpuUpgrades(html, preferences)
                }
                true
            }
            STATBOT_CHOICE -> {
                parseStatbotCost(html, preferences)
                true
            }
            else -> false
        }
    }

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            REASSEMBLY_CHOICE -> {
                val show = SHOW_FIELD.find(choiceUrl)?.groupValues?.getOrNull(1)?.lowercase().orEmpty()
                val part = PART_FIELD.find(choiceUrl)?.groupValues?.getOrNull(1).orEmpty()
                when (show) {
                    "cpus" -> {
                        if (part.isNotBlank()) {
                            val existing = preferences.getString("youRobotCPUUpgrades", "")
                                .split(',')
                                .filter { it.isNotBlank() }
                                .toMutableSet()
                            existing += part
                            preferences.setString(
                                "youRobotCPUUpgrades",
                                existing.sorted().joinToString(","),
                            )
                        }
                        parseCpuUpgrades(html, preferences)
                    }
                    "left", "right", "top", "bottom" -> {
                        val index = part.toIntOrNull()
                        if (index != null) {
                            preferences.setInt("youRobot${show.replaceFirstChar { it.uppercase() }}", index)
                        }
                        parseAvatar(html, preferences)
                    }
                    else -> parseAvatar(html, preferences)
                }
                true
            }
            STATBOT_CHOICE -> {
                parseStatbotCost(html, preferences)
                true
            }
            else -> false
        }
    }

    private fun parseAvatar(html: String, preferences: Preferences) {
        AVATAR.findAll(html).forEach { match ->
            val section = match.groupValues[1]
            val index = match.groupValues[2].toIntOrNull() ?: return@forEach
            if (section == "body") {
                preferences.setInt("youRobotBody", index)
            } else {
                preferences.setInt(
                    "youRobot${section.replaceFirstChar { it.uppercase() }}",
                    index,
                )
            }
        }
    }

    private fun parseCpuUpgrades(html: String, preferences: Preferences) {
        val keywords = CPU_INSTALLED.findAll(html).map { it.groupValues[1] }.toList()
        if (keywords.isEmpty()) return
        preferences.setString("youRobotCPUUpgrades", keywords.sorted().joinToString(","))
    }

    private fun parseStatbotCost(html: String, preferences: Preferences) {
        STATBOT_COST.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { cost ->
            preferences.setInt("statbotUses", cost - 10)
        }
    }
}
