package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor

/**
 * Desktop PlaceRequest campaway whichplace sync (Phases 2361–2375).
 * Deepened in Behavioral Deepen XLI (6311–6330) toward desktop CampAwayRequest.parseResponse.
 */
object CampAwaySync {
    private val EFFECT_PATTERN = Regex(
        """You acquire an effect:\s*<b>(.*?)</b>""",
        RegexOption.IGNORE_CASE,
    )
    private val CLOUD_LETTER = Regex(
        """otherimages/smoke2/([^"']+)""",
        RegexOption.IGNORE_CASE,
    )
    private val CLOUD_LETTERS = mapOf(
        "a.png" to 'A', "b.png" to 'B', "c.png" to 'C', "d.png" to 'D', "e.png" to 'E',
        "f.png" to 'F', "g.png" to 'G', "h.png" to 'H', "i.png" to 'I', "j.png" to 'J',
        "k.png" to 'K', "l.png" to 'L', "m.png" to 'M', "n.png" to 'N', "o.png" to 'O',
        "p.png" to 'P', "q.png" to 'Q', "r.png" to 'R', "s.png" to 'S', "t.png" to 'T',
        "u.png" to 'U', "v.png" to 'V', "w.png" to 'W', "x.png" to 'X', "y.png" to 'Y',
        "z.png" to 'Z',
        "0.png" to '0', "1.png" to '1', "2.png" to '2', "3.png" to '3', "4.png" to '4',
        "5.png" to '5', "6.png" to '6', "7.png" to '7', "8.png" to '8', "9.png" to '9',
        "space.png" to ' ', "comma.png" to ',', "period.png" to '.', "colon.png" to ':',
        "semicolon.png" to ';', "atsign.png" to '@', "asterisk.png" to '*',
        "hyphen.png" to '-', "equals.png" to '=',
    )

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
    ) {
        val prefs = preferences
        prefs?.setBoolean(
            "getawayCampsiteUnlocked",
            html.contains("campaway/campawaybg.gif", ignoreCase = true) ||
                html.contains("campaway", ignoreCase = true),
        )
        updateFreeRestsFromHtml(html, prefs)

        val action = PlaceSync.action(url)
        when {
            action.startsWith("campaway_tent") || action.contains("rest") -> {
                parseTentRest(html, prefs)
            }
            action.equals("campaway_sky", ignoreCase = true) ||
                action.contains("cloud") ||
                action.contains("smile") -> {
                parseSkyBuff(html, prefs)
            }
            action.contains("cloud") || html.contains("cloud bun", ignoreCase = true) -> {
                prefs?.setBoolean("_campAwayCloudBuffUsed", true)
                prefs?.setInt(
                    CampAwayRequest.CLOUD_BUFFS_PREF,
                    (prefs.getInt(CampAwayRequest.CLOUD_BUFFS_PREF, 0) + 1),
                )
            }
            action.contains("smile") || html.contains("smile of", ignoreCase = true) -> {
                prefs?.setBoolean("_campAwaySmileBuffUsed", true)
            }
        }
        ResultProcessor.processResults(false, html, null, character, preferences)
    }

    private fun parseTentRest(html: String, prefs: Preferences?) {
        prefs ?: return
        prefs.setInt("timesRested", prefs.getInt("timesRested", 0) + 1)
        val effect = EFFECT_PATTERN.find(html)?.groupValues?.getOrNull(1).orEmpty()
        when {
            effect.contains("Muscular", ignoreCase = true) -> prefs.setInt("campAwayDecoration", 1)
            effect.contains("Mystical", ignoreCase = true) -> prefs.setInt("campAwayDecoration", 2)
            effect.contains("Moxious", ignoreCase = true) -> prefs.setInt("campAwayDecoration", 3)
            else -> prefs.setInt("campAwayDecoration", 0)
        }
        if (html.contains("restlabel_free.gif", ignoreCase = true) ||
            html.contains("didn't cost", ignoreCase = true) ||
            html.contains("free", ignoreCase = true)
        ) {
            prefs.setInt("_freeRestsUsed", prefs.getInt("_freeRestsUsed", 0) + 1)
        }
        prefs.setBoolean("_campAwayTentRested", true)
        updateFreeRestsFromHtml(html, prefs)
    }

    private fun parseSkyBuff(html: String, prefs: Preferences?) {
        prefs ?: return
        val effect = EFFECT_PATTERN.find(html)?.groupValues?.getOrNull(1).orEmpty()
        when {
            effect.contains("Smile of the ", ignoreCase = true) -> {
                val sign = effect.substringAfter("Smile of the ", "").trim()
                if (sign.isNotEmpty()) {
                    prefs.setString("_campAwaySmileBuffSign", sign)
                }
                prefs.setInt(
                    "_campAwaySmileBuffs",
                    prefs.getInt("_campAwaySmileBuffs", 0) + 1,
                )
                prefs.setBoolean("_campAwaySmileBuffUsed", true)
            }
            effect.contains("Cloud-Talk", ignoreCase = true) -> {
                parseCloudTalk(html, prefs)
                prefs.setInt(
                    CampAwayRequest.CLOUD_BUFFS_PREF,
                    prefs.getInt(CampAwayRequest.CLOUD_BUFFS_PREF, 0) + 1,
                )
                prefs.setBoolean("_campAwayCloudBuffUsed", true)
            }
            else -> {
                // Desktop exhausts both daily caps when effect parse fails.
                prefs.setInt(CampAwayRequest.CLOUD_BUFFS_PREF, 1)
                prefs.setInt("_campAwaySmileBuffs", 3)
            }
        }
    }

    private fun parseCloudTalk(html: String, prefs: Preferences) {
        val buffer = StringBuilder()
        CLOUD_LETTER.findAll(html).forEach { match ->
            val file = match.groupValues[1].substringAfterLast('/').lowercase()
            CLOUD_LETTERS[file]?.let { buffer.append(it) }
        }
        if (buffer.isNotEmpty()) {
            prefs.setString("_campAwayCloudTalkMessage", buffer.toString())
        }
    }

    private fun updateFreeRestsFromHtml(html: String, prefs: Preferences?) {
        prefs ?: return
        prefs.setBoolean(
            "_freeRestsAvailable",
            html.contains("restlabel_free.gif", ignoreCase = true),
        )
    }
}
