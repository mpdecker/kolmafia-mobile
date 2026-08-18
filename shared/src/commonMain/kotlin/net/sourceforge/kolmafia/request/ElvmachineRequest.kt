package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.ElVibratoManager
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [ElVibratoManager.registerRequest] / parse for elvmachine.php.
 */
object ElvmachineRequest {

    fun registerRequest(
        url: String?,
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        val urlString = url.orEmpty()
        if (!urlString.contains("elvmachine.php", ignoreCase = true)) return false
        val action = Regex("""action=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(urlString)?.groupValues?.getOrNull(1)?.lowercase()
            ?: return true
        val message = when (action) {
            "slot" -> {
                val cardId = ElVibratoManager.extractCardId(urlString) ?: return true
                val card = ElVibratoManager.PUNCHCARDS.first { it.id == cardId }
                "Inserting a ${card.name} into the slot."
            }
            "button" -> "Pushing the button."
            else -> return true
        }
        sessionLogger?.appendRawLine(message)
        return true
    }

    fun parseResponse(
        url: String?,
        html: String = "",
        sessionLogger: SessionLogger? = null,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (!url.orEmpty().contains("elvmachine.php", ignoreCase = true)) return false
        registerRequest(url, sessionLogger)
        return ElVibratoManager.parseResponse(url, html, consumeItem)
    }
}
