package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [net.sourceforge.kolmafia.request.PeeVPeeRequest.parseResponse]
 * fight-page attacks / hippy-stone / stance / fight-result sync.
 */
object PeeVPeeSync {
    private val attacksPattern = Regex("""You have (\d+) fight""")
    private val swaggerPattern =
        Regex("""You gain a little swagger <b>\(\+(\d)\)</b>""")
    private val challengePattern1 = Regex(
        """<div class="fight"><a.*?who=(\d+)"><b>(.*?)</b></a> calls out <a.*?who=(\d+)"><b>(.*?)</b></a> for battle!""",
    )
    private val challengePattern2 =
        Regex("""<a.*?who=(\d+)">(.*?)</a> vs <a.*?who=(\d+)">(.*?)</a>""")
    private val winPattern1 =
        Regex("""<span[^>]*><b>(.*?)</b> won the fight, <b>(\d+)</b> to <b>(\d+)</b>!""")
    private val winPattern2 =
        Regex("""align="center"><b>(.*?)</b> Wins!</td>""")
    private val itemTablePattern = Regex(
        """<table class="item".*?rel="(.*?)".*?title="(.*?)".*?descitem\(([\d]*)\).*?</table>""",
    )
    private val relTagPattern = Regex("""([\w]+)=([^&]*)&?""")

    private val musSubstat = setOf(
        "Beefiness", "Fortitude", "Muscleboundness", "Strengthliness",
        "Strongness", "Muscle", "muskewlairtees",
    )
    private val mysSubstat = setOf(
        "Enchantedness", "Magicalness", "Mysteriousness", "Wizardliness",
        "Mysticality", "mistikkaltees",
    )
    private val moxSubstat = setOf(
        "Cheek", "Chutzpah", "Roguishness", "Sarcasm", "Smarm", "Moxie", "mawksees",
    )

    fun apply(
        html: String,
        url: String,
        character: KoLCharacter?,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        inventoryManager: InventoryManager? = null,
    ) {
        val location = url.lowercase()
        if (!location.contains("peevpee.php")) return
        if (!location.contains("lid=")) {
            parseItems(html, inventoryManager, sessionLogger)
        }
        if (location.contains("place=shop") || location.contains("action=buy")) return

        if (location.contains("place=fight")) {
            parseAttacksAndStone(html, character)
            if (location.contains("action=fight")) {
                parseFightResult(html, character, preferences, sessionLogger)
            } else if (!PvpManager.stancesKnown) {
                PvpManager.parseStances(html)
            }
            return
        }

        if (location.contains("action=smashstone") && html.contains("You shatter")) {
            character?.updatePvp(10, hippyStoneBroken = true)
        }
    }

    private fun parseAttacksAndStone(html: String, character: KoLCharacter?) {
        when {
            attacksPattern.containsMatchIn(html) -> {
                val attacks = attacksPattern.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                character?.updatePvp(attacks, hippyStoneBroken = true)
            }
            html.contains("You're out of fights!") -> {
                character?.updatePvp(0, hippyStoneBroken = true)
            }
            html.contains("Magical Mystical Hippy Stone") -> {
                val fights = character?.state?.value?.pvpFightsLeft ?: 0
                character?.updatePvp(fights, hippyStoneBroken = false)
            }
        }
    }

    private fun parseFightResult(
        html: String,
        character: KoLCharacter?,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        if (html.contains("<tr><td>You may not") ||
            html.contains("<tr><td>You can't") ||
            html.contains("<tr><td>You know") ||
            html.contains("<tr><td>Sorry")
        ) {
            return
        }

        if (html.contains("<td><p>Before entering combat")) {
            PvpManager.abortReason = "You need to pledge allegiance to a clan first."
            return
        }

        swaggerPattern.find(html)?.let { match ->
            val gained = match.groupValues[1].toIntOrNull() ?: 0
            if (preferences != null && gained > 0) {
                val current = preferences.getInt("availableSwagger", 0)
                preferences.setInt("availableSwagger", current + gained)
            }
        }

        var compactResults = false
        var me: String? = null
        var you: String? = null
        val challenge1 = challengePattern1.find(html)
        if (challenge1 != null) {
            me = challenge1.groupValues[2]
            you = challenge1.groupValues[4]
        } else {
            compactResults = true
            val challenge2 = challengePattern2.find(html)
            if (challenge2 != null) {
                me = challenge2.groupValues[2]
                you = challenge2.groupValues[4]
            }
        }

        var won = false
        var result1 = 0
        var result2 = 0
        if (!compactResults) {
            winPattern1.find(html)?.let { win ->
                won = win.groupValues[1] == me
                result1 = win.groupValues[2].toIntOrNull() ?: 0
                result2 = win.groupValues[3].toIntOrNull() ?: 0
            }
        } else {
            winPattern2.find(html)?.let { win ->
                won = win.groupValues[1] == me
            }
        }

        if (you == null) {
            if (html.contains("contains a Mystical Magical Hippy Stone")) {
                PvpManager.noFight = true
                return
            }
            PvpManager.abortReason = "Something went wrong with executing your PvP fights"
            return
        }

        val message = buildString {
            append("You challenged ")
            append(you)
            append(" and ")
            append(if (won) "won" else "lost")
            append(" the PvP fight")
            if (!compactResults) {
                append(", ")
                append(if (won) result1 else result2)
                append(" to ")
                append(if (won) result2 else result1)
                append("!")
            }
        }
        sessionLogger?.appendRawLine(message)

        if (won && preferences != null) {
            val existing = preferences.getString("currentPvpVictories", "")
            preferences.setString("currentPvpVictories", existing + you + ",")
        } else if (!won && !compactResults) {
            parseStatLoss(html, character, sessionLogger)
        }
    }

    private fun parseStatLoss(
        html: String,
        character: KoLCharacter?,
        sessionLogger: SessionLogger?,
    ) {
        val username = character?.state?.value?.name.orEmpty()
        if (username.isEmpty()) return
        val statPrefix = username.lowercase() + " lost "
        var musDelta = 0L
        var mysDelta = 0L
        var moxDelta = 0L
        for (block in html.split("<td>")) {
            if (!block.lowercase().startsWith(statPrefix)) continue
            val end = block.indexOf(".</td>")
            if (end < 0) continue
            val printed = block.substring(0, end)
            val lostIdx = printed.lastIndexOf(" lost ")
            if (lostIdx < 0) continue
            val parts = printed.substring(lostIdx + 6).split(" ")
            if (parts.size < 2) continue
            val statsLost = -1L * (parts[0].toLongOrNull() ?: continue)
            val token = parts[1]
            if (token in musSubstat) musDelta += statsLost
            if (token in mysSubstat) mysDelta += statsLost
            if (token in moxSubstat) moxDelta += statsLost
            sessionLogger?.appendRawLine(printed)
        }
        character?.adjustSubstats(musDelta, mysDelta, moxDelta)
    }

    private fun parseItems(
        html: String,
        inventoryManager: InventoryManager?,
        sessionLogger: SessionLogger?,
    ) {
        val match = itemTablePattern.find(html) ?: return
        val rel = match.groupValues[1]
        val title = match.groupValues[2]
        var itemId = -1
        var count = 1
        for (tag in relTagPattern.findAll(rel)) {
            when (tag.groupValues[1]) {
                "id" -> itemId = tag.groupValues[2].toIntOrNull() ?: -1
                "n" -> count = tag.groupValues[2].toIntOrNull() ?: 1
            }
        }
        if (itemId < 0) return
        inventoryManager?.gainItemLocally(itemId, count)
        val name = ItemDatabase.getItemName(itemId).ifEmpty { title }
        sessionLogger?.appendRawLine("You acquire an item: $name")
    }
}
