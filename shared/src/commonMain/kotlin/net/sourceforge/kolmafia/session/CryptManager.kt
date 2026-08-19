package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

/**
 * Desktop [CryptManager] Cyrpt evilness helpers.
 */
object CryptManager {

    const val EVILOMETER = 4964
    const val EVIL_EYE = 5010

    const val DEFILED_ALCOVE = 261
    const val DEFILED_CRANNY = 262
    const val DEFILED_NICHE = 263
    const val DEFILED_NOOK = 264

    private val ZONE_TO_PROPERTY = mapOf(
        DEFILED_ALCOVE to "cyrptAlcoveEvilness",
        DEFILED_CRANNY to "cyrptCrannyEvilness",
        DEFILED_NICHE to "cyrptNicheEvilness",
        DEFILED_NOOK to "cyrptNookEvilness",
    )

    private val ZONE_NAME_TO_PROPERTY = mapOf(
        "The Defiled Alcove" to "cyrptAlcoveEvilness",
        "The Defiled Cranny" to "cyrptCrannyEvilness",
        "The Defiled Niche" to "cyrptNicheEvilness",
        "The Defiled Nook" to "cyrptNookEvilness",
    )

    private val BOSS_TO_ZONE = mapOf(
        "conjoined zmombie" to DEFILED_ALCOVE,
        "huge ghuol" to DEFILED_CRANNY,
        "gargantulihc" to DEFILED_NICHE,
        "giant skeelton" to DEFILED_NOOK,
    )

    private val TOTAL_PATTERN = Regex("""<center>Total evil: <b>(\d+)</b>""")
    private val CORNERS_PATTERN = Regex(
        """<p>Alcove: <b>(\d+)</b><br>Cranny: <b>(\d+)</b><br>Niche: <b>(\d+)</b><br>Nook: <b>(\d+)</b>""",
    )
    private val BEEP_PATTERN = Regex("""Your Evilometer beeps (\d+) times""")

    fun evilZoneProperty(zone: Int): String? = ZONE_TO_PROPERTY[zone]

    fun evilZoneProperty(zoneName: String): String? = ZONE_NAME_TO_PROPERTY[zoneName]

    fun bossZone(monsterName: String): Int? = BOSS_TO_ZONE[monsterName.trim().lowercase()]

    fun bossProperty(monsterName: String): String? =
        bossZone(monsterName)?.let { evilZoneProperty(it) }

    fun setEvilness(property: String, value: Int, preferences: Preferences) {
        val current = preferences.getInt(property, 0)
        decreaseEvilness(property, current - value, preferences)
    }

    fun decreaseEvilness(zone: Int, delta: Int, preferences: Preferences): Boolean {
        val property = evilZoneProperty(zone) ?: return false
        decreaseEvilness(property, delta, preferences)
        return true
    }

    fun decreaseEvilness(property: String, delta: Int, preferences: Preferences) {
        preferences.setInt(property, (preferences.getInt(property, 0) - delta).coerceAtLeast(0))
        val total = (preferences.getInt("cyrptTotalEvilness", 0) - delta).coerceAtLeast(0)
        preferences.setInt("cyrptTotalEvilness", if (total == 0) 999 else total)
    }

    fun acquireEvilometer(questDatabase: QuestDatabase?, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        questDatabase?.setProgress(Quest.CYRPT, QuestDatabase.STARTED)
        prefs.setInt("cyrptTotalEvilness", 200)
        prefs.setInt("cyrptAlcoveEvilness", 50)
        prefs.setInt("cyrptCrannyEvilness", 50)
        prefs.setInt("cyrptNicheEvilness", 50)
        prefs.setInt("cyrptNookEvilness", 50)
        return true
    }

    fun applyAcquireFromHtml(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (!html.contains("You acquire an item", ignoreCase = true)) return false
        if (!html.contains("Evilometer", ignoreCase = true)) return false
        return acquireEvilometer(questDatabase, preferences)
    }

    fun examineEvilometer(
        html: String,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        val prefs = preferences ?: return false
        var total = 0
        var alcove = 0
        var cranny = 0
        var niche = 0
        var nook = 0
        TOTAL_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { total = it }
        CORNERS_PATTERN.find(html)?.let { match ->
            alcove = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 0
            cranny = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            niche = match.groupValues.getOrNull(3)?.toIntOrNull() ?: 0
            nook = match.groupValues.getOrNull(4)?.toIntOrNull() ?: 0
        }
        prefs.setInt("cyrptTotalEvilness", total)
        prefs.setInt("cyrptAlcoveEvilness", alcove)
        prefs.setInt("cyrptCrannyEvilness", cranny)
        prefs.setInt("cyrptNicheEvilness", niche)
        prefs.setInt("cyrptNookEvilness", nook)
        if (html.contains("give it a proper burial")) {
            consumeItem(EVILOMETER, 1)
        }
        return true
    }

    fun visitCrypt(html: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        if (!html.contains("cyrpt/", ignoreCase = true)) return false
        var nook = if (html.contains("cyrpt/ul.gif")) prefs.getInt("cyrptNookEvilness", 0) else 0
        var niche = if (html.contains("cyrpt/ur.gif")) prefs.getInt("cyrptNicheEvilness", 0) else 0
        var cranny = if (html.contains("cyrpt/ll.gif")) prefs.getInt("cyrptCrannyEvilness", 0) else 0
        var alcove = if (html.contains("cyrpt/lr.gif")) prefs.getInt("cyrptAlcoveEvilness", 0) else 0
        var total = nook + niche + cranny + alcove
        if (html.contains("cyrpt/thecrypt_heart.gif")) {
            nook = 0
            niche = 0
            cranny = 0
            alcove = 0
            total = 999
        }
        prefs.setInt("cyrptAlcoveEvilness", alcove)
        prefs.setInt("cyrptCrannyEvilness", cranny)
        prefs.setInt("cyrptNicheEvilness", niche)
        prefs.setInt("cyrptNookEvilness", nook)
        prefs.setInt("cyrptTotalEvilness", total)
        return true
    }

    fun applyFromVisit(url: String?, html: String, preferences: Preferences?): Boolean {
        if (url == null || !url.contains("crypt.php", ignoreCase = true)) return false
        return visitCrypt(html, preferences)
    }

    fun encounterBoss(monsterName: String, preferences: Preferences?): Boolean {
        val prefs = preferences ?: return false
        val property = bossProperty(monsterName) ?: return false
        if (prefs.getInt(property, 0) > 13) {
            setEvilness(property, 13, prefs)
            return true
        }
        return false
    }

    fun defeatBoss(
        monsterName: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        val trimmed = monsterName.trim()
        if (trimmed.equals("Bonerdagon", ignoreCase = true)) {
            questDatabase?.setProgress(Quest.CYRPT, "step1")
            preferences?.setInt("cyrptTotalEvilness", 0)
            return true
        }
        val property = bossProperty(trimmed) ?: return false
        val prefs = preferences ?: return false
        setEvilness(property, 0, prefs)
        return true
    }

    fun handleFightEvilness(
        html: String,
        adventureId: String,
        preferences: Preferences?,
    ): Boolean {
        val prefs = preferences ?: return false
        if (!html.contains("Evilometer") &&
            !html.contains("ghost vacuum") &&
            !html.contains("gravy sloshes") &&
            !html.contains("the nightmare fuel") &&
            !html.contains("an evil draft blows")
        ) {
            return false
        }
        val property = resolveFightZoneProperty(html, adventureId) ?: return false
        var evilness = when {
            html.contains("a single beep") -> 1
            html.contains("beeps three times") || html.contains("three quick beeps") -> 3
            html.contains("five quick beeps") -> 5
            html.contains("loud") -> prefs.getInt(property, 0)
            else -> 0
        }
        if (evilness == 0 && html.contains("Evilometer beeps once")) {
            evilness = 1
        }
        if (html.contains("ghost vacuum sucks up some extra evil")) {
            evilness++
        }
        if (html.contains("Some gravy sloshes")) {
            evilness++
        }
        if (html.contains("an evil draft blows")) {
            evilness++
        }
        if (html.contains("the nightmare fuel")) {
            evilness += 2
            prefs.setInt(
                "_nightmareFuelCharges",
                (prefs.getInt("_nightmareFuelCharges", 0) - 1).coerceAtLeast(0),
            )
        }
        if (evilness == 0) {
            evilness = BEEP_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return false
        }
        decreaseEvilness(property, evilness, prefs)
        return true
    }

    fun applyEvilEye(
        html: String,
        count: Int,
        preferences: Preferences?,
    ): Boolean {
        val prefs = preferences ?: return false
        if (!html.contains("Evilometer emits three quick beeps")) return false
        val evilness = minOf(prefs.getInt("cyrptNookEvilness", 0), 3 * count.coerceAtLeast(0))
        return decreaseEvilness(DEFILED_NOOK, evilness, prefs)
    }

    private fun resolveFightZoneProperty(html: String, adventureId: String): String? {
        adventureId.toIntOrNull()?.let { id ->
            evilZoneProperty(id)?.let { return it }
        }
        for ((name, property) in ZONE_NAME_TO_PROPERTY) {
            if (html.contains(name)) return property
        }
        return null
    }
}
