package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharpaneValhallaSync
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.session.ValhallaManager

/** Desktop AfterLifeRequest headless parse + session-log (Phases 3306–3320). */
object AfterLifeRequest {

    private val ITEM_PATTERN = Regex(
        """<span onclick='descitem\(([\d]+)\)'>([^<]*)<.*?name=whichitem value=([\d]+)>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val KARMA_PATTERN = Regex("""You gain ([0123456789,]+) Karma""", RegexOption.DOT_MATCHES_ALL)

    fun parseResponse(
        url: String,
        responseText: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        if (!url.startsWith("afterlife.php")) return false

        if (url == "afterlife.php") {
            CharpaneValhallaSync.markInValhalla()
            return true
        }

        ITEM_PATTERN.findAll(responseText).forEach { match ->
            val descId = match.groupValues[1]
            val itemName = match.groupValues[2]
            val itemId = match.groupValues[3].toIntOrNull() ?: return@forEach
            val dataName = ItemDatabase.getById(itemId)?.name
            if (dataName == null || !dataName.equals(itemName, ignoreCase = true)) {
                ItemDatabase.registerItem(itemId, itemName, descId)
            }
        }

        val action = actionFromUrl(url) ?: return false
        if (action == "pearlygates") {
            var karma = preferences?.getInt("bankedKarma", 0) ?: 0
            log(sessionLogger, "You have $karma banked Karma.")
            KARMA_PATTERN.findAll(responseText).forEach {
                val delta = it.groupValues[1].replace(",", "").toIntOrNull() ?: 0
                log(sessionLogger, "You gain $delta Karma")
                karma += delta
            }
            log(sessionLogger, "Your new Karma balance is $karma")
            preferences?.setInt("bankedKarma", karma)
            CharpaneValhallaSync.markInValhalla()
            return true
        }

        val delta = karmaDelta(action, responseText, url)
        if (delta != 0) {
            preferences?.incrementInt("bankedKarma", delta)
            val message = if (delta < 0) "You spend ${-delta} Karma" else "You gain $delta Karma"
            log(sessionLogger, message)
        }
        return true
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.startsWith("afterlife.php")) return false
        val action = actionFromUrl(url)
        val karma = preferencesFromLogger(sessionLogger)
        val message = when (action) {
            null -> if (url.contains("place=")) "Visiting Valhalla vendor" else null
            "pearlygates" -> "Welcome to Valhalla!"
            "scperm", "hcperm" -> permSkillMessage(url, action.startsWith("hc"), karma)
            "buydeli", "buyarmory" -> buyMessage(url, action, karma)
            "delireturn", "armoryreturn" -> returnMessage(url, action, karma)
            "ascend" -> if (url.contains("confirmascend=1")) ascendMessage(url) else null
            else -> null
        }
        if (message != null) {
            sessionLogger?.appendRawLine(message)
        }
        return true
    }

    fun handleAscensionConfirm(
        url: String,
        character: net.sourceforge.kolmafia.character.KoLCharacter?,
        preferences: Preferences?,
        banishManager: net.sourceforge.kolmafia.banish.BanishManager? = null,
    ) {
        if (!url.contains("afterlife.php") || !url.contains("confirmascend=1")) return
        ValhallaManager.onAscension(character, preferences, banishManager)
    }

    private fun actionFromUrl(url: String): String? =
        Regex("""[?&]action=([^&]+)""").find(url)?.groupValues?.get(1)

    private fun karmaDelta(action: String, responseText: String, url: String): Int = when (action) {
        "scperm", "hcperm" ->
            if (responseText.contains("don't have enough Karma")) 0 else if (action == "scperm") -100 else -200
        "returnskill" -> if (url.contains("hc=1")) 200 else 100
        "buydeli" -> -1
        "delireturn" -> 1
        "buyarmory" -> -10
        "armoryreturn" -> 10
        else -> 0
    }

    private fun log(sessionLogger: SessionLogger?, message: String) {
        sessionLogger?.appendRawLine(message)
    }

    private fun preferencesFromLogger(@Suppress("UNUSED_PARAMETER") sessionLogger: SessionLogger?): Int = 0

    private fun permSkillMessage(url: String, hc: Boolean, karma: Int): String? {
        val skillId = Regex("""whichskill=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        val type = if (hc) "Hard" else "Soft"
        val cost = if (hc) 200 else 100
        return "$type core perm skill #$skillId for $cost Karma (initial balance = $karma)"
    }

    private fun buyMessage(url: String, action: String, karma: Int): String? {
        val itemId = Regex("""whichitem=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        val itemName = ItemDatabase.getById(itemId)?.name ?: "item #$itemId"
        val cost = if (action == "buydeli") 1 else 10
        return "Buy $itemName for $cost Karma (initial balance = $karma)"
    }

    private fun returnMessage(url: String, action: String, karma: Int): String? {
        val itemId = Regex("""whichitem=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        val itemName = ItemDatabase.getById(itemId)?.name ?: "item #$itemId"
        val cost = if (action.startsWith("deli")) 1 else 10
        return "Return $itemName for $cost Karma (initial balance = $karma)"
    }

    private fun ascendMessage(url: String): String {
        val type = Regex("""asctype=(\d+)""").find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val typeLabel = when (type) {
            1 -> "Casual"
            2 -> "Normal"
            3 -> "Hardcore"
            else -> "Type $type"
        }
        return "Ascend as a $typeLabel"
    }
}

private fun Preferences.incrementInt(key: String, delta: Int) {
    setInt(key, getInt(key, 0) + delta)
}
