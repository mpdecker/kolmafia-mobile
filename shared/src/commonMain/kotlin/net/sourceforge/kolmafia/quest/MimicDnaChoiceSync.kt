package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Mimic DNA Bank choice 1517 —
 * visit counters + post donate/extract + mimicEggMonsters map.
 */
object MimicDnaChoiceSync {

    const val CHOICE_ID = 1517

    const val MIMIC_EGG = 11542
    const val OBTAINED_PREF = "_mimicEggsObtained"
    const val DONATED_PREF = "_mimicEggsDonated"
    const val MONSTERS_PREF = "mimicEggMonsters"
    const val MAX_OBTAINED = 11
    const val MAX_DONATED = 3

    private val OBTAINED_PATTERN = Regex("""(\d+)/11 eggs spawned today""", RegexOption.IGNORE_CASE)
    private val DONATED_PATTERN = Regex("""(\d+)/3 donations made for the day""", RegexOption.IGNORE_CASE)
    private val MID_PATTERN = Regex("""mid=(\d+)""", RegexOption.IGNORE_CASE)

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        var changed = false
        OBTAINED_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            preferences.setInt(OBTAINED_PREF, it)
            changed = true
        }
        DONATED_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            preferences.setInt(DONATED_PREF, it)
            changed = true
        }
        return changed
    }

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        return when (decision) {
            1 -> {
                if (!html.contains("You donate your egg to science.")) return false
                consumeItem(MIMIC_EGG, 1)
                updateMimicMonsters(preferences, choiceUrl, -1)
                val donated = preferences.getInt(DONATED_PREF, 0)
                preferences.setInt(DONATED_PREF, (donated + 1).coerceAtMost(MAX_DONATED))
                true
            }
            2 -> when {
                html.contains("pops into a backroom") -> {
                    val obtained = preferences.getInt(OBTAINED_PREF, 0)
                    preferences.setInt(OBTAINED_PREF, (obtained + 1).coerceAtMost(MAX_OBTAINED))
                    updateMimicMonsters(preferences, choiceUrl, 1)
                    true
                }
                html.contains("can't extract") -> {
                    preferences.setInt(OBTAINED_PREF, MAX_OBTAINED)
                    true
                }
                else -> false
            }
            else -> false
        }
    }

    fun updateMimicMonsters(preferences: Preferences, urlOrMid: String, increment: Int) {
        val mid = MID_PATTERN.find(urlOrMid)?.groupValues?.getOrNull(1)
            ?: urlOrMid.toIntOrNull()?.toString()
            ?: return
        val map = parseMonsterMap(preferences.getString(MONSTERS_PREF, ""))
        map[mid] = (map[mid] ?: 0) + increment
        preferences.setString(
            MONSTERS_PREF,
            map.filterValues { it > 0 }.entries.joinToString(",") { "${it.key}:${it.value}" },
        )
    }

    private fun parseMonsterMap(raw: String): MutableMap<String, Int> {
        if (raw.isBlank()) return mutableMapOf()
        val map = mutableMapOf<String, Int>()
        for (pair in raw.split(",")) {
            val parts = pair.split(":")
            if (parts.size != 2) continue
            val count = parts[1].toIntOrNull() ?: continue
            map[parts[0]] = count
        }
        return map
    }
}
