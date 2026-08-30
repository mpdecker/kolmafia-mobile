package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.BountyDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [BountyHunterHunterRequest] residual deepen — lucre, take/turnin,
 * completed bounty tally, location prefs (Phases 3276–3290).
 */
object BountyHunterHunterRequest {

    private val TOKEN_PATTERN =
        Regex("""You have.*?<b>([\d,]+)</b> filthy lucre""", RegexOption.DOT_MATCHES_ALL)
    private val COMPLETED_PATTERN =
        Regex("""turn in your (\d+) (.*?) to the Bounty Hunter Hunter""")

    private val EASY_PATTERN =
        Regex("""Easy Bounty!  Come back when you've collected (\d+) (.*?) from""")
    private val UNTAKEN_EASY_PATTERN =
        Regex(
            """Easy Bounty:.*?/itemimages/(.*?) width.*?>(\d+) (.*?) from.*?takelow""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    private val EASY_QTY_PATTERN =
        Regex("""Easy Bounty.*?You have collected (\d+) .*?giveup_low""", RegexOption.DOT_MATCHES_ALL)

    private val HARD_PATTERN =
        Regex("""Hard Bounty!  Come back when you've collected (\d+) (.*?) from""")
    private val UNTAKEN_HARD_PATTERN =
        Regex(
            """Hard Bounty:.*?/itemimages/(.*?) width.*?>(\d+) (.*?) from.*?takehigh""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    private val HARD_QTY_PATTERN =
        Regex("""Hard Bounty.*?You have collected (\d+) .*?giveup_high""", RegexOption.DOT_MATCHES_ALL)

    private val SPECIAL_PATTERN =
        Regex("""Specialty Bounty!  Come back when you've collected (\d+) (.*?) from""")
    private val UNTAKEN_SPECIAL_PATTERN =
        Regex(
            """Specialty Bounty:.*?/itemimages/(.*?) width.*?>(\d+) (.*?) from.*?takespecial""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    private val SPECIAL_QTY_PATTERN =
        Regex(
            """Specialty Bounty.*?You have collected (\d+) .*?giveup_spe""",
            RegexOption.DOT_MATCHES_ALL,
        )

    fun parseResponse(
        location: String,
        responseText: String,
        preferences: Preferences?,
        character: KoLCharacter? = null,
        inventory: InventoryManager? = null,
        sessionLogger: SessionLogger? = null,
    ) {
        preferences?.setBoolean("bountyHunterVisited", true)

        for (m in COMPLETED_PATTERN.findAll(responseText)) {
            val count = m.groupValues[1].toIntOrNull() ?: continue
            val plural = m.groupValues[2]
            val bountyItem = BountyDatabase.getName(plural) ?: continue
            preferences?.setInt(
                "bountiesCompleted",
                preferences.getInt("bountiesCompleted", 0) + 1,
            )
            RequestLogger.updateSessionLog(
                "turned in $count $plural ($bountyItem)",
                sessionLogger,
            )
        }

        TOKEN_PATTERN.find(responseText)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toIntOrNull()
            ?.let { lucre ->
                preferences?.setInt("availableFilthyLucre", lucre)
                val have = inventory?.getCount(ItemPool.LUCRE) ?: 0
                if (lucre > have) inventory?.gainItemLocally(ItemPool.LUCRE, lucre - have)
                else if (lucre < have) inventory?.consumeItemLocally(ItemPool.LUCRE, have - lucre)
            }

        val action = actionOf(location)
        if (action.isNullOrBlank()) {
            parseEasy(responseText, preferences)
            parseHard(responseText, preferences)
            parseSpecial(responseText, preferences)
            // Preserve thin currentBountyItem scrape
            Regex(
                """(?:Current Bounty|Your assignment):\s*<b>(.*?)</b>""",
                RegexOption.IGNORE_CASE,
            ).find(responseText)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let {
                preferences?.setString("currentBountyItem", it)
            }
            ResultProcessor.processResults(false, responseText, inventory, character, preferences)
            return
        }

        when (action) {
            "takelow" -> takeBounty(
                preferences,
                "_untakenEasyBountyItem",
                "currentEasyBountyItem",
                "_nextBountyLocation",
            )
            "takehigh" -> takeBounty(
                preferences,
                "_untakenHardBountyItem",
                "currentHardBountyItem",
                "_nextBountyLocation",
            )
            "takespecial" -> takeBounty(
                preferences,
                "_untakenSpecialBountyItem",
                "currentSpecialBountyItem",
                "_nextBountyLocation",
            )
            "giveup_low" -> preferences?.setString("currentEasyBountyItem", "")
            "giveup_high" -> preferences?.setString("currentHardBountyItem", "")
            "giveup_spe" -> preferences?.setString("currentSpecialBountyItem", "")
            "buy" -> {
                // Coinmaster buy response — lucre spend + item gain via ResultProcessor
                ResultProcessor.processResults(false, responseText, inventory, character, preferences)
                TOKEN_PATTERN.find(responseText)?.groupValues?.get(1)
                    ?.replace(",", "")
                    ?.toIntOrNull()
                    ?.let { preferences?.setInt("availableFilthyLucre", it) }
            }
            else -> ResultProcessor.processResults(false, responseText, inventory, character, preferences)
        }
    }

    fun registerRequest(urlString: String, preferences: Preferences?, sessionLogger: SessionLogger?): Boolean {
        if (!urlString.contains("bounty.php")) return false
        val action = actionOf(urlString)
        if (action.isNullOrBlank()) {
            RequestLogger.updateSessionLog("Visiting the Bounty Hunter Hunter", sessionLogger)
            return true
        }
        when (action) {
            "takelow" -> {
                val name = preferences?.getString("_untakenEasyBountyItem").orEmpty()
                logTake("easy", name, preferences, sessionLogger)
            }
            "takehigh" -> {
                val name = preferences?.getString("_untakenHardBountyItem").orEmpty()
                logTake("hard", name, preferences, sessionLogger)
            }
            "takespecial" -> {
                val name = preferences?.getString("_untakenSpecialBountyItem").orEmpty()
                logTake("specialty", name, preferences, sessionLogger)
            }
            "giveup_low" ->
                RequestLogger.updateSessionLog("abandon easy bounty assignment", sessionLogger)
            "giveup_high" ->
                RequestLogger.updateSessionLog("abandon hard bounty assignment", sessionLogger)
            "giveup_spe" ->
                RequestLogger.updateSessionLog("abandon special bounty assignment", sessionLogger)
            else -> return false
        }
        return true
    }

    private fun logTake(
        kind: String,
        bountyName: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ) {
        if (bountyName.isEmpty()) {
            RequestLogger.updateSessionLog("no $kind bounty accepted", sessionLogger)
            return
        }
        val number = BountyDatabase.getNumber(bountyName)
        val plural = BountyDatabase.getPlural(bountyName)
        RequestLogger.updateSessionLog(
            "accept $kind bounty assignment to collect $number $plural",
            sessionLogger,
        )
        preferences?.setString("_lastBountyAccepted", bountyName)
    }

    private fun takeBounty(
        preferences: Preferences?,
        untakenSetting: String,
        currentSetting: String,
        locationSetting: String,
    ) {
        preferences ?: return
        val currentUntaken = preferences.getString(untakenSetting)
        if (currentUntaken.isNotEmpty()) {
            preferences.setString(currentSetting, "$currentUntaken:0")
            preferences.setString(untakenSetting, "")
        }
        val location = BountyDatabase.getLocation(currentUntaken)
        if (!location.isNullOrEmpty()) {
            preferences.setString(locationSetting, location)
            preferences.setString("nextAdventure", location)
        }
    }

    private fun parseEasy(responseText: String, preferences: Preferences?) =
        parseBounty(
            responseText, EASY_PATTERN, UNTAKEN_EASY_PATTERN, EASY_QTY_PATTERN,
            "currentEasyBountyItem", "_untakenEasyBountyItem", "_unknownEasyBountyItem", preferences,
        )

    private fun parseHard(responseText: String, preferences: Preferences?) =
        parseBounty(
            responseText, HARD_PATTERN, UNTAKEN_HARD_PATTERN, HARD_QTY_PATTERN,
            "currentHardBountyItem", "_untakenHardBountyItem", "_unknownHardBountyItem", preferences,
        )

    private fun parseSpecial(responseText: String, preferences: Preferences?) =
        parseBounty(
            responseText, SPECIAL_PATTERN, UNTAKEN_SPECIAL_PATTERN, SPECIAL_QTY_PATTERN,
            "currentSpecialBountyItem", "_untakenSpecialBountyItem", "_unknownSpecialBountyItem",
            preferences,
        )

    private fun parseBounty(
        responseText: String,
        takenPattern: Regex,
        untakenPattern: Regex,
        quantityPattern: Regex,
        currentSetting: String,
        untakenSetting: String,
        unknownSetting: String,
        preferences: Preferences?,
    ) {
        preferences ?: return
        val taken = takenPattern.find(responseText)
        if (taken == null) {
            val untaken = untakenPattern.find(responseText)
            if (untaken != null) {
                val plural = untaken.groupValues[3]
                val bountyItem = BountyDatabase.getName(plural)
                if (bountyItem != null) {
                    preferences.setString(untakenSetting, bountyItem)
                } else {
                    preferences.setString(
                        unknownSetting,
                        "${untaken.groupValues[1]}:${untaken.groupValues[2]}:${untaken.groupValues[3]}",
                    )
                }
            } else {
                preferences.setString(untakenSetting, "")
            }
            preferences.setString(currentSetting, "")
            return
        }
        val plural = taken.groupValues[2]
        val bountyItem = BountyDatabase.getName(plural)
        if (bountyItem != null) {
            val qty = quantityPattern.find(responseText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            preferences.setString(currentSetting, "$bountyItem:$qty")
        }
    }

    private fun actionOf(url: String): String? {
        val m = Regex("""[?&]action=([^&]+)""", RegexOption.IGNORE_CASE).find(url)
        return m?.groupValues?.get(1)
    }
}
