package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [GenericRequest.parseResults] cross-page residual leftovers that mobile
 * [ResultProcessor] does not already cover: Lucky removal, empty Agua bottle,
 * God Lobster maxed, ascend-fail message, butler-meat session line.
 */
object ParseResultsResidual {
    const val EMPTY_AGUA_DE_VIDA_BOTTLE = 4130
    const val LUCKY_EFFECT_ID = 2693
    const val BUTLER_MEAT =
        "Your Meat Butler has collected some meat from around your campsite."
    private const val GASH_MESSAGE =
        "You may not enter the Astral Gash again until tomorrow."

    private val FAILED_ASCENSION =
        Regex(
            """<b style="color: white">Results:</b></td></tr><tr><td style="padding: 5px; border: 1px solid blue;"><center><table><tr><td>(.*?)</td>""",
            RegexOption.IGNORE_CASE,
        )

    data class Outcome(
        val luckyRemoved: Boolean = false,
        val aguaConsumed: Boolean = false,
        val godLobsterMaxed: Boolean = false,
        val ascendFailMessage: String? = null,
        val butlerLogged: Boolean = false,
    )

    fun apply(
        urlString: String,
        responseText: String,
        preferences: Preferences? = null,
        inventory: InventoryManager? = null,
        effectManager: EffectManager? = null,
        sessionLogger: SessionLogger? = null,
    ): Outcome {
        var luckyRemoved = false
        var aguaConsumed = false
        var godLobsterMaxed = false
        var ascendFail: String? = null
        var butlerLogged = false

        if (responseText.contains("You feel less lucky")) {
            luckyRemoved = effectManager?.removeEffect(LUCKY_EFFECT_ID) == true ||
                effectManager == null
            if (effectManager == null) {
                // Pref-only fallback when no EffectManager is wired.
                preferences?.setBoolean("_luckyRemovedByParseResults", true)
            }
        }

        if (responseText.contains("You break the bottle on the ground")) {
            ResultProcessor.processItem(
                EMPTY_AGUA_DE_VIDA_BOTTLE,
                -1,
                preferences,
                inventory = inventory,
            )
            aguaConsumed = true
        }

        val page = pageName(urlString)
        when (page) {
            "main.php" -> {
                if (urlString.contains("fightgodlobster=1", ignoreCase = true) &&
                    responseText.contains("can't challenge your God Lobster anymore")
                ) {
                    preferences?.setInt("_godLobsterFights", 3)
                    godLobsterMaxed = true
                }
                if (responseText.contains(GASH_MESSAGE)) {
                    ascendFail = "Failed to ascend: $GASH_MESSAGE"
                    RequestLogger.updateSessionLog(ascendFail, sessionLogger)
                }
            }
            "campground.php" -> {
                if (responseText.contains(BUTLER_MEAT)) {
                    RequestLogger.updateSessionLog(BUTLER_MEAT, sessionLogger)
                    butlerLogged = true
                }
            }
            "ascend.php" -> {
                if (urlString.contains("confirm=on") && urlString.contains("confirm2=on")) {
                    val msg = FAILED_ASCENSION.find(responseText)?.groupValues?.getOrNull(1)
                    if (msg != null) {
                        ascendFail = "Failed to ascend: $msg"
                        RequestLogger.updateSessionLog(ascendFail, sessionLogger)
                    }
                }
            }
        }

        return Outcome(
            luckyRemoved = luckyRemoved,
            aguaConsumed = aguaConsumed,
            godLobsterMaxed = godLobsterMaxed,
            ascendFailMessage = ascendFail,
            butlerLogged = butlerLogged,
        )
    }

    private fun pageName(urlString: String): String {
        val path = urlString.substringAfterLast('/').substringBefore('?').substringBefore('#')
        return path.lowercase()
    }
}
