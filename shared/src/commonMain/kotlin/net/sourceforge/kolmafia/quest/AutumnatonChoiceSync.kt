package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [AutumnatonManager] choice 1483 + Conspicuous Plaque choice 1484.
 */
object AutumnatonChoiceSync {

    const val AUTUMNATON_CHOICE = 1483
    const val PLAQUE_CHOICE = 1484

    const val AUTUMNATON_ITEM_ID = 10954

    private val UPGRADE = Regex("""autumnaton/(.*?)\.png""")
    private val SNARFBLAT = Regex("""heythereprogrammer=(\d+)""", RegexOption.IGNORE_CASE)
    private val PLAQUE_VISIT = Regex("""The plaque currently reads: <b>(.*?)</b>""", RegexOption.IGNORE_CASE)
    private val NAME_FIELD = Regex("""(?:^|[?&])name=([^&]*)""", RegexOption.IGNORE_CASE)

    private val UPGRADE_DESCRIPTIONS = mapOf(
        "enhanced left arm" to "leftarm1",
        "upgraded left leg" to "leftleg1",
        "high performance right arm" to "rightarm1",
        "high speed right leg" to "rightleg1",
        "energy-absorptive hat" to "base_blackhat",
        "collection prow" to "cowcatcher",
        "vision extender" to "periscope",
        "radar dish" to "radardish",
        "dual exhaust" to "dualexhaust",
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            AUTUMNATON_CHOICE -> {
                val upgrades = UPGRADE.findAll(html)
                    .map { it.groupValues[1] }
                    .filter { it != "base" && !it.endsWith("0") }
                    .sorted()
                    .joinToString(",")
                preferences.setString("autumnatonUpgrades", upgrades)
                true
            }
            PLAQUE_CHOICE -> {
                val name = PLAQUE_VISIT.find(html)?.groupValues?.getOrNull(1) ?: return false
                preferences.setString("speakeasyName", name)
                true
            }
            else -> false
        }
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        turnsPlayed: Int = 0,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
        adventureNameForSnarfblat: (Int) -> String? = { id ->
            AdventureDatabase.getBySnarfblat(id.toString())?.locationName
        },
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            AUTUMNATON_CHOICE -> applyAutumnatonPost(
                decision, html, preferences, choiceUrl, turnsPlayed, consumeItem, adventureNameForSnarfblat,
            )
            PLAQUE_CHOICE -> applyPlaquePost(decision, html, preferences, choiceUrl)
            else -> false
        }
    }

    private fun applyAutumnatonPost(
        decision: Int,
        html: String,
        preferences: Preferences,
        choiceUrl: String,
        turnsPlayed: Int,
        consumeItem: (Int, Int) -> Unit,
        adventureNameForSnarfblat: (Int) -> String?,
    ): Boolean = when (decision) {
        1 -> {
            parseUpgrade(html, preferences)
            true
        }
        2 -> {
            parseQuest(html, preferences, choiceUrl, turnsPlayed, consumeItem, adventureNameForSnarfblat)
            true
        }
        else -> false
    }

    private fun applyPlaquePost(
        decision: Int,
        html: String,
        preferences: Preferences,
        choiceUrl: String,
    ): Boolean {
        if (decision != 1) return false
        if (!html.contains("All right, you're the boss.")) return false
        val name = NAME_FIELD.find(choiceUrl)?.groupValues?.getOrNull(1)
            ?.replace('+', ' ')
            ?.let { decodeSimple(it) }
            ?: return false
        preferences.setString("speakeasyName", name)
        return true
    }

    private fun parseUpgrade(html: String, preferences: Preferences) {
        if (!html.contains("You attach")) return
        val fromText = UPGRADE_DESCRIPTIONS.entries
            .filter { html.contains(it.key) }
            .map { it.value }
        val existing = preferences.getString("autumnatonUpgrades", "")
            .split(',')
            .filter { it.isNotBlank() }
        preferences.setString(
            "autumnatonUpgrades",
            (fromText + existing).distinct().sorted().joinToString(","),
        )
    }

    private fun parseQuest(
        html: String,
        preferences: Preferences,
        choiceUrl: String,
        turnsPlayed: Int,
        consumeItem: (Int, Int) -> Unit,
        adventureNameForSnarfblat: (Int) -> String?,
    ) {
        if (!html.contains("Good luck, little buddy")) return
        consumeItem(AUTUMNATON_ITEM_ID, 1)
        val questNumber = preferences.getInt("_autumnatonQuests", 0) + 1
        preferences.setInt("_autumnatonQuests", questNumber)
        val snarfblat = SNARFBLAT.find(choiceUrl)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        adventureNameForSnarfblat(snarfblat)?.let {
            preferences.setString("autumnatonQuestLocation", it)
        }
        preferences.setInt(
            "autumnatonQuestTurn",
            turnsPlayed + calculateQuestTurns(questNumber, preferences),
        )
    }

    private fun calculateQuestTurns(questNumber: Int, preferences: Preferences): Int {
        var effectiveQuest = questNumber - 1
        val upgrades = preferences.getString("autumnatonUpgrades", "")
        if (upgrades.contains("leftleg1")) effectiveQuest--
        if (upgrades.contains("rightleg1")) effectiveQuest--
        return maxOf(1, effectiveQuest) * 11
    }

    private fun decodeSimple(raw: String): String =
        raw.replace("%20", " ").replace("%27", "'")
}
