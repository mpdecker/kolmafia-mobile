package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleBeachChange] / [QuestManager.setDesertExploration] place visit hooks
 * for `desertExploration` and `oasisAvailable`.
 */
object DesertVisitSync {

    const val DESERT_LABEL_ID = "db_l11desertlabel"
    private val DIV_LINK_PATTERN =
        Regex("""<div id=([^ >]+)[^>]*>(.*?)</div>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val ZONEFONT_PATTERN = Regex("""otherimages/zonefont/(.*?)\.gif""", RegexOption.IGNORE_CASE)
    private val EXP_PATTERN = Regex("""\((\d+)%explored\)""")

    fun parseDivLabel(label: String, html: String): String {
        val match = DIV_LINK_PATTERN.findAll(html).firstOrNull { it.groupValues[1] == label } ?: return ""
        return reconstructZonefont(match.groupValues[2]).ifBlank {
            match.groupValues[2]
        }
    }

    fun reconstructZonefont(divText: String): String {
        val out = StringBuilder()
        for (m in ZONEFONT_PATTERN.findAll(divText)) {
            when (val c = m.groupValues[1]) {
                "lparen" -> out.append('(')
                "percent" -> out.append('%')
                "rparen" -> out.append(')')
                else -> if (c.length == 1) out.append(c[0])
            }
        }
        return out.toString()
    }

    fun parseExplorationPercent(html: String): Int? {
        val labelText = parseDivLabel(DESERT_LABEL_ID, html)
        EXP_PATTERN.find(labelText)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        // Fixture / fallback: literal percent text near the label id
        val labelIndex = html.indexOf(DESERT_LABEL_ID, ignoreCase = true)
        if (labelIndex >= 0) {
            val window = html.substring(labelIndex, minOf(html.length, labelIndex + 800))
            EXP_PATTERN.find(window)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
        }
        return EXP_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    fun applyExploration(
        preferences: Preferences,
        questDatabase: QuestDatabase?,
        exploredPercent: Int,
    ): Boolean {
        val current = preferences.getInt("desertExploration", 0)
        return applyExplorationDelta(preferences, questDatabase, current, exploredPercent - current)
    }

    /** Desktop [QuestManager.incrementDesertExploration]. */
    fun incrementExploration(
        preferences: Preferences,
        questDatabase: QuestDatabase?,
        increment: Int,
    ): Boolean {
        val current = preferences.getInt("desertExploration", 0)
        return applyExplorationDelta(preferences, questDatabase, current, increment)
    }

    private fun applyExplorationDelta(
        preferences: Preferences,
        questDatabase: QuestDatabase?,
        current: Int,
        increment: Int,
    ): Boolean {
        if (current == 100 && increment >= 0) return false
        val explored = minOf(current + increment, 100)
        if (explored == current) return false
        preferences.setInt("desertExploration", explored)
        if (explored == 100) {
            questDatabase?.setProgress(Quest.DESERT, QuestDatabase.FINISHED)
        }
        return true
    }

    fun applyFromVisit(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        val location = url.orEmpty()
        if (!isBeachVisit(location)) return false
        var changed = false
        val oasis = html.contains("db_oasis")
        if (preferences.getBoolean("oasisAvailable", false) != oasis) {
            changed = true
        }
        preferences.setBoolean("oasisAvailable", oasis)
        parseExplorationPercent(html)?.let { percent ->
            if (applyExploration(preferences, questDatabase, percent)) changed = true
        }
        return changed
    }

    fun isBeachVisit(url: String): Boolean {
        val lower = url.lowercase()
        if (lower.contains("action=db_pyramid1") || lower.contains("action=expl_pyramidpre")) {
            return false
        }
        return lower.contains("whichplace=desertbeach") ||
            lower.contains("whichplace=exploathing_beach")
    }
}
