package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [DwarfContraptionRequest] — machine room hoppers / gauges / vacuum
 * (Phases 2646–2660).
 */
object DwarfContraptionRequest {
    private val ACTION = Regex("""[?&]action=([^&]*)""")
    private val GAUGES = Regex("""temp0=(\d*)&temp1=(\d*)&temp2=(\d*)&temp3=(\d*)""")
    private val HOPPER = Regex("""action=dohopper(\d*).*howmany=(\d*).*whichore=([^&]*)""")
    private val CHAMBER = Regex("""howmany=(\d*).*whichitem=([^&]*)""")

    fun parseResponse(
        urlString: String,
        responseText: String,
        preferences: Preferences,
        @Suppress("UNUSED_PARAMETER") inventoryManager: InventoryManager? = null,
        resultProcessor: ResultProcessor? = null,
        ascensions: Int = preferences.getInt("lastDwarfFactoryReset", 0).coerceAtLeast(0),
    ) {
        if (!urlString.contains("dwarfcontraption.php", ignoreCase = true)) return
        val action = ACTION.findAll(urlString).lastOrNull()?.groupValues?.get(1) ?: return
        when {
            action == "hopper0" -> DwarfFactoryRequest.setHopperRune(1, responseText, preferences, ascensions)
            action == "hopper1" -> DwarfFactoryRequest.setHopperRune(2, responseText, preferences, ascensions)
            action == "hopper2" -> DwarfFactoryRequest.setHopperRune(3, responseText, preferences, ascensions)
            action == "hopper3" -> DwarfFactoryRequest.setHopperRune(4, responseText, preferences, ascensions)
            action.startsWith("dohopper") -> parseDoHopper(
                urlString, responseText, preferences, inventoryManager, resultProcessor, ascensions,
            )
            action == "dorightpanel" -> {
                if (responseText.contains("You feed the punchcard into the slot") && resultProcessor != null) {
                    resultProcessor.processItem(3207, -1)
                }
            }
            action.startsWith("doredbutton") -> {
                if (responseText.contains("something falls into the bin")) {
                    DwarfFactoryRequest.clearHoppers()
                }
            }
            action == "dochamber" -> {
                if (responseText.contains("nothing much has happened")) return
                val match = CHAMBER.find(urlString) ?: return
                val count = match.groupValues[1].toIntOrNull() ?: return
                val itemId = match.groupValues[2].toIntOrNull() ?: return
                resultProcessor?.processItem(itemId, -count)
            }
        }
    }

    private fun parseDoHopper(
        urlString: String,
        responseText: String,
        preferences: Preferences,
        inventoryManager: InventoryManager?,
        resultProcessor: ResultProcessor?,
        ascensions: Int,
    ) {
        if (responseText.contains("You don't have") || responseText.contains("right material")) return
        val hopperMatch = HOPPER.find(urlString) ?: return
        val hopper = (hopperMatch.groupValues[1].toIntOrNull() ?: return) + 1
        DwarfFactoryRequest.setHopperRune(hopper, responseText, preferences, ascensions)
        val count = hopperMatch.groupValues[2].toIntOrNull() ?: return
        val oreName = oreName(hopperMatch.groupValues[3])
        val oreId = oreItemId(oreName)
        if (oreId > 0) {
            resultProcessor?.processItem(oreId, -count)
            val rune = preferences.getString("lastDwarfHopper$hopper")
            DwarfFactoryRequest.setItemRunes(oreId, rune, preferences)
        }
    }

    private fun oreName(token: String): String =
        if (token == "coal") "lump of coal" else "$token ore"

    private fun oreItemId(name: String): Int = when (name.lowercase()) {
        "linoleum ore" -> DwarfFactoryRequest.LINOLEUM_ORE
        "asbestos ore" -> DwarfFactoryRequest.ASBESTOS_ORE
        "chrome ore" -> DwarfFactoryRequest.CHROME_ORE
        "lump of coal" -> DwarfFactoryRequest.LUMP_OF_COAL
        else -> -1
    }

    fun registerRequest(urlString: String, sessionLogger: SessionLogger?): Boolean {
        if (!urlString.contains("dwarfcontraption.php", ignoreCase = true)) return false
        val action = ACTION.findAll(urlString).lastOrNull()?.groupValues?.get(1) ?: return true
        if (action == "dochamber") {
            val match = CHAMBER.find(urlString) ?: return false
            val count = match.groupValues[1]
            val itemId = match.groupValues[2]
            sessionLogger?.appendRawLine("Putting $count of item $itemId into the vacuum chamber.")
            return true
        }
        placeName(action)?.let {
            sessionLogger?.appendRawLine("Visiting $it in the Dwarven Factory Machine Room")
            return true
        }
        command(action, urlString)?.let {
            sessionLogger?.appendRawLine(it)
            return true
        }
        return false
    }

    private fun placeName(action: String): String? = when (action) {
        "hopper0" -> "Hopper #1"
        "hopper1" -> "Hopper #2"
        "hopper2" -> "Hopper #3"
        "hopper3" -> "Hopper #4"
        "gauges" -> "Gauges"
        "panelleft" -> "Left Panel"
        "panelright" -> "Right Panel"
        "bin" -> "Bin"
        "chamber" -> "Vacuum Chamber"
        else -> null
    }

    private fun command(action: String, urlString: String): String? = when {
        action == "doleftpanel" -> when {
            urlString.contains("which1") -> "Selecting pants"
            urlString.contains("which2") -> "Selecting weapon"
            urlString.contains("which3") -> "Selecting helmet"
            else -> null
        }
        action == "dorightpanel" -> "Feeding punchcard into slot"
        action == "doredbutton" -> "Pushing the red button"
        action == "dogauges" -> {
            val m = GAUGES.find(urlString)
            if (m != null) {
                "Setting gauges to ${m.groupValues[1]}, ${m.groupValues[2]}, ${m.groupValues[3]}, ${m.groupValues[4]}"
            } else null
        }
        action.startsWith("dohopper") -> {
            val m = HOPPER.find(urlString)
            if (m != null) {
                val hopper = (m.groupValues[1].toIntOrNull() ?: 0) + 1
                val count = m.groupValues[2]
                val ore = oreName(m.groupValues[3])
                if (urlString.contains("addtake=take")) {
                    "Taking $count $ore from hopper #$hopper"
                } else {
                    "Adding $count $ore to hopper #$hopper"
                }
            } else null
        }
        else -> null
    }
}
